package db.persisters;

import com.google.gson.Gson;
import config.ConfigManager;
import db.adapters.DatabaseAdapter;
import db.adapters.IDatabaseAdapter;
import main.ISyslog;
import main.Syslog;
import readers.FileDataReader;
import readers.IDataReader;
import utils.DataMap;
import java.util.*;
import java.util.stream.Collectors;

public class FileDatabasePersister extends DatabasePersister implements ISyslog.Loggable {

    // Unique name of persister
    private String name = "";
    // Link to adapter, which provides database access settings
    private IDatabaseAdapter databaseAdapter;
    // Path to folder with aggregated source data
    private String sourcePath = "";
    // Name of destination collection (table) in database
    private String collectionName = "";
    // Should this persister write duplicate rows
    private boolean writeDuplicates = false;
    // Should this persister fill gaps in data using values from previous rows
    private boolean fillDataGaps = false;
    // Link to data reader instance, which used to manage data reading process from source folder
    private IDataReader sourceDataReader;
    // How many rows should this persister write to database per single run. If 0, then will process all data in
    // source folder
    private int rowsPerRun = 0;
    // Path to folder, in which this persister write temporary status information, like last processed row
    private String statusPath = "";
    // Last processed record
    private HashMap<String,Object> lastRecord;

    /**
     * Class constructor
     * @param config - Configuration object
     */
    public FileDatabasePersister(HashMap<String,Object> config) {
        this.configure(config);
    }

    /**
     * Class constructor
     * @param name - Name of persister
     * @param sourcePath - Path to source data folder
     * @param databaseAdapter - Name of database adapter in configuration
     */
    FileDatabasePersister(String name,String sourcePath,String databaseAdapter) {
        this.configure(DataMap.create("name",name,"sourcePath",sourcePath,"databaseAdapter",databaseAdapter));
    }

    /**
     * Class constructor
     * @param name - Name of persister
     */
    FileDatabasePersister(String name) {
        this.configure(ConfigManager.getInstance().getDatabasePersister(name));
    }

    /**
     * Method used to load settings of this persister from configuration object, provided by configuration manager
     * from configuration file
     * @param config - Configuration object
     */
    @Override
    public void configure(HashMap<String, Object> config) {
        super.configure(config);
        name = config.getOrDefault("name",name).toString();
        sourcePath = config.getOrDefault("sourcePath",sourcePath).toString();
        collectionName = config.getOrDefault("collectionName",collectionName).toString();
        writeDuplicates = Boolean.parseBoolean(config.getOrDefault("writeDuplicates",writeDuplicates).toString());
        fillDataGaps = Boolean.parseBoolean(config.getOrDefault("fillDataGaps",fillDataGaps).toString());
        rowsPerRun = Double.valueOf(config.getOrDefault("rowsPerRun",0).toString()).intValue();
        statusPath = config.getOrDefault("statusPath",statusPath).toString();
        if (config.containsKey("databaseAdapter")) databaseAdapter = DatabaseAdapter.get(config.get("databaseAdapter").toString());
        if (syslog == null) syslog = new Syslog(this);
        sourceDataReader = new FileDataReader(sourcePath,syslog);
    }

    /**
     * Entry point method. Used to start process of writing source data to database
     * @return
     */
    @Override
    public Integer persist() {
        syslog.log(ISyslog.LogLevel.DEBUG,"Data persister '"+this.name+"' started to persist...",
                this.getClass().getName(),"persist");
        lastRecord = new HashMap<>();
        ArrayList<HashMap<String,Object>> data = prepareData();
        if (data == null || data.size()==0) return null;
        syslog.log(ISyslog.LogLevel.DEBUG,"Data persister '"+this.name+"' got data record "+data.toString(),
                this.getClass().getName(),"persist");
        Integer insertedRowsCount = databaseAdapter.insert(collectionName,data);
        if (insertedRowsCount==null || insertedRowsCount==0) return null;
        syslog.log(ISyslog.LogLevel.DEBUG,"Data persister '"+this.name+"' wrote data record "+data.toString(),
                this.getClass().getName(),"persist");
        writeLastRecord();
        return insertedRowsCount;
    }

    /**
     * Method used to read source data and transform it to format, ready for data adapter
     * to write to database
     * @return
     */
    private ArrayList<HashMap<String,Object>> prepareData() {
        syslog.log(ISyslog.LogLevel.DEBUG,"Data persister '"+this.name+"'. Begin prepare data to persist",
                this.getClass().getName(),"prepareData");
        if (sourceDataReader == null) return null;
        readAndSetLastRecord();
        syslog.log(ISyslog.LogLevel.DEBUG,"Data persister '"+this.name+"'. Read last data record.",
                this.getClass().getName(),"prepareData");
        Long startDate = 0L;
        if (lastRecord != null) startDate = Long.parseLong(lastRecord.get("timestamp").toString());
        if (startDate > 0) startDate +=1;
        syslog.log(ISyslog.LogLevel.DEBUG,"Data persister '"+this.name+"'. Last record timestamp = ."+startDate,
                this.getClass().getName(),"prepareData");
        NavigableMap<Long,HashMap<String,Object>> data = sourceDataReader.getData(startDate,true);
        syslog.log(ISyslog.LogLevel.DEBUG,"Data persister '"+this.name+"'. Got data ."+data,
                this.getClass().getName(),"prepareData");
        return (data == null || data.size()==0) ? null :
        (ArrayList<HashMap<String,Object>>)data.values().stream()
                .sorted(Comparator.comparingInt(s -> Integer.parseInt(s.get("timestamp").toString())))
                .filter(record -> !isDuplicateRecord(record))
                .limit(rowsPerRun > 0 ? rowsPerRun : data.size())
                .peek(this::setLastRecord)
                .collect(Collectors.toList());
    }

    /**
     * Method used to check if provided record contains the same data as last processed record
     * @param record Record to check
     * @return True if records are equal or false otherwise
     */
    private boolean isDuplicateRecord(HashMap<String,Object> record) {
        if (record == null || lastRecord == null) return false;
        if (record.size() != lastRecord.size()) return false;
        for (String key: record.keySet()) {
            if (!lastRecord.containsKey(key)) return false;
            if (key.equals("timestamp")) continue;
            if (!record.get(key).toString().equals(lastRecord.get(key).toString())) return false;
        }
        syslog.log(ISyslog.LogLevel.DEBUG,"NOT DUPLICATED",this.getClass().getName(),"isDuplicateRecord");
        return true;
    }

    /**
     * Method used to update last rocessed record in temporary variable
     * @param record - Record to set
     */
    private void setLastRecord(HashMap<String,Object> record) { lastRecord = (HashMap<String,Object>)record.clone();};

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    public String getLastRecordString() {
        if (lastRecord == null) return null;
        Gson gson = new Gson();
        return gson.toJson(lastRecord);
    }

    /**
     * Method used to get string value of last record from status file, parse it and setup
     */
    protected void readAndSetLastRecord() {
        String result = readLastRecord();
        if (result == null) {
            lastRecord = null;
            return;
        }
        Gson gson = new Gson();
        setLastRecord(gson.fromJson(result,HashMap.class));
    }

    public HashMap<String,Object> getLastRecord() {
        return lastRecord;
    }

    @Override
    public String getName() {
        return name;
    }

}