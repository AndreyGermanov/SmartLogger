package aggregators;

import config.ConfigManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

public class SimpleFileDataAggregatorTest {

    @Before
    public void init() {
        ConfigManager.getInstance().loadConfig();
    }

    @Test
    public void aggregate() {
        SimpleFileDataAggregator aggregator = new SimpleFileDataAggregator("yandex_weather_golubitskaya_10");
        aggregator.aggregate();
    }
}
