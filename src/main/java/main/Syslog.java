package main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class which implements internal error and info logging
 */
public class Syslog implements ISyslog {

    /**
     * Owning object
     */
    private Loggable owner;

    /**
     * Class constuctor
     * @param owner Owner of this logger instance
     */
    public Syslog(Loggable owner) {
        this.owner = owner;
    }

    /**
     * Method used to log exception, catched by owner
     * @param e: Link to exception
     * @param source: Link to source object, which caught exception
     * @param methodName: Name of method, in which exception caught
     */
    @Override
    public void logException(Exception e, Object source, String methodName) {
        this.log(Syslog.LogLevel.ERROR,e.getMessage(),source.getClass().getName(),methodName);
    }

    /**
     * Method used to log message, which owner used to write messages
     * @param level: Log level
     * @param message: Text of message
     * @param className: Name of class, which sent request to write message to log
     * @param methodName: Name of method, which sent request to write message to log
     */
    @Override
    public void log(LogLevel level, String message, String className, String methodName) {
        Path filePath = getLogFilePath(level);
        if (filePath == null) return;
        try {
            if (!Files.exists(filePath.getParent()))
                Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath.getParent())) return;
            StandardOpenOption openOption = StandardOpenOption.CREATE_NEW;
            if (Files.exists(filePath)) openOption = StandardOpenOption.APPEND;
            BufferedWriter writer = Files.newBufferedWriter(filePath, openOption);
            LocalDateTime now = LocalDateTime.now();
            writer.write(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " - " + owner.getName() + " - " +
                    message + " ("+className+","+methodName+")\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("Syslog error: "+e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Method used to get full path to log file, based on Log level and on owner object
     * @param level: Log level
     * @return
     */
    Path getLogFilePath(LogLevel level) {
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
}