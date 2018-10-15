package archivers;

import org.junit.Test;
import utils.DataMap;
import utils.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class FileCopyDataArchiverTest {

    @Test
    public void archive() {
        String rootDestDir = "/home/andrey/logger/test/archivers/";
        String sourceDir = "/home/andrey/logger/aggregators/yandex_weather_golubitskaya_10";
        Path sourcePath = Paths.get(sourceDir);
        String destinationDir = rootDestDir+"aggregate_test";
        Path destinationPath = Paths.get(destinationDir);
        HashMap<String,Object> config = DataMap.create("name","aggregate_test",
                "sourcePath",sourceDir,
                "destinationPath",destinationDir,
                "type","copy");
        IDataArchiver archiver = new FileCopyDataArchiver(config);
        System.out.println("Source files count="+FileUtils.getFolderFilesCount(sourcePath)+",size="+
                FileUtils.getFolderFilesSize(sourcePath));
        System.out.println("ARCHIVED "+archiver.archive()+" files");
        System.out.println("Destination files count="+FileUtils.getFolderFilesCount(destinationPath)+",size="+
                FileUtils.getFolderFilesSize(destinationPath));
        String destinationDir2 = rootDestDir+"/aggregate_test2";
        config.put("sourcePath",destinationDir);
        config.put("destinationPath",destinationDir2);
        config.put("removeSourceAfterArchive",true);
        archiver.configure(config);
        archiver.archive();
    }

}
