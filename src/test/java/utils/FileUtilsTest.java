package utils;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtilsTest {

    private String folder = "/home/andrey/logger/aggregators/yandex_weather_golubitskaya_5";

    @Test
    public void removeFolder() {
        Path path = Paths.get(folder);
        FileUtils.removeFolder(path,true);
    }

    @Test
    public void getFolderFilesSize() {
        System.out.println(FileUtils.getFolderFilesSize(Paths.get(folder)));
    }

    @Test
    public void getFolderFilesCount() {
        System.out.println(FileUtils.getFolderFilesCount(Paths.get(folder)));
        System.out.println(FileUtils.getFolderFilesCount(Paths.get("/home/andrey/logger/test/archivers/aggregate_test/logs")));
    }
}
