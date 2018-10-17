package archivers;

import org.junit.Test;
import utils.DataMap;

import java.util.HashMap;

public class SendFtpDataArchiverTest {
    @Test
    public void archive() {
        String rootDestDir = "/home/andrey/logger/test/archivers/";
        String sourceDir = "/home/andrey/logger/test/archivers/aggregate_test";
        String destinationDir = rootDestDir + "aggregate_test_extracted";
        HashMap<String, Object> config = DataMap.create("name", "ftp_sender",
                "sourcePath", "/home/andrey/logger/test/archivers/aggregate_test",
                "host", "portal.it-port.ru",
                "port",21,"username","","password","","maxArchiveFilesCount",1,
                "destinationPath", destinationDir);
        IDataArchiver archiver = new SendFtpDataArchiver(config);
        System.out.println("PROCESSED " + archiver.archive() + " files");
    }
}
