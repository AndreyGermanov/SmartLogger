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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Data Archiver base class. Used to create Data archiver components. Data archiver used to archive all files
 * in specified folder to destination folder in various formats or just by copy files
 */
public abstract class DataArchiver extends CronjobTask implements IDataArchiver, ISyslog.Loggable {

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
    // Regular expression which used as a filter for filenames, that should be processed by archiver
    private String filterRegex = "";

    /**
     * Factory method, used to build concrete Data Archiver object, based on provided unique name
     * which method will try to find in current configuration
     * @param name Unique name
     * @return Constructed archiver object
     */
    public static IDataArchiver create(String name) {
        if (name.isEmpty()) return null;
        HashMap<String,Object> config = ConfigManager.getInstance().getDataArchiver(name);
        return create(config);
    }

    /**
     * Factory method, used to build concrete Data Archiver object, based on provided configuration
     * @param config Configuration object
     * @return Constructed archiver object
     */
    public static IDataArchiver create(HashMap<String,Object> config) {
        if (config == null) return null;
        String type = config.getOrDefault("type","").toString();
        if (type.isEmpty()) return null;
        switch (type) {
            case "copy": return new FileCopyDataArchiver(config);
            case "zip": return new FileZipDataArchiver(config);
            case "data_copy": return new DataCopyDataArchiver(config);
            case "data_zip": return new DataZipDataArchiver(config);
            case "send_ftp": return new SendFtpDataArchiver(config);
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
        filterRegex = config.getOrDefault("filterRegex",filterRegex).toString();
        if (syslog == null) syslog = new Syslog(this);
        processor = ArchiveProcessor.create(config.getOrDefault("type","").toString(),this);
        if (processor != null) processor.configure(config);
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
        readAndSetLastRecord();
        result = archiveFiles();
        processor.finish();
        writeLastRecord();
        return result;
    }

    /**
     * Method used to get string value of last record from status file, parse it and setup
     */
    public void readAndSetLastRecord() {
        String record = readLastRecord();
        if (record == null || record.isEmpty()) return;
        try {
            String[] tokens = record.split(" ");
            if (tokens.length < 2) return;
            lastFileTimestamp = Long.parseLong(tokens[0]);
            lastFileName = Arrays.stream(tokens).skip(1).reduce((s,s1) -> s+=" "+s1).orElse("");
        } catch (NumberFormatException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not parse timestamp from last record '"+record+"'."+
                    "Error message+'"+ e.getMessage()+"'",this.getClass().getName(),"readAndSetLastRecord");
        }
    }

    /**
     * Method which used to get list of files to process, filter them and pass to archive processor
     * to archive
     * @return Number of processed files
     */
    private long archiveFiles() {
        try {
            Files.walk(Paths.get(sourcePath)).filter(p -> Files.isRegularFile(p))
                    .sorted(this::sortFiles)
                    .filter(this::checkFile)
                    .forEach(this::processFile);
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
        if (!filterRegex.isEmpty() && !Pattern.compile(filterRegex).matcher(file.toString()).find()) return false;
        if (file.toString().endsWith(".tmp")) return false;
        if (file.toString().equals(lastFileName)) return false;
        if (lastFileTimestamp>0 && getFileTimestamp(file) < lastFileTimestamp) return false;
        if (getFileTimestamp(file).equals(lastFileTimestamp) &&
                file.toString().compareTo(lastFileName)<0) return false;
        try {
            if (Files.size(file) == 0) return false;
            long fileSize = Files.size(file);
            if (maxArchiveSize>0 && getArchivedFilesSize()+fileSize > maxArchiveSize) {
                return false;
            }
            if (maxArchiveFilesCount>0 && getArchivedFilesCount()+1>maxArchiveFilesCount) {
                return false;
            }
            addArchivedFilesSize(fileSize);
            incArchivedFilesCount();
        }
        catch (IOException e) {
            e.printStackTrace();
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
        return Long.valueOf(getFileTimestamp(file1) - getFileTimestamp(file2)).intValue();
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
        processor.finishFileProcessing(sourceFile);
    }

    /**
     * Method used to get timestamp of file
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
     * Method used to get timestmap of file
     * @param file - File to get timestamp of
     * @param usingPathContents - If true, than function uses components of path to determine timestamp
     *                          (used for logged data),otherwise it reads last modified time of file
     * @return Timestamp of file
     */
    protected Long getFileTimestamp(Path file, boolean usingPathContents) {
        if (!usingPathContents) return getFileTimestamp(file);
        Path parentPath = file.getParent();
        if (parentPath.getNameCount()<6) return 0L;
        try {
            String fileName = file.getFileName().toString();
            int count = parentPath.getNameCount();
            int second = Integer.parseInt(fileName.substring(0,fileName.indexOf(".")));
            int minute = Integer.parseInt(parentPath.getName(count-1).toString());
            int hour = Integer.parseInt(parentPath.getName(count-2).toString());
            int day = Integer.parseInt(parentPath.getName(count-3).toString());
            int month = Integer.parseInt(parentPath.getName(count-4).toString());
            int year = Integer.parseInt(parentPath.getName(count-5).toString());
            return LocalDateTime.of(year,month,day,hour,minute,second).toEpochSecond(ZoneOffset.UTC);
        } catch (Exception e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not get timestamp for path '"+parentPath.toString()+"'. "+
                    "Error message: "+e.getMessage(),this.getClass().getName(),"getFileTimestamp");
            return 0L;
        }
    }

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    public String getLastRecordString() {
        if (lastFileName.isEmpty() || lastFileTimestamp == 0L) return null;
        return lastFileTimestamp.toString()+" "+lastFileName;
    }

    @Override
    public long getLastRecordTimestamp() {
        if (lastFileTimestamp<=0) return 0L;
        return lastFileTimestamp;
    }

    /**
     * Method used to get destination path which will be used to archive provided source file
     * @param file Path to source file
     * @return Destination path of this file
     */
    public Path getDestinationPathOfFile(Path file) {
        String relativePath = file.toString().replace(sourcePath,"");
        return Paths.get(getDestinationPath()+relativePath);
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    public void run() {
        super.run();
        archive();
        setTaskStatus(CronjobTaskStatus.IDLE);
        setLastExecTime(Instant.now().getEpochSecond());
    }

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    public String getCollectionType() { return "archivers";}

    /**
     * Getters and setters for properties
     */

    @Override
    public String getName() {
        return this.name;
    }



    public String getSourcePath() { return sourcePath; }

    public String getDestinationPath() {
        String resultPath = destinationPath;
        if (resultPath.isEmpty())
            resultPath = LoggerApplication.getInstance().getCachePath()+"/archivers/"+this.getName();
        if (!Paths.get(resultPath).isAbsolute())
            resultPath = LoggerApplication.getInstance().getCachePath()+"/archivers/"+this.getName()+"/"+destinationPath;
        return resultPath;
    }
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

    public String getLastRecord() {
        return this.getLastRecordString();
    }
}