package webservers;

import io.javalin.Javalin;

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
    // List of routes which this webserver should serve
    private ArrayList<String> routes = new ArrayList<>();

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
    }

    /**
     * Method used to setup web server instance according to configuration options before start
     */
    public void setup() {
        app = Javalin.create();
        if (!staticPath.isEmpty() && Files.exists(Paths.get(staticPath).toAbsolutePath())) {
            app.enableStaticFiles(staticPath);
        }
        app.get("/", ctx -> {
            ctx.result("Server '"+name+"' is listening on port "+port);
        });
    }

    /**
     * Method used to run web server either directly or inside separate thread (new Thread(webserver) )
     */
    public void run() {
        app.start(port);
    }
}
