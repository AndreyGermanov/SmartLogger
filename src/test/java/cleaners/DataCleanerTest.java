package cleaners;

import archivers.DataArchiver;
import com.google.gson.Gson;
import config.ConfigManager;
import db.persisters.FileDatabasePersister;
import main.LoggerApplication;
import main.LoggerService;
import org.junit.Before;
import org.junit.Test;
import utils.DataList;
import utils.DataMap;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class DataCleanerTest {

    @Before
    public void init() throws IOException  {
        HashMap<String,Object> config = DataMap.create(
            "appPath", "/tmp/cleaner_test/work",
            "statusPath", "status",
            "cachePath", "cache",
            "logPath", "logs",
            "syslog",  DataMap.create(
                "rotateLogs", true,
                "maxLogFileSize", 2024,
                "maxLogFiles", 5,
                "compressArchives", true
            ),
            "archivers", DataMap.create(
                "archiver1", DataMap.create(
                    "name", "archiver1",
                    "enabled", false,
                    "pollPeriod", 60,
                    "type","data_zip",
                    "sourcePath", "/tmp/path1",
                    "destinationPath", "/tmp/path2"
                )
            ),
            "persisters", DataMap.create(
                "persister1", DataMap.create(
                    "name", "persister1",
                    "databaseAdapter", "mongodb_local",
                    "collectionName", "weather_900",
                    "sourcePath", "/tmp/path2",
                    "writeDuplicates", false,
                    "fillDataGaps", false,
                    "rowsPerRun", 0,
                    "enabled", false,
                    "pollPeriod", 1000
                )
            ),
            "cleaners", DataMap.create(
                "cleaner1", DataMap.create(
                    "name","cleaner1",
                    "sourcePath","/tmp/source_to_remove",
                    "consumers", DataList.create(
                            "persisters_persister1",
                            "archivers_archiver1"
                    )
                )
            )
        );
        Gson gson = new Gson();
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.loadConfig(config);
        LoggerApplication.getInstance().configure(configManager.getConfig());
        try {
            Files.createDirectories(Paths.get("/tmp/cleaner_test/work/status/archivers/archiver1"));
            Files.createDirectories(Paths.get("/tmp/cleaner_test/work/status/persisters/persister1"));
        } catch (Exception e) {}
        FileWriter f = new FileWriter("/tmp/cleaner_test/work/status/archivers/archiver1/last_record");
        f.write("1541030400 /tmp/file1");f.flush();f.close();
        f = new FileWriter("/tmp/cleaner_test/work/status/persisters/persister1/last_record");
        f.write(gson.toJson(DataMap.create("timestamp","1540512000","f1","value")));f.flush();f.close();
        LoggerService.getInstance().start();
        ((FileDatabasePersister)LoggerService.getInstance().getCronjobTask("persisters_persister1")).readAndSetLastRecord();
        ((DataArchiver)LoggerService.getInstance().getCronjobTask("archivers_archiver1")).readAndSetLastRecord();
    }

    @Test
    public void clean() {
        IDataCleaner cleaner = new DataCleaner("cleaner1");
        cleaner.clean();
    }
}
