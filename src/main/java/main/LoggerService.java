package main;

import loggers.ILogger;
import loggers.Logger;
import java.util.ArrayList;
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
        ArrayList<HashMap<String,Object>> config = getLoggersConfig();
        for (HashMap<String,Object> logger_config : config) {
            int pollPeriod = Integer.valueOf(logger_config.getOrDefault("pollPeriod",0).toString());
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

    /**
     * Method used to get current Data logger modules configuration
     * @return Array with configuration objects for each data logging module
     */
    public ArrayList<HashMap<String,Object>> getLoggersConfig() {
        ArrayList<HashMap<String,Object>> config = new ArrayList<>();
        HashMap<String,Object> logger1 = new HashMap<>();
        logger1.put("name","yandex_weather_golubitskaya");
        logger1.put("enabled",true);
        logger1.put("placeName","golubitskaya");
        logger1.put("shouldWriteDuplicates",true);
        logger1.put("pollPeriod",1);
        logger1.put("className","YandexWeatherLogger");
        config.add(logger1);
        HashMap<String,Object> logger2 = new HashMap<>();
        logger2.put("name","yandex_weather_temryuk");
        logger2.put("enabled",true);
        logger2.put("placeName","temryuk");
        logger2.put("shouldWriteDuplicates",true);
        logger2.put("pollPeriod",1);
        logger2.put("className","YandexWeatherLogger");
        config.add(logger2);
        return config;
    }
}