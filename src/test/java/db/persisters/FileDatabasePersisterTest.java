package db.persisters;

import config.ConfigManager;
import main.LoggerApplication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileDatabasePersisterTest {

    @Before
    public void init() {
        ConfigManager.getInstance().loadConfig();
        LoggerApplication.getInstance().configure(ConfigManager.getInstance().getConfig());
    }

    @Test
    public void persist() {
        IDatabasePersister persister = new FileDatabasePersister("yandex_weather_golubitskaya_5");
        Integer result = persister.persist();
       // Assert.assertNotNull("Should not return null result",result);
       // Assert.assertTrue("Should insert correct number of unique records",result==28);
    }
}
