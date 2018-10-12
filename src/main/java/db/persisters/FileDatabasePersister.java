package db.persisters;

import com.google.gson.Gson;
import config.ConfigManager;
import db.adapters.DatabaseAdapter;
import db.adapters.IDatabaseAdapter;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;
import readers.FileDataReader;
import readers.IDataReader;
import utils.DataMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
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
    // Link to system logger, used to store system error messages
    private ISyslog syslog;
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
        lastRecord = new HashMap<>();
        ArrayList<HashMap<String,Object>> data = prepareData();
        if (data == null || data.size()==0) return null;
        Integer insertedRowsCount = databaseAdapter.insert(collectionName,data);
        if (insertedRowsCount==null || insertedRowsCount==0) return null;
        writeLastRecord();
        return insertedRowsCount;
    }

    /**
     * Method used to read source data and transform it to format, ready for data adapter
     * to write to database
     * @return
     */
    private ArrayList<HashMap<String,Object>> prepareData() {
        if (sourceDataReader == null) return null;
        lastRecord = readLastRecord();
        Long startDate = 0L;
        if (lastRecord != null) startDate = Long.parseLong(lastRecord.get("timestamp").toString());
        if (startDate > 0) startDate +=1;
        NavigableMap<Long,HashMap<String,Object>> data = sourceDataReader.getData(startDate,true);
        return (data == null || data.size()==0) ? null :
        (ArrayList<HashMap<String,Object>>)data.entrySet().stream().map(Map.Entry::getValue)
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
        return true;
    }

    /**
     * Method used to update last rocessed record in temporary variable
     * @param record - Record to set
     */
    private void setLastRecord(HashMap<String,Object> record) { lastRecord = (HashMap<String,Object>)record.clone();};

    /**
     * Method used to read last written record from file
     * @return Record
     */
    private HashMap<String,Object> readLastRecord() {
        Path statusPath = Paths.get(this.getStatusPath()+"/last_record");
        if (!Files.exists(statusPath)) return null;
        try {
            Gson gson = new Gson();
            BufferedReader reader = Files.newBufferedReader(statusPath);
            HashMap<String,Object> result = gson.fromJson(reader.readLine(),HashMap.class);
            reader.close();
            return result;
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not read last record from '"+statusPath.toString()+"' file",
                    this.getClass().getName(),"readLastRecord");
        } catch (Exception e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not parse last record value from '"+statusPath.toString()+"' file.",
                    this.getClass().getName(),"readLastRecord");
        }
        return null;
    }

    /**
     * Method used to write last written record to file as JSON object
     */
    private void writeLastRecord() {
        if (lastRecord == null) return;
        Path statusPath = Paths.get(this.getStatusPath()+"/last_record");
        try {
            Gson gson = new Gson();
            if (!Files.exists(statusPath.getParent())) Files.createDirectories(statusPath.getParent());
            Files.deleteIfExists(statusPath);
            BufferedWriter writer = Files.newBufferedWriter(statusPath,StandardOpenOption.CREATE_NEW);
            writer.write(gson.toJson(lastRecord));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not write last record '"+lastRecord.toString()+
                    "' to file '"+statusPath.toString()+"'",this.getClass().getName(),"writeLastRecord");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSyslogPath() {
        return LoggerApplication.getInstance().getCachePath()+"/logs/db/"+this.getName()+"/";
    }

    public void setSyslog(ISyslog syslog) {
        this.syslog = syslog;
    }

    /**
     * Method returns path to status folder, which persister used to write status files (as timestamp of last
     * written record)
     * @return Full path
     */
    private String getStatusPath() {
        if (statusPath.isEmpty()) return LoggerApplication.getInstance().getCachePath()+"/persisters/"+this.getName()+"/";
        return statusPath;
    }
}