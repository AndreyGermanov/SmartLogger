package main;

import java.util.HashMap;

public interface ISyslog {
    void logException(Exception e, Object source, String methodName);

    void log(LogLevel level, String message, String className, String methodName);

    /**
     * Possible log levels
     */
    public enum LogLevel {
        DEBUG,INFO,WARNING,ERROR
    }

    /**
     * Interface which class must implement to be able to use this object to log messages
     */
    public interface Loggable {
        String getName();
        String getSyslogPath();
        HashMap<String,Object> getSyslogConfig();
    }
}
