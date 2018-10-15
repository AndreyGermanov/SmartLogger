package archivers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Data archiver which archives file by just copying it from source to destination folder
 */
public class FileCopyDataArchiver extends DataArchiver {

    /**
     * Class constructor
     * @param config Configuration object
     */
    FileCopyDataArchiver(HashMap<String,Object> config) {
        super(config);
    }

    // Determines how to process files, which already exists in destination folder (skip, overwrite, overwrite if newer)
    private FileUpdateRule fileUpdateRule = FileUpdateRule.OVERWRITE_IF_NEW;

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    @Override
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        String overwriteFiles = config.getOrDefault("overwriteFiles","").toString();
        switch (overwriteFiles) {
            case "OVERWRITE": fileUpdateRule = FileUpdateRule.OVERWRITE;break;
            case "OVERWRITE_IF_NEW": fileUpdateRule = FileUpdateRule.OVERWRITE_IF_NEW;break;
            case "SKIP": fileUpdateRule = FileUpdateRule.SKIP;
        }
    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param file Path to file to check
     * @return True if file should be archived or false otherwise
     */
    public boolean checkFile(Path file) {
        if (!super.checkFile(file)) return false;
        Path destinationFile = getDestinationPathOfFile(file);
        try {
            if (!Files.exists(destinationFile)) return true;
            switch (fileUpdateRule) {
                case OVERWRITE: return true;
                case OVERWRITE_IF_NEW:
                    return Files.getLastModifiedTime(file).toMillis() > Files.getLastModifiedTime(destinationFile).toMillis();
                default: return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
}

/**
 * List of all possible file update rules (used by archiver to determine what to do with
 * source files when they already exist in destination folder)
 */
enum FileUpdateRule {
    OVERWRITE,OVERWRITE_IF_NEW,SKIP
}