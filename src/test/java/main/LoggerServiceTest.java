package main;

import config.ConfigManager;
import org.junit.Test;

public class LoggerServiceTest {

    @Test
    public void start() throws Exception {
        LoggerService service = LoggerService.getInstance();
        ConfigManager.getInstance().loadConfig();
        service.start();
        Thread.sleep(60000);
    }

}
