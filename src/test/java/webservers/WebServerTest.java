package webservers;

import config.ConfigManager;
import main.LoggerApplication;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class WebServerTest {

    @Test
    public void run() {
        ConfigManager.getInstance().loadConfig();
        LoggerApplication.getInstance().configure(ConfigManager.getInstance().getConfig());
        HashMap<String,Object> config = ConfigManager.getInstance().getConfigNode("webservers","dashboard");
        if (config == null) Assert.fail();
        IWebServer server = new WebServer(config);
        server.setup();
        server.run();
    }
}
