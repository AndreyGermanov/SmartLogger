package archivers;

import org.junit.Test;
import utils.DataMap;

import java.util.HashMap;

public class ZipArchiveExtractorTest {

    @Test
    public void archive() {
        String rootDestDir = "/home/andrey/logger/test/archivers/";
        String sourceDir = "/home/andrey/logger/test/archivers/aggregate_test";
        String destinationDir = rootDestDir+"aggregate_test_extracted";
        HashMap<String,Object> config = DataMap.create("name","aggregate_test_extractor",
                "sourcePath",sourceDir,
                "destinationPath",destinationDir);
        IDataArchiver archiver = new ZipArchiveExtractor(config);
        System.out.println("PROCESSED "+archiver.archive()+" files");
    }
}
