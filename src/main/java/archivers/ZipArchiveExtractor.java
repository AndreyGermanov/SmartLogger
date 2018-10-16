package archivers;

import archivers.processors.ZipArchiveProcessor;

import java.nio.file.Path;
import java.util.HashMap;

/**
 * Class implements specific type of Archiver, which used to extract files from ZIP archives
 * to destination folder. Source folder of this archiver should contain list of ZIP files to extract.
 */
public class ZipArchiveExtractor extends FileCopyDataArchiver {

    /**
     * Class constructor
     *
     * @param config Configuration object
     */
    ZipArchiveExtractor(HashMap<String, Object> config) {
        super(config);
    }

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        config.put("type","data_zip");
        super.configure(config);
    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param sourceFile Path to file to check
     * @return True if file should be archived or false otherwise
     */
    public boolean checkFile(Path sourceFile) {
        if (!sourceFile.toString().endsWith(".zip")) return false;
        return super.checkFile(sourceFile);
    }

    /**
     * Method used to archive file
     * @param sourceFile Path to file to archive
     */
    public void processFile(Path sourceFile) {
        ZipArchiveProcessor processor = (ZipArchiveProcessor)this.processor;
        processor.extractArchive(sourceFile);
        finishFileProcessing(sourceFile);
    }
}
