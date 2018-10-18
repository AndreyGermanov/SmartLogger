package archivers;

import config.ConfigManager;
import main.LoggerApplication;
import org.junit.Test;
import utils.DataMap;
import utils.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class FileZipDataArchiverTest {
        @Test
        public void archive() {
            ConfigManager.getInstance().loadConfig();
            LoggerApplication.getInstance().configure(ConfigManager.getInstance().getConfig());
            String rootDestDir = "/home/andrey/logger/test/archivers/";
            String sourceDir = "/home/andrey/logger/aggregators/yandex_weather_golubitskaya_10";
            Path sourcePath = Paths.get(sourceDir);
            String destinationDir = rootDestDir+"aggregate_test";
            Path destinationPath = Paths.get(destinationDir);
            HashMap<String,Object> config = DataMap.create("name","aggregate_test",
                    "sourcePath",sourceDir,
                    "destinationPath",destinationDir,
                    "type","zip");
            IDataArchiver archiver = new FileZipDataArchiver(config);
            System.out.println("ARCHIVED "+archiver.archive()+" files");
        }
}
