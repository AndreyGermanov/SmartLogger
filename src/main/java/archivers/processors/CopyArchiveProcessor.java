package archivers.processors;

import archivers.IDataArchiver;
import main.ISyslog;
import java.io.IOException;
import java.nio.file.*;

/**
 * Archive processor, which used to archive files by just copying them to destination folder,
 * provided by Data archiver
 */
public class CopyArchiveProcessor extends ArchiveProcessor {

    /**
     * Class consturctor
     * @param archiver - Source data Archiver
     */
    CopyArchiveProcessor(IDataArchiver archiver) {
        super(archiver);
    }

    /**
     * Method puts provided file to archive
     * @param sourceFile Path to source file to place to archive
     */
    public void processFile(Path sourceFile) {
        Path destinationFile = archiver.getDestinationPathOfFile(sourceFile);
        try {
            if (!Files.exists(destinationFile.getParent())) Files.createDirectories(destinationFile.getParent());
            Path tmpDestinationFile = Paths.get(destinationFile.toString()+".tmp");
            Files.copy(sourceFile,tmpDestinationFile,StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmpDestinationFile,destinationFile,StandardCopyOption.REPLACE_EXISTING);
            archiver.finishFileProcessing(sourceFile);
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not copy file '"+destinationFile.toString()+"'."+
                    "Error message+'"+ e.getMessage()+"'",this.getClass().getName(),"processFile");
        }
    }
}
