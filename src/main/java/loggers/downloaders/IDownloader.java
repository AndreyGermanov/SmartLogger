package loggers.downloaders;

import main.Syslog;

/**
 * Base interface that all data loader classes must implement to be used in Data loggers
 */
public interface IDownloader {
    String download();
    void setSyslog(Syslog syslog);
}