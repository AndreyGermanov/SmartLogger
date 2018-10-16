package archivers;

import java.nio.file.Path;
import java.util.HashMap;

/**
 * Zip archiver specific to archive data folder structures, captured by loggers
 * and aggregated by aggregators
 */
public class DataZipDataArchiver extends FileZipDataArchiver {

    /**
     * Class constructor
     *
     * @param config - Configuration object
     */
    DataZipDataArchiver(HashMap<String, Object> config) {
        super(config);
    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param file Path to file to check
     * @return True if file should be archived or false otherwise
     */
    public boolean checkFile(Path file) {
        if (!file.toString().endsWith(".json")) return false;
        return super.checkFile(file);
    }

    /**
     * Method used to get timestmap of file
     * @param file - File to get timestamp of
     * @return Timestamp of file
     */
    public Long getFileTimestamp(Path file) { return getFileTimestamp(file,true); }
}
