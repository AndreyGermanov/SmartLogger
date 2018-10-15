package archivers;

import cronjobs.ICronjobTask;
import main.ISyslog;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Interface which each Data Archiver must implement
 */
public interface IDataArchiver extends ICronjobTask {
    void configure(HashMap<String,Object> config);
    long archive();
    String getSourcePath();
    String getDestinationPath();
    ISyslog getSyslog();
    boolean getRemoveSourceAfterArchive();
    void finishFileProcessing(Path sourceFile);
    Path getDestinationPathOfFile(Path sourceFile);
    long getArchivedFilesCount();
}
