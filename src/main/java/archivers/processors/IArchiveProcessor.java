package archivers.processors;

import java.nio.file.Path;
import java.util.HashMap;

/**
 * Interface, which each Archive processor should implement to be included as processor
 * of Data archiver
 */
public interface IArchiveProcessor {
    void configure(HashMap<String,Object> config);
    boolean validateAndInitArchive();
    void processFile(Path file);
    void finishFileProcessing(Path file);
    void finish();
}
