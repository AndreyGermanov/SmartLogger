package loggers.downloaders;

import main.ISyslog;

import java.util.HashMap;

/**
 * Base class for all data downloaders.
 */
public abstract class Downloader implements IDownloader {

    /// During download process, object can experience probems and throw exceptions.
    /// This Syslog object used to write them to log file
    ISyslog syslog;

    /**
     * Main method, which downloader use to get content
     * @return Content as string
     */
    abstract public String download();

    /**
     * Method used to set Syslog object for internal error logging
     * @param syslog Link to syslog instance
     */
    public void setSyslog(ISyslog syslog) { this.syslog = syslog; }

    public void configure(HashMap<String,Object> config) {}
}
