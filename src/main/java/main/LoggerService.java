package main;

import config.ConfigManager;
import loggers.ILogger;
import loggers.Logger;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Service which manages Data loggers. Used to create schedule and run data logging operations cronjobs
 * Implemented as singleton.
 */
public class LoggerService {

    /// Link to single instance of this class
    private static LoggerService instance;

    /**
     * Private constuctor
     */
    private LoggerService() {};

    /// Determines if service already started
    private boolean started = false;

    /**
     * Method used to get instance of service from other classes.
     * @return Instance of application
     */
    public static LoggerService getInstance() {
        if (instance == null) instance = new LoggerService();
        return instance;
    }

    /**
     * Used to start service
     */
    public void start() {
        if (started) return;
        startLoggers();
        this.started = true;
    }

    /**
     * Method used to setup cronjobs for all configured data logger modules
     */
    void startLoggers() {
        ConfigManager configManager = ConfigManager.getInstance();
        HashMap<String,HashMap<String,Object>> config = configManager.getDataLoggers();
        for (String logger_name  : config.keySet()) {
            HashMap<String,Object> logger_config =  configManager.getDataLogger(logger_name);
            int pollPeriod = Double.valueOf(logger_config.getOrDefault("pollPeriod",0).toString()).intValue();
            boolean enabled = Boolean.valueOf(logger_config.getOrDefault("enabled","true").toString());
            String className = logger_config.get("className").toString();
            if (pollPeriod==0 || !enabled) continue;
            ILogger logger = Logger.create(className, logger_config);
            if (logger == null) continue;
            (new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.log();
                }
            },0, pollPeriod);
        }
    }
}