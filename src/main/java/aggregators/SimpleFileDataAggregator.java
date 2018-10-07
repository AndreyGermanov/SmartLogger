package aggregators;

import com.google.gson.Gson;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import readers.FileDataReader;
import readers.IDataReader;
import utils.MathUtils;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

/**
 * Class used to aggregate data, collected by logger in folder of filesystem.
 */
public class SimpleFileDataAggregator extends DataAggregator implements Syslog.Loggable {

    /// Field definitions. Contains information about data fields, which should be aggregated
    HashMap<String,HashMap<String,Object>> fieldDefs = new HashMap<>();

    // Aggregation period in seconds
    private int aggregationPeriod = 5;

    // How much aggregation periods of source data to process per single run. If 0, then unlimited e.g. all data
    private int aggregatesPerRun = 0;

    // Should gaps in data be filled with values of previous period (or from next period if no previous period)
    private boolean fillDataGaps = true;

    /// Should write duplicate data (if current record is the same as previous).
    public boolean shouldWriteDuplicates = false;

    // Full path to the folder with source data
    private String sourcePath = "";

    // Link to FileDataReader object, which will be used to work with source data files
    private IDataReader sourceDataReader;

    private IDataReader aggregatorDataReader;

    // Unique name of this aggregator
    private String name = "";

    // Full path to destination folder to which destination aggregated data will be written. If empty, then full path
    // will be automatically calculated based on application cache path
    private String filePath = "";

    // Link to internal logger, which used to write error, warning or information messages while aggregating data
    private ISyslog syslog;

    /**
     * Class constructors
     */
    public SimpleFileDataAggregator(String name,String sourcePath) {
        HashMap<String,Object> config = new HashMap<>();
        config.put("name",name);
        config.put("sourcePath",sourcePath);
        this.configure(config);
    }

    public SimpleFileDataAggregator(HashMap<String,Object> config) {
        this.configure(config);
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        this.name = config.getOrDefault("name",this.name).toString();
        this.sourcePath = config.getOrDefault("sourcePath",this.sourcePath).toString();
        this.fieldDefs = (HashMap<String,HashMap<String,Object>>)config.getOrDefault("fields",this.fieldDefs);
        this.fillDataGaps = (boolean)config.getOrDefault("fillDataGaps",this.fillDataGaps);
        this.aggregationPeriod = (int)config.getOrDefault("aggregationPeriod",this.aggregationPeriod);
        this.aggregatesPerRun = (int)config.getOrDefault("aggregatesPerRun",this.aggregatesPerRun);
        this.syslog = this.getSyslog();
        this.sourceDataReader = new FileDataReader(this.sourcePath,this.syslog);
    }

    /**
     * Main entry point. Method loads source data from files, applies aggregation rules to data fields using configuration
     * and stores aggregated data to destination folder
     */
    public void aggregate() {
        FileDataReader.DataRange range = getAggregationRange();
        Stream.iterate(range.startDate,(Long timestamp) -> timestamp+aggregationPeriod)
                .limit(Math.round((range.endDate-range.startDate)/aggregationPeriod))
                .parallel()
                .forEach(this::aggregateInterval);
    }

    /**
     * Method calculates time interval to aggregate data
     * @return Range with startDate and endDate
     */
    FileDataReader.DataRange getAggregationRange() {
        FileDataReader.DataRange aggregatedDataRange = this.getAggregatorDataReader().getRange();
        Long startDate = aggregatedDataRange.endDate;
        Long endDate = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        if (this.aggregatesPerRun!=0) endDate = startDate + aggregatesPerRun*aggregationPeriod;
        FileDataReader.DataStats sourceDataRange = sourceDataReader.getDataStats(startDate,endDate,true);
        startDate = alignDate(Long.max(startDate,sourceDataRange.range.startDate));
        endDate = alignDate(Long.min(endDate,sourceDataRange.range.endDate));
        return IDataReader.getDataRange(startDate,endDate);
    }

    /**
     * Method used to round timestamp to closest aggregation interval
     * @param timestamp: Timestamp to align
     * @return Aligned timestamp
     */
    Long alignDate(Long timestamp) {
        return (long)Math.floor(timestamp/aggregationPeriod)*aggregationPeriod;
    }

    /**
     * Method used to write single aggregation interval
     * @param startDate: Start date of interval
     */
    void aggregateInterval(Long startDate) {
        NavigableMap<Long,HashMap<String,Object>> data = sourceDataReader.getData(startDate+1,
                startDate+aggregationPeriod,false);
        HashMap<String,AggregateFieldStats> stats = getAggregateStats(data);
        if (stats.size() == 0) return;
        HashMap<String,Object> aggregate = new HashMap<>();
        for (String fieldName: stats.keySet()) {
            Object value = getAggregatedValue(fieldName,stats.get(fieldName));
            if (value != null) aggregate.put(fieldName,value);
        }
        if (aggregate.size() == 0) return;
        aggregate = markRecord(startDate,aggregate);
        Path path = Paths.get(getRecordPath(aggregate));
        try {
            if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
            if (Files.exists(path)) Files.delete(path);
            BufferedWriter writer = Files.newBufferedWriter(path,StandardOpenOption.CREATE_NEW);
            writer.write((new Gson()).toJson(aggregate));
            writer.flush();
        } catch (Exception e) {
            syslog.logException(e,this,"aggregateInterval");
        }
    }

    /**
     * Method used to calculate aggregated value from data, collected in interval
     * @param fieldName: Name of field, for which calculate aggregated value
     * @param stats: Summarized interval data
     * @return Calculated value or null in case of errors
     */
    Object getAggregatedValue(String fieldName, AggregateFieldStats stats) {
        if (stats == null) return null;
        HashMap<String,Object> fieldConf = this.fieldDefs.get(fieldName);
        if (fieldConf == null) return null;
        int precision = (int)fieldConf.getOrDefault("precision",2);
        Object result = null;
        switch (fieldConf.getOrDefault("aggregate_function","constant").toString()) {
            case "count": result = stats.count;break;
            case "sum": result = stats.sum;break;
            case "min": result = stats.min;break;
            case "max": result = stats.max;break;
            case "first": result = stats.first;break;
            case "last": result = stats.last;break;
            case "constant": result = stats.first;break;
            case "average":
                if (stats.count == 0) return null;
                result = stats.sum / stats.count;
        }
        return result instanceof Double ? MathUtils.round(result,precision) : result;
    }

    /**
     * Method returns summarized data for each field from provided source data array
     * @param data: Source data array
     * @return HashMap of statistical objects for each field.
     */
    HashMap<String,AggregateFieldStats> getAggregateStats(NavigableMap<Long,HashMap<String,Object>> data) {
        HashMap<String,AggregateFieldStats> result = new HashMap<>();
        for(Long timestamp : data.keySet() ) {
            HashMap<String,Object> record = data.get(timestamp);
            if (record.size()==0) continue;
            for (String fieldName: this.fieldDefs.keySet()) {
                AggregateFieldStats stats = result.get(fieldName);
                Object value = calculateFieldValue(fieldName,record);
                stats = addEntryToFieldStats(value,stats);
                if (stats != null) result.put(fieldName,stats);
            }
        }
        return result;
    }

    /**
     * Calculate value of field for aggregation.
     * @param fieldName: Name of field
     * @param record: Record of source data
     * @return Calculated value or null in case of errors
     */
    Object calculateFieldValue(String fieldName,HashMap<String,Object> record) {
        HashMap<String,Object> fieldConf = this.fieldDefs.get(fieldName);
        if (fieldConf.containsKey("expression") && !fieldConf.get("expression").toString().isEmpty())
            return evaluateExpression(fieldConf.get("expression").toString(),record);
        else if (fieldConf.containsKey("field") && !fieldConf.get("field").toString().isEmpty())
            return record.get(fieldName);
        else if (fieldConf.containsKey("constant") && !fieldConf.get("constant").toString().isEmpty())
            return evaluateConstant(fieldConf.get("constant").toString());
        else
            return null;
    }

    /**
     * Calculate value of field, if formula expression specified for this field config
     * @param expression: Formula
     * @param record: Source data record
     * @return: Calculated value or null in case of errors
     */
    Double evaluateExpression(String expression,HashMap<String,Object> record) {
        try {
            Expression calculator = new ExpressionBuilder(expression).variables(record.keySet()).build();
            for (String fieldName: record.keySet()) {
                Object rawValue = record.get(fieldName);
                try {
                    Double value = Double.valueOf(rawValue.toString());
                    calculator.setVariable(fieldName,value);
                } catch (NumberFormatException e) { }
            }
            return calculator.evaluate();
        } catch (Exception e) {
            syslog.log(Syslog.LogLevel.WARNING,"Could not process expression '"+expression+"' for record '"+
            record+"'. Exception thrown: "+e.getMessage()+".",this.getClass().getName(),"evaluateExpression");
            return null;
        }
    }

    /**
     * Method evaluates value of type "constant" for field
     * @param constantValue
     * @return
     */
    Object evaluateConstant(String constantValue) {
        switch (constantValue) {
            case "$aggregatorId":
                return this.getName();
            case "$aggregationPeriod":
                return this.aggregationPeriod;
            default:
                return constantValue;
        }
    }

    /**
     * Adds value of field to summarized statistical object
     * @param value: Value to add
     * @param stats Source statistical object
     * @return Statistical object with added value to it fields
     */
    AggregateFieldStats addEntryToFieldStats(Object value, AggregateFieldStats stats) {
        if (value == null) return stats;
        if (stats == null) stats = new AggregateFieldStats();
        if (stats.first == null) stats.first = value;
        stats.last = value;
        try {
            Double decimalValue = Double.valueOf(value.toString());
            stats.init();
            stats.sum += decimalValue;
            stats.count += 1;
            if (decimalValue>stats.max) stats.max = decimalValue;
            if (decimalValue<stats.min) stats.min = decimalValue;
        } catch (NumberFormatException e) {
            syslog.log(Syslog.LogLevel.WARNING,"Could not parse field value '"+value+"'",this.getClass().getName(),
                    "addEntryToFieldStats");
        }
        return stats;
    }

    /**
     * Method used to add current aggregator identification fields to data record before writing it to
     * destinaton folder
     * @param timestamp timestamp of record
     * @param record record to mark
     * @return Marked record
     */
    HashMap<String,Object> markRecord(Long timestamp,HashMap<String,Object> record) {
        record.put("timestamp",timestamp.toString());
        return record;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setSourceDataReader(IDataReader sourceDataReader) {
        this.sourceDataReader = sourceDataReader;
    }

    public void setAggregatorDataReader(IDataReader aggregatorDataReader) {
        this.aggregatorDataReader = aggregatorDataReader;
    }

    public ISyslog getSyslog() {
        if (this.syslog == null)
            this.syslog = new Syslog(this);
        return this.syslog;
    }

    public void setSyslog(ISyslog syslog) {
        this.syslog = syslog;
    }

    public IDataReader getAggregatorDataReader() {
        if (this.aggregatorDataReader == null)
            this.aggregatorDataReader = new FileDataReader(this.getAggregatorPath(),syslog);
        return this.aggregatorDataReader;
    }

    /**
     * Returns root directory, in which currect aggregator writes it's data
     * @return Full path to file in filesystem
     */
    String getAggregatorPath() {
        String cachePath = LoggerApplication.getInstance().getCachePath();
        if (this.filePath.isEmpty()) {
            return cachePath + "/aggregators/"+this.getName();
        } else {
            Path path = Paths.get(this.filePath);
            if (path.isAbsolute()) return this.filePath;
            return cachePath + "/" + this.filePath;
        }
    }

    /**
     * Method used to return Path to file, to which provided record will be written
     * @param record: Record to write
     * @return Full path to file in file system
     */
    String getRecordPath(HashMap<String,Object> record) {
        if (record == null) return "";
        String basePath = this.getAggregatorPath();
        String timestampStr = record.get("timestamp").toString();
        long timestamp = new Long(timestampStr);
        LocalDateTime date = LocalDateTime.ofEpochSecond(timestamp,0,ZoneOffset.UTC);
        String path = basePath +  "/data/"+ date.getYear() + "/" + date.getMonthValue() + "/" +
                date.getDayOfMonth() + "/" + date.getHour() + "/" + date.getMinute() + "/" + date.getSecond() + ".json";
        return path;
    }

    @Override
    /**
     * Returns path to folder, to which current aggregator writes logs with errors, warnings etc.
     */
    public String getSyslogPath() {
        return getAggregatorPath() + "/logs";
    }

    /**
     * Class, which holds summarized information of single field in aggregated interval
     */
    public class AggregateFieldStats {
        Double max;
        Double min;
        Object first;
        Object last;
        Integer count;
        Double sum;
        public void init() {
            if (sum==null) sum = 0.0;
            if (count==null) count = 0;
            if (max==null) max = -999999999.0;
            if (min==null) min = 999999999.0;
        }
    }
}