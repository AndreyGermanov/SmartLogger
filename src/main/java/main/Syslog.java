package main;

import config.ConfigManager;
import rotators.FileRotator;
import rotators.IFileRotator;
import utils.DataMap;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Class which implements internal error and info logging
 */
public class Syslog implements ISyslog {

    /**
     * Owning object
     */
    private Loggable owner;
    // Is log rotation enabled
    private boolean rotateLogs = true;
    // Maximum single log file size to rotate it
    private long maxLogFileSize = 10 * 1024L;
    // Maximum number of log archived files in rotation
    private int maxLogFiles = 5;
    // Should log archives in rotation be compressed by ZIP
    private boolean compressArchives = true;
    // Log rotation configuration for concrete log levels
    private HashMap<String,Object> logRotatorsConfig = new HashMap<>();

    /**
     * Class constuctor
     * @param owner Owner of this logger instance
     */
    public Syslog(Loggable owner) {
        this.owner = owner;
        this.configure(this.owner.getSyslogConfig());
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        if (config == null) return;
        rotateLogs = Boolean.parseBoolean(config.getOrDefault("rotateLogs",rotateLogs).toString());
        maxLogFileSize = Double.valueOf(config.getOrDefault("maxLogFileSize",maxLogFileSize).toString()).longValue();
        maxLogFiles = Double.valueOf(config.getOrDefault("maxLogFiles",maxLogFiles).toString()).intValue();
        compressArchives = Boolean.parseBoolean(config.getOrDefault("compressArchives",compressArchives).toString());
        try {
            logRotatorsConfig = (HashMap<String,Object>)config.getOrDefault("logRotators",logRotatorsConfig);
        } catch (Exception e) { e.printStackTrace();}
    }

    /**
     * Method used to log exception, catched by owner
     * @param e: Link to exception
     * @param source: Link to source object, which caught exception
     * @param methodName: Name of method, in which exception caught
     */
    @Override
    public void logException(Exception e, Object source, String methodName) {
        String message = "Message: "+e.getLocalizedMessage()+"\n Stack trace: \n";
        message += Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).reduce("",(s, s1) -> s = s+"\n"+s1);
        this.log(Syslog.LogLevel.ERROR,message,source.getClass().getName(),methodName);
    }

    /**
     * Method used to log message, which owner used to write messages
     * @param level: Log level
     * @param message: Text of message
     * @param className: Name of class, which sent request to write message to log
     * @param methodName: Name of method, which sent request to write message to log
     */
    @Override
    synchronized public void log(LogLevel level, String message, String className, String methodName) {
        Path filePath = getLogFilePath(level);
        if (filePath == null) return;
        try {
            if (!Files.exists(filePath.getParent())) Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath.getParent())) return;
            if (rotateLogs) getFileRotator(level).rotate();
            StandardOpenOption openOption = StandardOpenOption.CREATE_NEW;
            if (Files.exists(filePath)) {
                openOption = StandardOpenOption.APPEND;
            }
            BufferedWriter writer = Files.newBufferedWriter(filePath, openOption);
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write( date + " - " + owner.getName() + " - " + message + " ("+className+","+methodName+")\n");
            writer.flush();writer.close();
        } catch (IOException e) {
            System.out.println("Syslog error: "+e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Method used to get full path to log file, based on Log level and on owner object
     * @param level: Log level
     * @return Full path to log file of specified level
     */
    private Path getLogFilePath(LogLevel level) {
        String logPath = owner.getSyslogPath();
        if (logPath == null) return null;
        switch (level) {
            case DEBUG:
                return Paths.get(logPath,"debug.log");
            case INFO:
                return Paths.get(logPath,"info.log");
            case WARNING:
                return Paths.get(logPath,"warning.log");
            case ERROR:
                return Paths.get(logPath,"error.log");
            default:
                return null;
        }
    }

    /**
     * Method used to get FileRotator which will be used to archive log file
     * and rotate log files if needed based on configuration
     * @param level Log level, for which get rotator
     * @return FileRotator class instance, used to archive current log of this "Level"
     */
    private IFileRotator getFileRotator(LogLevel level) {
        HashMap<String,Object> config = ConfigManager.getInstance().getConfigNode("rotators",
                logRotatorsConfig.getOrDefault(level.toString(),"").toString());
        if (config == null)
            return new FileRotator(DataMap.create("filePath",getLogFilePath(level),"maxArchives", maxLogFiles,
                    "maxSourceFileSize",maxLogFileSize,"removeSourceFileAfterRotation",true,
                    "compressArchives",compressArchives));
        config.put("filePath",getLogFilePath(level));
        return new FileRotator(config);
    }
}