package main;

import config.ConfigManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Main application class. Used to load configuration and start required services,
 * which is configured and enabled. This is singleton
 */
public class LoggerApplication {

    private String appPath = "";
    private String cachePath = "cache";
    private String logPath = "logs";
    private String statusPath = "statusPath";

    /**
     * Class constuctor
     */
    private LoggerApplication() { }



    /**
     * Returns path, which various modules can use to cache their data
     * @return
     */
    public String getCachePath() { return getAbsolutePath(cachePath); }
    public String getLogPath() { return getAbsolutePath(logPath);}
    public String getStatusPath() { return getAbsolutePath(statusPath);}

    public String getAppPath() {
        if (appPath.isEmpty()) return System.getProperty("user.dir");
        return appPath;
    }

    public String getAbsolutePath(String sourceDir) {
        String resultPath = sourceDir;
        if (sourceDir.isEmpty()) resultPath = System.getProperty("user.dir");
        if (!Paths.get(resultPath).isAbsolute()) resultPath = getAppPath()+"/"+resultPath;
        return resultPath;
    }

    public void configure(HashMap<String,Object> config) {
        appPath = config.getOrDefault("appPath",appPath).toString();
        cachePath = config.getOrDefault("cachePath",cachePath).toString();
        logPath = config.getOrDefault("logPath",logPath).toString();
        statusPath = config.getOrDefault("statusPath",statusPath).toString();
    }

    /// Link to single instance of application
    private static LoggerApplication application;


    /**
     * Method used to get instance of application from other classes.
     * @return Instance of application
     */
    public static LoggerApplication getInstance() {
        if (application == null) application = new LoggerApplication();
        return application;
    }

    public void run() {
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.loadConfig();
        this.configure(configManager.getConfig());
        LoggerService.getInstance().start();
        System.out.println("Application started ...");

    }

    /**
     * Entry poin of application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        LoggerApplication.getInstance().run();
    }
}
