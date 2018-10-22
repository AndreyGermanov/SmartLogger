package webservers;

import com.google.gson.internal.LinkedTreeMap;
import io.javalin.Javalin;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;
import main.WebService;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class which implements WebServer, working over HTTP or HTTPS
 */
public class WebServer implements IWebServer {

    // Unique name of webserver
    private String name = "";
    // Port on which webserver runs
    private int port = 0;
    // Which path web server will use as path with static content (html, css files and images)
    private String staticPath = "";
    // Link to Jetty webserver instance
    private Javalin app;
    // Link to system logger to log error or warning messages of this server or controllers
    private ISyslog syslog;
    // Configuration of routes, which webserver can serve
    private HashMap<String,Object> routes = new HashMap<>();

    /**
     * Class constructors
     */
    public WebServer() {}

    public WebServer(HashMap<String,Object> config) {
        this.configure(config);
        this.setup();
    }

    @Override
    /**
     * Method used to set server variables from configuration file
     */
    public void configure(HashMap<String, Object> config) {
        name = config.getOrDefault("name",name).toString();
        port = Double.valueOf(config.getOrDefault("port",port).toString()).intValue();
        staticPath = config.getOrDefault("staticPath",staticPath).toString();
        if (syslog == null) syslog = new Syslog(this);
        if (config.containsKey("routes") && config.get("routes") instanceof HashMap) {
            routes = (HashMap<String,Object>)config.get("routes");
        }
    }

    /**
     * Method used to bind route handlers for all routes, which configured for this webserver
     * in configuration file
     * @param routes Configuration object for routes
     */
    void initRoutes(HashMap<String,Object> routes) {
        WebService webService = WebService.getInstance();
        routes.entrySet().stream().forEach(routeEntry -> {
            if(!(routeEntry.getValue() instanceof HashMap)) return;
            HashMap<String,Object> route = (HashMap<String,Object>)routeEntry.getValue();
            String url = route.getOrDefault("url","").toString();
            if (url.isEmpty()) return;
            String requestMethod = route.getOrDefault("method","GET").toString();
            switch (requestMethod) {
                case "GET": app.get(url,ctx -> webService.handleRequest(route,this,ctx));break;
                case "POST": app.post(url,ctx-> webService.handleRequest(route,this,ctx));break;
                case "PUT": app.put(url,ctx->webService.handleRequest(route,this,ctx));break;
                case "DELETE": app.delete(url,ctx->webService.handleRequest(route,this,ctx));
            }
        });
    }

    /**
     * Method used to setup web server instance according to configuration options before start
     */
    public void setup() {
        app = Javalin.create();
        if (!staticPath.isEmpty() && Files.exists(Paths.get(staticPath).toAbsolutePath())) {
            app.enableStaticFiles(staticPath);
        }
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin","*");
            ctx.header("Access-Control-Allow-Methods","GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers","Authorization,Content-Type");
            ctx.result("");
        });
        app.get("/", ctx -> ctx.result("Server '"+name+"' is listening on port "+port));
        if (routes.size()>0) initRoutes(routes);
    }

    /**
     * Method used to run web server either directly or inside separate thread (new Thread(webserver) )
     */
    public void run() {
        app.start(port);
    }

    public String getName() { return name; }

    public String getSyslogPath() { return LoggerApplication.getInstance().getLogPath()+"/webservers/"+this.getName();}

    public ISyslog getSyslog() { return syslog;}

}
