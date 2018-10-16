package archivers;

import org.junit.Test;
import utils.DataMap;
import java.util.HashMap;

public class DataZipDataArchiverTest {
    @Test
    public void archive() {
        String rootDestDir = "/home/andrey/logger/test/archivers/";
        String sourceDir = "/home/andrey/logger/aggregators/yandex_weather_golubitskaya_10";
        String destinationDir = rootDestDir+"aggregate_test";
        HashMap<String,Object> config = DataMap.create("name","aggregate_test",
                "sourcePath",sourceDir,
                "destinationPath",destinationDir,
                "maxArchiveFilesCount",100,
                "type","data_zip");
        IDataArchiver archiver = new DataZipDataArchiver(config);
        System.out.println("ARCHIVED "+archiver.archive()+" files");
    }
}
