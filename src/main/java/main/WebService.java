package main;

import config.ConfigManager;
import webservers.IWebServer;
import webservers.WebServer;

import java.util.HashMap;

/**
 * Singleton object, used to start all web servers, enabled in configuration files
 */
public class WebService {

    /**
     * Private constructor
     */
    private WebService() {}

    /// Determines if service already started
    private boolean started = false;
    /// Array of started service cronjobs indexed by names
    private HashMap<String, IWebServer> webservers = new HashMap<>();
    /// Link to configuration manager, which provides configuration objects for cronjobs
    private ConfigManager configManager = ConfigManager.getInstance();

    private static WebService instance;

    /**
     * Method used to get instance of service from other classes.
     * @return Instance of application
     */
    public static WebService getInstance() {
        if (instance == null) instance = new WebService();
        return instance;
    }

    /**
     * Entry point method
     */
    public void start() {
        if (started) return;
        startWebServers();
        started = true;
    }

    /**
     * Method which goes through collection of web server configurations
     * and invokes start method for each item
     */
    private void startWebServers() {
        HashMap<String,HashMap<String,Object>> configArray = configManager.getConfigCollection("webservers");
        for (String name: configArray.keySet()) {
            startWebServer(configManager.getConfigNode("webservers",name));

        }
    }

    /**
     * Method instantiates, configures and starts webserver using provided config in separate Thread
     * @param config: Configuration object
     */
    private void startWebServer(HashMap<String,Object> config) {
        if (!config.containsKey("name") || config.get("name").toString().isEmpty()) return;
        if (!config.containsKey("enabled") || !Boolean.parseBoolean(config.getOrDefault("enabled",false).toString())) return;
        IWebServer webserver = new WebServer(config);
        webserver.setup();
        webservers.put(config.get("name").toString(),webserver);
        (new Thread(webserver)).start();
    }
}
