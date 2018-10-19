package archivers;

import config.ConfigManager;
import main.LoggerApplication;
import org.junit.Test;
import utils.DataMap;

import java.util.HashMap;

public class SendFtpDataArchiverTest {
    @Test
    public void archive() {
        ConfigManager.getInstance().loadConfig();
        LoggerApplication.getInstance().configure(ConfigManager.getInstance().getConfig());
        String rootDestDir = "/home/andrey/logger/test/archivers/";
        String sourceDir = "/home/andrey/logger/test/archivers/aggregate_test";
        String destinationDir = rootDestDir + "aggregate_test_extracted";
        HashMap<String, Object> config = DataMap.create("name", "ftp_sender",
                "sourcePath", "/home/andrey/logger/test/archivers/aggregate_test",
                "host", "portal.it-port.ru",
                "port",21,"username","","password","","rootPath","",
                "destinationPath", destinationDir);
        IDataArchiver archiver = new SendFtpDataArchiver(config);
        System.out.println("PROCESSED " + archiver.archive() + " files");
    }
}
