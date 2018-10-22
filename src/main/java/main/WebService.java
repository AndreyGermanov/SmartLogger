package main;

import com.google.gson.internal.LinkedTreeMap;
import config.ConfigManager;
import controllers.CronjobsController;
import controllers.IController;
import controllers.StatusController;
import io.javalin.Context;
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
    private HashMap<String, IController> controllers = new HashMap<>();

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
        registerControllers();
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

    /**
     * Method used to create instances and register all controllers,
     * which can be used to handle requests to webservers
     */
    private void registerControllers() {
        controllers.put(CronjobsController.class.getName(),new CronjobsController());
        controllers.put(StatusController.class.getName(), new StatusController());
    }

    /**
     * Method which webserver calls when receive request to find controller which is responsible to
     * handle this request and execute action on this controller to handle this request
     * @param routeConfig: Config of route, on which webserver responded (from config file)
     * @param webServer: Link to webserver instance, which received request
     * @param ctx: Link to request context, contains all request data and link to response object
     */
    public void handleRequest(HashMap<String,Object> routeConfig, IWebServer webServer, Context ctx) {
        String url = routeConfig.get("url").toString();
        if (url.isEmpty()) return;
        if (routeConfig.containsKey("controller")) {
            controllers.get("controller").handleRequest(url,webServer,ctx);
            return;
        }
        controllers.entrySet().stream().forEach(controller -> {
            controller.getValue().handleRequest(url,webServer,ctx);
        });
    }
}
