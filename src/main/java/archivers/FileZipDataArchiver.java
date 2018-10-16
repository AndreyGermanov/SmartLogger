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

}