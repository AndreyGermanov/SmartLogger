package archivers;

import archivers.processors.ArchiveProcessor;
import archivers.processors.IArchiveProcessor;
import config.ConfigManager;
import cronjobs.CronjobTask;
import cronjobs.CronjobTaskStatus;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;
import utils.DataMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Data Archiver base class. Used to create Data archiver components. Data archiver used to archive all files
 * in specified folder to destination folder in various formats or just by copy files
 */
public abstract class DataArchiver extends CronjobTask implements IDataArchiver, ISyslog.Loggable {

    // Path, which archiver used to write status data (as timestamp of last processed file)
    private String statusPath = "";
    // Destination path in which archiver created
    private String destinationPath = "";
    // Source path of files to archive
    private String sourcePath = "";
    // Timestamp of last archive file, used to determine from which file to begin (to not archive older files)
    protected Long lastFileTimestamp = 0L;
    // Last processed file name (to not process it twice)
    protected String lastFileName = "";
    // Unique name of current archive
    private String name = "";
    // Maximum number of files to archive per single run (0 - unlimited)
    private long maxArchiveFilesCount = 0L;
    // Maximum size of data to archive per single run (0 - unlimited)
    private long maxArchiveSize = 0L;
    // Should archiver remove archived files from source folder after process
    private boolean removeSourceAfterArchive = false;
    // Link to syslog daemon, which used to write errors and warningss
    protected ISyslog syslog;
    // Link to file processor, which used to implement archive operation (depends on type of archiver: File, Zip, etc)
    protected IArchiveProcessor processor;
    // Caches information about number of processed archive files per current run
    private long archivedFilesCount = 0L;
    // Caches information about summary size of data processed per current run
    private long archivedFilesSize = 0L;

    /**
     * Factory method, used to build concrete Data Archiver object, based on provided unique name
     * which method will try to find in current configuration
     * @param name Unique name
     * @return Constructed archiver object
     */
    static IDataArchiver create(String name) {
        if (name.isEmpty()) return null;
        HashMap<String,Object> config = ConfigManager.getInstance().getDataArchiver(name);
        return create(config);
    }

    /**
     * Factory method, used to build concrete Data Archiver object, based on provided configuration
     * @param config Configuration object
     * @return Constructed archiver object
     */
    static IDataArchiver create(HashMap<String,Object> config) {
        if (config == null) return null;
        String type = config.getOrDefault("type","").toString();
        if (type.isEmpty()) return null;
        switch (type) {
            case "copy": return new FileCopyDataArchiver(config);
            case "zip": return new FileZipDataArchiver(config);
            default: return null;
        }
    }

    /**
     * Class constructor
     * @param name Unique name of archiver
     */
    DataArchiver(String name) {
        if (!name.isEmpty()) configure(ConfigManager.getInstance().getDataArchiver(name));
    }

    /**
     * Class constructor
     * @param config Configuration object to configure archiver
     */
    DataArchiver(HashMap<String,Object> config) {
        configure(config);
    }

    /**
     * Class constructor
     * @param name - Unique name of archiver
     * @param type - Type of archiver
     * @param sourcePath - Source path with data to archiver
     * @param destinationPath - Destination path in which create archives
     */
    DataArchiver(String name, String type, String sourcePath, String destinationPath) {
        configure(DataMap.create("name",name,"type",type,"sourcePath",sourcePath,"destinationPath",destinationPath));
    }

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        name = config.getOrDefault("name",name).toString();
        statusPath = config.getOrDefault("statusPath",statusPath).toString();
        destinationPath = config.getOrDefault("destinationPath",destinationPath).toString();
        sourcePath = config.getOrDefault("sourcePath",sourcePath).toString();
        maxArchiveSize = Long.parseLong(config.getOrDefault("maxArchiveSize",maxArchiveFilesCount).toString());
        maxArchiveFilesCount = Long.parseLong(config.getOrDefault("maxArchiveFilesCount",maxArchiveFilesCount).toString());
        removeSourceAfterArchive = Boolean.parseBoolean(config.getOrDefault("removeSourceAfterArchive",removeSourceAfterArchive).toString());
        if (syslog == null) syslog = new Syslog(this);
        processor = ArchiveProcessor.create(config.getOrDefault("type","").toString(),this);
    }

    /**
     * Main method to start archiving procedure
     * @return Number of archived files
     */
    public long archive() {
        long result = 0L;
        archivedFilesCount = 0L;
        archivedFilesSize = 0L;
        if (processor == null || !processor.validateAndInitArchive()) return result;
        readLastRecord();
        result = archiveFiles();
        processor.finish();
        writeLastRecord();
        return result;
    }

    /**
     * Method used to read information about last file, archived in last archiver run.
     * This information includes last processed file name and timestamp of this file
     */
    private void readLastRecord() {
        Path path = Paths.get(getStatusPath()+"/last_record");
        if (Files.notExists(path)) return;
        try (BufferedReader reader = Files.newBufferedReader(path) ) {
            String record = reader.readLine();
            if (record==null || record.isEmpty()) return;
            String[] tokens = record.split(" ");
            if (tokens.length < 2) return;
            lastFileTimestamp = Long.parseLong(tokens[0]);
            lastFileName = Arrays.stream(tokens).skip(1).reduce((s,s1) -> s+=" "+s1).orElse("");
        } catch (NumberFormatException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not parse timestamp from last record file '"+path.toString()+"'."+
                    "Error message+'"+ e.getMessage()+"'",this.getClass().getName(),"readLastRecord");
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not read last record from file '"+path.isAbsolute()+"'."+
                    "Error message+'"+ e.getMessage()+"'",this.getClass().getName(),"readLastRecord");
        }
    }

    /**
     * Method which used to get list of files to process, filter them and pass to archive processor
     * to archive
     * @return Number of processed files
     */
    private long archiveFiles() {
        try {
            Files.walk(Paths.get(sourcePath)).filter(this::checkFile).sorted(this::sortFiles).forEach(this::processFile);
            return archivedFilesCount;
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not archive files. Error message: '"+e.getMessage(),
                    this.getClass().getName(),"archiveFiles");
            return 0;
        }
    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param file Path to file to check
     * @return True if file should be archived or false otherwise
     */
    public boolean checkFile(Path file) {
        if (!Files.isRegularFile(file)) return false;
        if (file.toString().endsWith(".tmp")) return false;
        try {
            long fileSize = Files.size(file);
            if (maxArchiveSize>0 && getArchivedFilesSize()+fileSize > maxArchiveSize) {
                addArchivedFilesSize(fileSize);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (maxArchiveFilesCount>0 && getArchivedFilesCount()+1>maxArchiveFilesCount) {
            incArchivedFilesCount();
            return false;
        }
        return true;
    }

    /**
     * Method used as comparator to sort list of files before pass to archive processor
     * @param file1 First file to compare
     * @param file2 Second file to compare
     * @return 0 - if files are equal, >0 if first file greater than second, <0 if first file less than seconf
     */
    private int sortFiles(Path file1,Path file2) {
        try {
            return Long.valueOf(Files.getLastModifiedTime(file1).toMillis() -
                    Files.getLastModifiedTime(file2).toMillis()).intValue();
        } catch (IOException e) { return 0;}
    }

    /**
     * Method used to archive file
     * @param sourceFile Path to file to archive
     */
    public void processFile(Path sourceFile) {
        processor.processFile(sourceFile);
    }

    /**
     * Method called after archiving each file
     * @param sourceFile Path to source file archived
     */
    public void finishFileProcessing(Path sourceFile) {
        lastFileName = sourceFile.toString();
        lastFileTimestamp = getFileTimestamp(sourceFile);
        incArchivedFilesCount();
        try {
            addArchivedFilesSize(Files.size(sourceFile));
        } catch (IOException e) { e.printStackTrace();}
        processor.finishFileProcessing(sourceFile);
    }

    /**
     * Method used to get timestmap of file
     * @param file - File to get timestamp of
     * @return Timestamp of file
     */
    public Long getFileTimestamp(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis() / 1000;
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not get timestamp of file '"+file.toString()+"'." +
                    "Error message: '"+e.getMessage(), this.getClass().getName(),"archiveFiles");
            return 0L;
        }
    }

    /**
     * Method used to write record about last archived file. Information includes full path to last archived file
     * and timestamp of this file
     */
    private void writeLastRecord() {
        if (lastFileName.isEmpty() || lastFileTimestamp == 0L) return;
        Path path = Paths.get(getStatusPath()+"/last_record");
        try {
            if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
            Files.deleteIfExists(path);
            BufferedWriter writer = Files.newBufferedWriter(path);
            writer.write(lastFileTimestamp.toString()+" "+lastFileName);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            syslog.log(ISyslog.LogLevel.ERROR,"Could not read last record to file '"+path.isAbsolute()+"'."+
                    "Error message+'"+ e.getMessage()+"'",this.getClass().getName(),"writeLastRecord");
        }
    }

    /**
     * Method used to get destination path which will be used to archive provided source file
     * @param file Path to source file
     * @return Destination path of this file
     */
    public Path getDestinationPathOfFile(Path file) {
        String relativePath = file.toString().replace(sourcePath,"");
        return Paths.get(destinationPath+relativePath);
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    public void run() {
        archive();
        setTaskStatus(CronjobTaskStatus.IDLE);
        setLastExecTime(Instant.now().getEpochSecond());
    }

    /**
     * Getters and setters for properties
     */

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getSyslogPath() {
        return LoggerApplication.getInstance().getCachePath()+"/logs/archivers/";
    }

    private String getStatusPath() {
        if (!statusPath.isEmpty()) return statusPath;
        return LoggerApplication.getInstance().getCachePath()+"/archivers/"+this.getName();
    }

    public String getSourcePath() { return sourcePath; }
    public String getDestinationPath() { return destinationPath; }
    public ISyslog getSyslog() { return syslog; }
    public boolean getRemoveSourceAfterArchive() { return removeSourceAfterArchive; }

    public void setArchivedFilesCount(long count) {
        archivedFilesCount = count;
    }

    public void incArchivedFilesCount() {
        archivedFilesCount += 1;
    }

    public long getArchivedFilesCount() {
        return archivedFilesCount;
    }

    public void setArchivedFilesSize(long size) {
        archivedFilesSize = size;
    }

    public void addArchivedFilesSize(long size) {
        archivedFilesSize += size;
    }

    public long getArchivedFilesSize() {
        return archivedFilesSize;
    }
}