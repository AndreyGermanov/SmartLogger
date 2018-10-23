package loggers;

import com.google.gson.Gson;
import cronjobs.CronjobTask;
import cronjobs.CronjobTaskStatus;
import loggers.downloaders.IDownloader;
import loggers.parsers.IParser;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for Data loggers.
 * Each data logger used to download source data using "Data Downloader class", parse it and extract
 * needed fields using "Data parser class" and write to file in filesystem
 */
public abstract class Logger extends CronjobTask implements ILogger,Cloneable, Syslog.Loggable {

    /// ID of logger
    String name;
    /// Link to Data downloader class instance
    IDownloader downloader;
    /// Link to Data parser class instance
    IParser parser;
    /// Link to Syslog object, used to write messages and error exceptions to log file
    private ISyslog syslog;
    /// Last record of data, written
    private HashMap<String,Object> lastRecord;
    /// Should write duplicate data (if current record is the same as previous (lastRecord)).
    public boolean shouldWriteDuplicates = false;
    /// Destination path to which logger will write parsed data
    private String destinationPath = "";
    /// Path, to which logger will write status information, as last record
    private String statusPath = "";
    // List of field names, which should be logged to file. If empty or null, then
    // all fields will be saved
    private List<String> fieldsToLog;

    /**
     * Factory method, used to get instanse of logger of specified type
     * @param config: Configuration object, to configure logger before start
     * @return Instance of Data Logger class
     */
    public static ILogger create(HashMap<String,Object> config) {
        String loggerType = config.getOrDefault("className","").toString();
        ILogger result = null;
        switch (loggerType) {
            case "YandexWeatherLogger": result = new YandexWeatherLogger(config);
        }
        if (result != null) result.propagateSyslog();
        return result;
    }

    /**
     * Class constuctors
     */

    Logger() { this.syslog = new Syslog(this); }

    Logger(String name) {
        this();
        this.name = name;
    }

    Logger(HashMap<String,Object> config) {
        this();
    }

    Logger(String name, IDownloader downloader, IParser parser) {
        this();
        this.name = name;
        this.downloader = downloader;
        this.parser = parser;
    }

    /**
     * Method used to apply configuration to data logger instance.
     * @param config: Configuration object
     */
    public void configure(HashMap<String, Object> config) {
        super.configure(config);
        this.name = config.get("name").toString();
        this.shouldWriteDuplicates = Boolean.valueOf(config.getOrDefault("shouldWriteDuplicates","false").toString());
        this.destinationPath = config.getOrDefault("destinationPath",destinationPath).toString();
        this.statusPath = config.getOrDefault("statusPath",statusPath).toString();
        String fieldsToLog = config.getOrDefault("fieldsToLog","").toString();
        if (!fieldsToLog.isEmpty())
            this.fieldsToLog = Arrays.asList(fieldsToLog.split(","));
        if (this.syslog == null) this.syslog = new Syslog(this);
        this.downloader.configure(config);
        this.parser.configure(config);
        this.propagateSyslog();
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    public void run() {
        super.run();
        log();
        setTaskStatus(CronjobTaskStatus.IDLE);
        setLastExecTime(Instant.now().getEpochSecond());
    }

    /**
     * Matin method, used to read source data, transform and write to file
     */
    public void log() {
        HashMap<String,Object> record = readRecord();
        if (record!=null)
            syslog.log(ISyslog.LogLevel.DEBUG,"Logger '"+this.name+"' received record '"+record.toString()+"'.",
                    this.getClass().getName(),"log");
        if (!shouldWriteDuplicates) record = getChangedRecord(record);
        if (record == null) return;
        syslog.log(ISyslog.LogLevel.DEBUG,"Logger '"+this.name+"' filtered record '"+record.toString()+"'.",
                this.getClass().getName(),"log");
        syslog.log(ISyslog.LogLevel.DEBUG,"Logger '"+this.name+"' wrote record '"+record.toString()+"'.",
                this.getClass().getName(),"log");
        writeRecord(record);
    }

    /**
     * Method used to read current data from source and parse it
     * @return: Object with extracted data fields and their values or null if could not extract data
     */
    public HashMap<String,Object> readRecord() {
        String source = downloader.download();
        if (source.isEmpty()) {
            syslog.log(Syslog.LogLevel.WARNING,"Downloader returned empty string",
                    this.getClass().getName(),"readRecord");
            return null;
        }
        parser.setInputString(source);
        HashMap<String,Object> result = getFilteredRecord((HashMap<String,Object>)parser.parse());
        if (result.isEmpty()) {
            syslog.log(Syslog.LogLevel.WARNING,"Empty record returned after parsing",
                    this.getClass().getName(),"readRecord");
            return null;
        }
        result.putIfAbsent("timestamp",String.valueOf(Instant.now().getEpochSecond()));
        return result;
    }

    public HashMap<String,Object> getFilteredRecord(HashMap<String,Object> record) {
        if (record == null || fieldsToLog == null || fieldsToLog.isEmpty()) return record;
        return record.keySet().stream()
                .filter(key -> fieldsToLog.contains(key) || key.equals("timestamp"))
                .collect(Collectors.toMap(key->key, record::get,(item1, item2) -> item1,HashMap::new));
    }

    /**
     * Method used to compare provided data record with previous saved record
     * @param record Record to compare
     * @return True if provided record is different or false otherwise
     */
    boolean isRecordChanged(HashMap<String,Object> record) {
        if (lastRecord == null) readAndSetLastRecord();
        if (lastRecord == null) return true;
        if (record == lastRecord) return false;
        if (record == null) return true;
        if (record.keySet().size() != lastRecord.keySet().size()) return true;
        for (String key: record.keySet()) {
            if (key.equals("timestamp")) continue;
            if (!lastRecord.containsKey(key)) return true;
            if (!lastRecord.get(key).equals(record.get(key))) return true;
        }
        return false;
    }

    /**
     * Method compares provided record with last processed record and returns new record
     * with only fields that changed
     * @param record Source record
     * @return Resulting record with changed fields
     */
    private HashMap<String,Object> getChangedRecord(HashMap<String,Object> record) {
        if (record == null) return null;
        if (lastRecord == null) readAndSetLastRecord();
        if (lastRecord == null) {
            lastRecord = (HashMap<String,Object>)record.clone();
            return record;
        }
        HashMap<String,Object> resultRecord = new HashMap<>();
        for (String key: record.keySet()) {
            if (key == "timestamp") continue;
            if (!lastRecord.containsKey(key) || !lastRecord.get(key).equals(record.get(key)))
                resultRecord.put(key,record.get(key));
        }
        if (resultRecord.size()==0) return null;
        resultRecord.put("timestamp",record.get("timestamp"));
        lastRecord = (HashMap<String,Object>)record.clone();
        return resultRecord;
    }

    /**
     * Method used to get string value of last record from status file, parse it and setup
     */
    protected void readAndSetLastRecord() {
        String record = readLastRecord();
        if (record == null || record.isEmpty()) return;
        Gson gson = new Gson();
        lastRecord = gson.fromJson(record,HashMap.class);
    }

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    public String getLastRecordString() {
        return getJson(lastRecord);
    }

    /**
     * Method used to write provided record to file in JSON format
     * @param record: Input record
     */
    void writeRecord(HashMap<String,Object> record) {
        if (record == null) return;
        String recordPath = getRecordPath(record);
        Path path = Paths.get(recordPath);
        try {
            if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
            String json = getJson(record);
            BufferedWriter recordFile = Files.newBufferedWriter(path);
            recordFile.write(json);
            recordFile.flush();
            recordFile.close();
            writeLastRecord();
        } catch (IOException e) {
            e.printStackTrace();
            syslog.logException(e,this,"writeRecord");
        }
    }

    /**
     * Method used to return Path to file, to which provided record will be written
     * @param record: Record to write
     * @return Full path to file in file system
     */
    String getRecordPath(HashMap<String,Object> record) {
        if (record == null) return "";
        String timestampStr = record.get("timestamp").toString();
        long timestamp = new Long(timestampStr);
        LocalDateTime date = LocalDateTime.ofEpochSecond(timestamp,0,ZoneOffset.UTC);
        return getDestinationPath() + "/" + date.getYear() + "/" + date.getMonthValue() + "/" +
                date.getDayOfMonth() + "/" + date.getHour() + "/" + date.getMinute() + "/" + date.getSecond() + ".json";
    }

    /**
     * Method used to convert data record to JSON string
     * @param record: Source record
     * @return JSON string of record
     */
    String getJson(HashMap<String,Object> record) {
        Gson gson = new Gson();
        if (record != null) return gson.toJson(record);
        return "";
    }

    /**
     * Method returns destination path, which is a base path, to which logger writes data
     * @return String with path
     */
    public String getDestinationPath() {
        String resultPath = destinationPath;
        if (resultPath == null || resultPath.isEmpty())
            resultPath = LoggerApplication.getInstance().getCachePath()+"/loggers/"+this.getName();
        if (!Paths.get(resultPath).isAbsolute())
            resultPath = LoggerApplication.getInstance().getCachePath()+"/loggers/"+this.getName() + resultPath;
        return resultPath;
    }

    /**
     * Used to get unique name of this logger module
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    public String getCollectionType() { return "loggers"; }

    /**
     * Used to get link to current syslog object, used to log messages about this logger
     * @return
     */
    public ISyslog getSyslog() { return this.syslog;}

    /**
     * Used to manually set instance of Syslog object
     * @param syslog
     */
    public void setSyslog(ISyslog syslog) {
        this.syslog = syslog;
    }

    @Override
    public HashMap<String,Object> getLastRecord() { return lastRecord;}

    /**
     * Method used to set Syslog instance to Downloader and Parser instances of this logger
     */
    public void propagateSyslog() {
        this.downloader.setSyslog(this.syslog);
        this.parser.setSyslog(this.syslog);
    }
}