package main;

import aggregators.SimpleFileDataAggregator;
import archivers.DataArchiver;
import archivers.ZipArchiveExtractor;
import config.ConfigManager;
import cronjobs.Cronjob;
import cronjobs.ICronjobTask;
import db.persisters.FileDatabasePersister;
import loggers.Logger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service which manages all modules, related to logging (loggers,aggregators, persisters).
 * Used to create schedule and run configured components as cronjobs
 * Implemented as singleton.
 */
public class LoggerService {

    /// Link to single instance of this class
    private static LoggerService instance;

    /**
     * Private constructor
     */
    private LoggerService() {}

    /// Determines if service already started
    private boolean started = false;
    /// Array of started service cronjobs indexed by names
    private HashMap<String,Cronjob> cronjobs = new HashMap<>();
    /// Link to configuration manager, which provides configuration objects for cronjobs
    private ConfigManager configManager = ConfigManager.getInstance();

    /**
     * Method used to get instance of service from other classes.
     * @return Instance of application
     */
    public static LoggerService getInstance() {
        if (instance == null) instance = new LoggerService();
        return instance;
    }

    /**
     * Method used to start service
     */
    public void start() {
        if (started) return;
        String[] collections = {"loggers","aggregators","persisters","archivers","extractors"};
        Arrays.stream(collections).forEach(this::startCronjobs);
        this.started = true;
    }

    /**
     * Method used to setup cronjobs for all configured modules in collection of specified type
     * @param collectionType Type of module (loggers,aggregators, persisters etc.)
     */
    private void startCronjobs(String collectionType) {
        HashMap<String,HashMap<String,Object>> collection = configManager.getConfigCollection(collectionType);
        if (collection == null || collection.size() == 0) return;
        collection.forEach((name,value) -> {
            Cronjob cronjob = createCronjob(collectionType,name);
            if (cronjob == null) return;
            ICronjobTask task = cronjob.getTask();
            cronjobs.put(task.getCollectionType()+"_"+task.getName(),cronjob);
            (new Timer()).scheduleAtFixedRate(cronjob,0, cronjob.getPollPeriod()*1000);
        });
    }

    /**
     * Method used to create cronjob instance for specified module for Timer thread
     * @param collectionType - Type of module collection (loggers,aggregators,persisters)
     * @param objectName - System name of object
     * @return Initialized cronjob instance
     */
    private Cronjob createCronjob(String collectionType, String objectName) {
        HashMap<String,Object> objectConfig =  configManager.getConfigNode(collectionType,objectName);
        int pollPeriod = Double.valueOf(objectConfig.getOrDefault("pollPeriod",0).toString()).intValue();
        boolean enabled = Boolean.valueOf(objectConfig.getOrDefault("enabled","true").toString());
        if (pollPeriod == 0 || !enabled) return null;
        ICronjobTask task = createCronjobTask(collectionType,objectConfig);
        if (task == null) return null;
        return new Cronjob(task,pollPeriod);
    }

    /**
     * Method used to load component which used as a task inside cronjob
     * @param collectionType - Type of module collection (loggers,aggregators,persisters)
     * @param objectConfig - Configuration object, used to construct and configure object
     * @return Object which implements ICronjoTask interface and used inside cronjob as a task
     */
    private ICronjobTask createCronjobTask(String collectionType,HashMap<String,Object> objectConfig) {
        switch (collectionType) {
            case "loggers":
                return Logger.create(objectConfig);
            case "aggregators":
                return new SimpleFileDataAggregator(objectConfig);
            case "persisters":
                return new FileDatabasePersister(objectConfig);
            case "archivers":
                return DataArchiver.create(objectConfig);
            case "extractors":
                return new ZipArchiveExtractor(objectConfig);
            default:
                return null;
        }
    }

    /**
     * Returns list of unique names of cronjobs, created from config file
     * @return List of cronjob names
     */
    public Set<String> getCronjobNames() {
        return cronjobs.keySet();
    }

    /**
     * Returns list of cronjob types
     * @return List of cronjob types
     */
    public Set<String> getCronjobTypes() {
        return cronjobs.values().stream().map(it->it.getTask().getCollectionType()).collect(Collectors.toSet());
    }


    public ICronjobTask getCronjobTask(String name) {
        if (!cronjobs.containsKey(name)) return null;
        return cronjobs.get(name).getTask();
    }
}