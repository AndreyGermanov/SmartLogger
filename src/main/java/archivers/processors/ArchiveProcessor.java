package archivers.processors;

import archivers.IDataArchiver;
import main.ISyslog;
import utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Base class for archive processors. Archive processor works in composition with Data Archiver, it used
 * to get source files from data archiver and do actual work to put them to archiver
 */
public abstract class ArchiveProcessor implements IArchiveProcessor {

    // Link to DataArchiver object, which used as a source of files to archive
    protected IDataArchiver archiver;
    // Link ty syslog object, used to write errors and warnings
    protected ISyslog syslog;

    /**
     * Factory method used to construct Archive processors of concrete type from configuration file
     * @param type Type of archiver
     * @param archiver - Link to archiver object
     * @return
     */
    public static IArchiveProcessor create(String type, IDataArchiver archiver) {
        switch (type) {
            case "copy": case "data_copy": return new CopyArchiveProcessor(archiver);
            case "zip": case "data_zip": return new ZipArchiveProcessor(archiver);
            case "send_ftp": return new SendFtpArchiveProcessor(archiver);
            default: return null;
        }
    }

    /**
     * Class consturctor
     * @param archiver - Source data archiver
     */
    ArchiveProcessor(IDataArchiver archiver) {
        this.archiver = archiver;
        syslog = archiver.getSyslog();
    }

    ArchiveProcessor() {}

    /**
     * Method used to initialize archive before starting put files to it
     * @return True if archive initialized successfully or false otherwise
     */
    public boolean validateAndInitArchive() {
        String sourcePath = archiver.getSourcePath();
        String destinationPath = archiver.getDestinationPath();

        if (!Files.exists(Paths.get(sourcePath))) return false;
        try {
            if (!Files.exists(Paths.get(destinationPath))) Files.createDirectories(Paths.get(destinationPath));
            if (!Files.exists(Paths.get(destinationPath))) return false;
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not create destinationPath '"+destinationPath+"'."+
                    "Error message+'"+ e.getMessage()+"'",this.getClass().getName(),"validateConfig");
            return false;
        }
        return true;
    }

    /**
     * Method called by archiver after each file placed to archive
     * @param file - Source file, which placed to archive
     */
    public void finishFileProcessing(Path file) {
        if (!archiver.getRemoveSourceAfterArchive()) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not remove source file '"+file.toString()+"'."+
                    "Error message+'"+ e.getMessage()+"'",this.getClass().getName(),"finishFileProcessing");
        }
    }

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {}

    /**
     * Method called after last file placed to archive
     */
    public void finish() {
        if (archiver.getRemoveSourceAfterArchive())
            FileUtils.removeFolder(Paths.get(archiver.getSourcePath()),true);
    }
}