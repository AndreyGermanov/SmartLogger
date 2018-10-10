package db.persisters;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FileDatabasePersister implements IDatabasePersister, ISyslog.Loggable {

    private String name = "";
    private IDatabaseAdapter databaseAdapter;
    private String sourcePath = "";
    private String collectionName = "";
    private boolean writeDuplicates = false;
    private boolean fillDataGaps = false;
    private IDataReader sourceDataReader;
    private int rowsPerRun = 0;
    private ISyslog syslog;
    private String statusPath = "";
    private Long lastWriteTimestamp=0L;

    FileDatabasePersister(HashMap<String,Object> config) {
        this.configure(config);
    }

    FileDatabasePersister(String name,String sourcePath,String databaseAdapter) {
        this.configure(DataMap.create("name",name,"sourcePath",sourcePath,"databaseAdapter",databaseAdapter));
    }

    FileDatabasePersister(String name) {
        this.configure(ConfigManager.getInstance().getDatabasePersister(name));
    }

    @Override
    public void configure(HashMap<String, Object> config) {
        if (config==null) return;
        name = config.getOrDefault("name",name).toString();
        sourcePath = config.getOrDefault("sourcePath",sourcePath).toString();
        collectionName = config.getOrDefault("collectionName",collectionName).toString();
        writeDuplicates = Boolean.parseBoolean(config.getOrDefault("writeDuplicates",writeDuplicates).toString());
        fillDataGaps = Boolean.parseBoolean(config.getOrDefault("fillDataGaps",fillDataGaps).toString());
        rowsPerRun = Integer.parseInt(config.getOrDefault("rowsPerRun",0).toString());
        statusPath = config.getOrDefault("statusPath",statusPath).toString();
        if (config.containsKey("databaseAdapter")) databaseAdapter = DatabaseAdapter.get(config.get("databaseAdapter").toString());
        if (syslog == null) syslog = new Syslog(this);
        sourceDataReader = new FileDataReader(sourcePath,syslog);
    }

    @Override
    public Integer persist() {
        lastWriteTimestamp = 0L;
        ArrayList<HashMap<String,Object>> data = prepareData();
        if (data == null || data.size()==0) return null;
        Integer insertedRowsCount = databaseAdapter.insert(collectionName,data);
        if (insertedRowsCount==null || insertedRowsCount==0) return null;
        writeLastWriteTimestamp();
        return insertedRowsCount;
    }

    private ArrayList<HashMap<String,Object>> prepareData() {
        if (sourceDataReader == null) return null;
        Long startDate = readLastWriteTimestamp();
        if (startDate > 0) startDate +=1;
        NavigableMap<Long,HashMap<String,Object>> data = sourceDataReader.getData(startDate,true);
        return (data == null || data.size()==0) ? null :
        (ArrayList<HashMap<String,Object>>)data.entrySet().stream().map(Map.Entry::getValue)
                .collect(Collectors.toMap(this::createUniqueRecordIndex,
                        record -> record,(i1,i2) -> i1)).entrySet().stream().map(Map.Entry::getValue)
                .sorted(Comparator.comparingInt(s -> Integer.parseInt(s.get("timestamp").toString())))
                .limit(rowsPerRun > 0 ? rowsPerRun : data.size())
                .peek(record -> setLastWriteTimestamp(record.get("timestamp").toString()))
                .collect(Collectors.toList());
    }

    private String createUniqueRecordIndex(HashMap<String,Object> record) {
        Predicate<String> condition = !writeDuplicates ? key -> !key.equals("timestamp") : key -> true;
        return record.keySet().stream().filter(condition).reduce((s,key) -> s+"_"+record.get(key).toString()).orElse("");
    }

    private void setLastWriteTimestamp(String timestamp) {
        lastWriteTimestamp = Long.parseLong(timestamp);
    }

    private Long readLastWriteTimestamp() {
        Path statusPath = Paths.get(this.getStatusPath()+"/last_timestamp");
        if (!Files.exists(statusPath)) return 0L;
        try {
            BufferedReader reader = Files.newBufferedReader(statusPath);
            Long result = Long.valueOf(reader.readLine());
            reader.close();
            return result;
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not read timestamp from '"+statusPath.toString()+"' file",
                    this.getClass().getName(),"getLastWriteTimestamp");
        } catch (NumberFormatException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not parse timestamp value from '"+statusPath.toString()+"' file.",
                    this.getClass().getName(),"getLastWriteTimestamp");
        }
        return 0L;
    }

    private void writeLastWriteTimestamp() {
        if (lastWriteTimestamp == 0) return;
        Path statusPath = Paths.get(this.getStatusPath()+"/last_timestamp");
        try {
            if (!Files.exists(statusPath.getParent())) Files.createDirectories(statusPath.getParent());
            BufferedWriter writer = Files.newBufferedWriter(statusPath,StandardOpenOption.CREATE);
            writer.write(lastWriteTimestamp.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not write last write timestamp '"+lastWriteTimestamp.toString()+
                    "' to file '"+statusPath.toString()+"'",this.getClass().getName(),"writeLastWriteTimestamp");
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

    private String getStatusPath() {
        if (statusPath.isEmpty()) return LoggerApplication.getInstance().getCachePath()+"/persisters/"+this.getName()+"/";
        return statusPath;
    }
}