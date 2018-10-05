package loggers;

import com.google.gson.Gson;
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
import java.util.HashMap;

/**
 * Base class for Data loggers.
 * Each data logger used to download source data using "Data Downloader class", parse it and extract
 * needed fields using "Data parser class" and write to file in filesystem
 */
public abstract class Logger implements ILogger,Cloneable, Syslog.Loggable {

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

    /**
     * Factory method, used to get instanse of logger of specified type
     * @param loggerType: Logger type (string representation of logger class)
     * @param config: Configuration object, to configure logger before start
     * @return: Instance of Data Logger class
     */
    public static ILogger create(String loggerType, HashMap<String,Object> config) {
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
        this.name = config.get("name").toString();
        this.shouldWriteDuplicates = Boolean.valueOf(config.getOrDefault("shouldWriteDuplicates","false").toString());
        if (this.syslog == null) this.syslog = new Syslog(this);
    }

    /**
     * Matin method, used to read source data, transform and write to file
     */
    public void log() {
        HashMap<String,Object> record = readRecord();
        if (record == null) return;
        if (shouldWriteDuplicates || isRecordChanged(record)) writeRecord(record);
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
        HashMap<String,Object> result = (HashMap<String,Object>)parser.parse();
        if (result.isEmpty()) {
            syslog.log(Syslog.LogLevel.WARNING,"Empty record returned after parsing",
                    this.getClass().getName(),"readRecord");
            return null;
        }
        result.put("timestamp",String.valueOf(Instant.now().getEpochSecond()));
        return result;
    }

    /**
     * Method used to compare provided data record with previous saved record
     * @param record Record to compare
     * @return True if provided record is different or false otherwise
     */
    boolean isRecordChanged(HashMap<String,Object> record) {
        if (lastRecord == null) lastRecord = getLastRecord();
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
     * Method returns last data record, written to filesystem by this logger
     * @return Record object
     */
    HashMap<String,Object> getLastRecord() {
        return lastRecord;
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
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            BufferedWriter recordFile = Files.newBufferedWriter(path);
            String json =getJson(record);
            recordFile.write(json);
            recordFile.flush();
            lastRecord = (HashMap<String,Object>)record.clone();
        } catch (IOException e) {
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
        String cacheRootPath = LoggerApplication.getInstance().getCachePath();
        String timestampStr = record.get("timestamp").toString();
        long timestamp = new Long(timestampStr);
        LocalDateTime date = LocalDateTime.ofEpochSecond(timestamp,0,ZoneOffset.UTC);
        String path = cacheRootPath + "/" + this.getName() + "/data/"+ date.getYear() + "/" + date.getMonthValue() + "/" +
                date.getDayOfMonth() + "/" + date.getHour() + "/" + date.getMinute() + "/" + date.getSecond() + ".json";
        return path;
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
     * Method returns path to log files, which Syslog uses to write error, info or warning messages
     * related to work of this data logger
     * @return Full path to directory for log files of this module
     */
    public String getSyslogPath() { return LoggerApplication.getInstance().getCachePath()+"/"+this.getName()+"/logs/"; }


    /**
     * Used to get unique name of this logger moddule
     * @return
     */
    public String getName() {
        return this.name;
    }

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

    /**
     * Method used to set Syslog instance to Downloader and Parser instances of this logger
     */
    public void propagateSyslog() {
        this.downloader.setSyslog(this.syslog);
        this.parser.setSyslog(this.syslog);
    }
}