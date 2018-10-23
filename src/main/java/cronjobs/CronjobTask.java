package cronjobs;

import main.ISyslog;
import main.LoggerApplication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;

/**
 * Base class for object, which can be started as a Cronjob by LoggerService inside Timer thread
 */
public abstract class CronjobTask implements ICronjobTask {

    // Is this task enabled (otherwise LoggerService will not run it)
    private boolean enabled = false;
    // Current status of task (RUNNING or IDLE)
    private CronjobTaskStatus taskStatus = CronjobTaskStatus.IDLE;
    // Last timestamp when this task started execution
    private Long lastStartTime = 0L;
    // Last timestmap when this task finished execution
    private Long lastExecutionTime = 0L;
    // Path, to which task can write status information about progress or result of execution
    protected String statusPath = "";
    // Link to system logger used to write information about errors or warnings to file
    protected ISyslog syslog;

    /**
     * Getters and setters for variables defined above
     */
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public CronjobTaskStatus getTaskStatus() { return taskStatus; }
    public void setTaskStatus(CronjobTaskStatus taskStatus) { this.taskStatus = taskStatus; }
    public Long getLastExecTime() { return lastExecutionTime; }
    public void setLastExecTime(Long lastExecTime) { this.lastExecutionTime = lastExecTime; }
    public Long getLastStartTime() { return lastStartTime; }
    public void setLastStartTime(Long lastStartTime) { this.lastStartTime = lastStartTime; }

    /**
     * Returns various status information about task (descendants should override to provide specific
     * information)
     * @return HashMap with various data
     */
    public HashMap<String, Object> getTaskInfo() { return null; }

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    public String getCollectionType() {
        return "";
    }

    /**
     * Method used to apply configuration from configuration file
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        if (config==null) return;
        enabled = Boolean.parseBoolean(config.getOrDefault("enabled",false).toString());
        statusPath = config.getOrDefault("statusPath",statusPath).toString();
    }

    /**
     * Method which used by Cronjob object to start this task
     */
    public void run() {
        setLastStartTime(Instant.now().getEpochSecond());
        setTaskStatus(CronjobTaskStatus.RUNNING);
        if (syslog != null)
            syslog.log(ISyslog.LogLevel.DEBUG,"Running task '"+this.getName()+"'.",this.getClass().getName(),"run");
    }

    /**
     * Method returns path to status folder, which persister used to write status files (as timestamp of last
     * written record)
     * @return Full path
     */
    protected String getStatusPath() {
        String resultPath = statusPath;
        if (resultPath.isEmpty())
            resultPath = LoggerApplication.getInstance().getStatusPath()+"/"+getCollectionType()+"/"+this.getName();
        if (!Paths.get(resultPath).isAbsolute())
            resultPath =  LoggerApplication.getInstance().getStatusPath()+"/"+getCollectionType()+"/"+this.getName()+"/"+resultPath;
        return resultPath;
    }

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    public String getLastRecordString() { return null;}

    /**
     * Method used to read last written record from file
     * @return Record
     */
    protected String readLastRecord() {
        Path statusPath = Paths.get(this.getStatusPath()+"/last_record");
        if (!Files.exists(statusPath)) return null;
        try (BufferedReader reader = Files.newBufferedReader(statusPath)) {
            return reader.readLine();
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
    protected void writeLastRecord() {
        String lastRecordString = getLastRecordString();
        if (lastRecordString == null) return;
        Path statusPath = Paths.get(this.getStatusPath()+"/last_record");
        try {
            if (!Files.exists(statusPath.getParent())) Files.createDirectories(statusPath.getParent());
            Files.deleteIfExists(statusPath);
            BufferedWriter writer = Files.newBufferedWriter(statusPath,StandardOpenOption.CREATE_NEW);
            writer.write(lastRecordString);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not write last record '"+ lastRecordString +
                    "' to file '"+statusPath.toString()+"'",this.getClass().getName(),"writeLastRecord");
        }
    }

    /**
     * Method returns path to log files, which Syslog uses to write error, info or warning messages
     * related to work of this data logger
     * @return Full path to directory for log files of this module
     */
    public String getSyslogPath() { return LoggerApplication.getInstance().getLogPath()+"/"+getCollectionType()+"/"+this.getName(); }

    /**
     * Method used to manually assing System logger object to this adapter
     * @param syslog
     */
    public void setSyslog(ISyslog syslog) {
        this.syslog = syslog;
    }

}
