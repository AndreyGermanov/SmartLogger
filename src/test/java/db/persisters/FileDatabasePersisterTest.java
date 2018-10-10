package db.persisters;

import org.junit.Assert;
import org.junit.Test;

public class FileDatabasePersisterTest {

    @Test
    public void persist() {
        IDatabasePersister persister = new FileDatabasePersister("yandex_weather_golubitskaya_5");
        Integer result = persister.persist();
        Assert.assertNotNull("Should not return null result",result);
        Assert.assertTrue("Should insert correct number of unique records",result==28);
    }
}
