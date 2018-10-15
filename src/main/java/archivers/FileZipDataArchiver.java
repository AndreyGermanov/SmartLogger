package archivers;

import java.nio.file.Path;
import java.util.HashMap;

/**
 * Data archiver which archives file by creating ZIP archives in destination folder
 */
public class FileZipDataArchiver extends DataArchiver {

    /**
     * Class constructor
     * @param config - Configuration object
     */
    FileZipDataArchiver(HashMap<String, Object> config) {
        super(config);
    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param file Path to file to check
     * @return True if file should be archived or false otherwise
     */
    public boolean checkFile(Path file) {
        if (!super.checkFile(file)) return false;
        if (file.toString().equals(lastFileName)) return false;
        if (getFileTimestamp(file) < lastFileTimestamp) return false;
        if (getFileTimestamp(file).equals(lastFileTimestamp) &&
                file.toString().compareTo(lastFileName)<0) return false;
        return true;
    }
}