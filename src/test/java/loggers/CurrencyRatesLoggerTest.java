package loggers;

import config.ConfigManager;
import main.LoggerApplication;
import org.junit.Before;
import org.junit.Test;
import utils.DataMap;

public class CurrencyRatesLoggerTest {

    @Before
    public void init() {
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.loadConfig();
        LoggerApplication.getInstance().configure(configManager.getConfig());
    }

    @Test
    public void log() {
        CurrencyRatesLogger logger = new CurrencyRatesLogger(DataMap.create("name","currency_logger","base","USD"));
        logger.log();

    }
}
