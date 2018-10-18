package archivers;

import config.ConfigManager;
import main.LoggerApplication;
import org.junit.Test;
import utils.DataMap;
import java.util.HashMap;

public class DataZipDataArchiverTest {
    @Test
    public void archive() {
        ConfigManager.getInstance().loadConfig();
        LoggerApplication.getInstance().configure(ConfigManager.getInstance().getConfig());
        String rootDestDir = "/home/andrey/logger/test/archivers/";
        String sourceDir = "/home/andrey/logger/aggregators/yandex_weather_golubitskaya_10";
//        String destinationDir = rootDestDir+"aggregate_test";
        HashMap<String,Object> config = DataMap.create("name","aggregate_test",
                "sourcePath",sourceDir,
                "maxArchiveFilesCount",100,
                "type","data_zip");
        IDataArchiver archiver = new DataZipDataArchiver(config);
        System.out.println("ARCHIVED "+archiver.archive()+" files");
    }
}
