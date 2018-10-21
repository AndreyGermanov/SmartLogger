package aggregators;

import config.ConfigManager;
import main.LoggerApplication;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

public class SimpleFileDataAggregatorTest {

    @Before
    public void init() {
        ConfigManager.getInstance().loadConfig();
        LoggerApplication.getInstance().configure(ConfigManager.getInstance().getConfig());
    }

    @Test
    public void aggregate() {
        SimpleFileDataAggregator aggregator = new SimpleFileDataAggregator("yandex_weather_golubitskaya_900");
        aggregator.aggregate();
    }
}
