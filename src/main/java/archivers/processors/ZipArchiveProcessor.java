package archivers.processors;

import archivers.IDataArchiver;
import main.ISyslog;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Archive processor, which archives source files to ZIP archive
 */
public class ZipArchiveProcessor extends ArchiveProcessor {

    // Link to Zip archive object
    ZipOutputStream archive;
    // Full path and name of destination ZIP archive
    String archiveName = "";

    /**
     * Class constructor
     * @param archiver - Source Data archiver
     */
    ZipArchiveProcessor(IDataArchiver archiver) {
        super(archiver);
    }

    /**
     * Method used to initialize archive before starting put files to it
     * @return True if archive initialized successfully or false otherwise
     */
    public boolean validateAndInitArchive() {
        if (!super.validateAndInitArchive()) return false;
        try {
            archiveName = "";
            archive = new ZipOutputStream(new FileOutputStream(getArchiveFilePath()+".tmp"));
            return true;
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not create archive '"+getArchiveFilePath()+"'. "+
            "Error message: "+e.getMessage(),this.getClass().getName(),"validatedAndInitArchive");
            return false;
        }
    }

    /**
     * Method puts provided file to archive
     * @param sourceFile Path to source file to place to archive
     */
    @Override
    public void processFile(Path sourceFile) {
        try {
            ZipEntry entry = new ZipEntry(sourceFile.toString().replace(archiver.getSourcePath()+"/",""));
            if (Files.isDirectory(sourceFile)) return;
            archive.putNextEntry(entry);
            InputStream stream = Files.newInputStream(sourceFile);
            int bufSize = 1024;
            byte[] buf = new byte[bufSize];
            int length;
            while ((length = stream.read(buf)) > 0) {
                archive.write(buf,0,length);
            }
            archive.closeEntry();
            archiver.finishFileProcessing(sourceFile);
        } catch (IOException e) {
            e.printStackTrace();
            syslog.log(ISyslog.LogLevel.ERROR,"Could not add file '"+sourceFile.toString()+"', to archive. "+
            "Error message: "+e.getMessage(),this.getClass().getName(),"processFile");
        }
    }

    /**
     * Method called after last file placed to archive
     */
    public void finish() {
        try {
            if (archiver.getArchivedFilesCount()>0) {
                archive.close();
                Files.move(Paths.get(getArchiveFilePath()+".tmp"),Paths.get(getArchiveFilePath()));
            }
            else
                Files.deleteIfExists(Paths.get(getArchiveFilePath()+".tmp"));
        } catch (IOException e) {
            e.printStackTrace();
            syslog.log(ISyslog.LogLevel.ERROR,"Could not finish writing archive file '"+getArchiveFilePath()+". "+
            "Error message: "+e.getMessage(),this.getClass().getName(),"finish");
        }
    }

    /**
     * Method returns file name of ZIP archive which is created without extension
     * @return File name
     */
    public String getArchiveName() {
        if (archiveName.isEmpty()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
            archiveName = archiver.getName()+"_"+LocalDateTime.now().format(fmt);
        }
        return archiveName;
    }

    /**
     * Method returns full path to created archive without extension
     * @return Path to archive
     */
    public String getArchivePath() {
        return archiver.getDestinationPath()+"/"+getArchiveName();
    }

    /**
     * Method returns file name of ZIP archive with extension
     * @return File name
     */
    public String getArchiveFileName() {
        return getArchiveName()+".zip";
    }

    /**
     * Method returns full path to created ZIP archive with extension
     * @return Path to file
     */
    public String getArchiveFilePath() {
        return getArchivePath()+".zip";
    }
}