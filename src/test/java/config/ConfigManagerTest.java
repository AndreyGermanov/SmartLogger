package config;

import org.junit.Assert;
import org.junit.Test;

public class ConfigManagerTest {

    @Test
    public void loadConfig() {
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.loadConfig();
        Assert.assertNotNull("Should load data loggers",configManager.getDataLogger("yandex_weather_golubitskaya"));
        Assert.assertNotNull("Should load data aggregators",configManager.getDataAggregator("yandex_weather_golubitskaya_5"));
        Assert.assertNotNull("Should load database adapters",configManager.getDatabaseAdapter("mysql_local"));
        Assert.assertNotNull("Should load database persisters",configManager.getDatabasePersister("yandex_weather_golubitskaya_5"));
    }
}
