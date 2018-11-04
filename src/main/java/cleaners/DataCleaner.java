package cleaners;

import config.ConfigManager;
import cronjobs.CronjobTask;
import cronjobs.CronjobTaskStatus;
import cronjobs.ICronjobTask;
import main.ISyslog;
import main.LoggerService;
import utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Class implements "Data cleaner" tasks, which used to clean older files from specified folder,
 * based on information from "consumers" of this folder. Cleaner should remove only files, which
 * already processed by all consumers, specified in configuration of each cleaner
 */
public class DataCleaner extends CronjobTask implements IDataCleaner, ISyslog.Loggable {

    // Unique name of cleaner
    private String name = "";
    // Source path to clean
    private Path sourcePath = null;
    // List of module names, which are consumers of this folder and requires data in it
    private ArrayList<String> consumers = new ArrayList<>();

    /**
     * Class constructor
     * @param name Name of rotator in configuration cleaners
     */
    public DataCleaner(String name) {
        this.configure(ConfigManager.getInstance().getConfigNode("cleaners",name));
    }

    /**
     * Class constructor
     * @param config Configuration object for cleaner
     */
    public DataCleaner(HashMap<String,Object> config) {
        this.configure(config);
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        if (config == null || !config.containsKey("sourcePath") ) return;
        super.configure(config);
        name = config.getOrDefault("name",name).toString();
        sourcePath = Paths.get(config.getOrDefault("sourcePath",sourcePath).toString());
        try {
            consumers = (ArrayList<String>) config.getOrDefault("consumers", consumers);
        } catch (Exception e) { syslog.logException(e,this,"configure");}
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    public void run() {
        super.run();
        clean();
        setTaskStatus(CronjobTaskStatus.IDLE);
        setLastExecTime(Instant.now().getEpochSecond());
    }

    /**
     * Entry method, which starts cleaning process
     */
    @Override
    public void clean() {
        if (sourcePath == null || Files.notExists(sourcePath)) return;
        long maxTimestamp = getMaxTimestamp();
        try {
            Files.walk(sourcePath)
                .filter(path -> {
                    try {
                        return Files.isRegularFile(path) &&
                                Files.getLastModifiedTime(path).toInstant().getEpochSecond() < maxTimestamp;
                    } catch (Exception e) { syslog.logException(e, this, "clean.filter");return false; }
                })
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) { syslog.logException(e, this, "clean.delete"); }
                });
        } catch (IOException e) {
            syslog.logException(e,this,"clean");
        }
        FileUtils.removeFolder(sourcePath,true);
    }

    /**
     * Method determines the maximum timestamp of data to remove, based on information from consumers
     * of this folder. Cleaner will remove only files, which modification time is less than timestamp,
     * returned by this method
     * @return Timestamp
     */
    private long getMaxTimestamp() {
        if (consumers == null || consumers.size()==0) return 0L;
        LoggerService service = LoggerService.getInstance();
        return consumers.stream()
                .map(service::getCronjobTask).filter(Objects::nonNull)
                .mapToLong(ICronjobTask::getLastRecordTimestamp).min().orElse(0L);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getLastRecord() {
        return null;
    }

    @Override
    public long getLastRecordTimestamp() {
        return 0L;
    }

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    public String getCollectionType() { return "cleaners";}
}
