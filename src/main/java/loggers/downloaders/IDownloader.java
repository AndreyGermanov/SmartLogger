package loggers.downloaders;

import main.ISyslog;

import java.util.HashMap;

/**
 * Base interface that all data loader classes must implement to be used in Data loggers
 */
public interface IDownloader {
    String download();
    void setSyslog(ISyslog syslog);
    void configure(HashMap<String,Object> config);
}