package main;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SyslogTest {

    private String logPath = "/home/andrey/logger/yandex_weather_golubitskaya/logs";
    private Syslog logger;

    @Before
    public void init() {
        logger = new Syslog(new Syslog.Loggable() {
            @Override
            public String getName() {
                return "test_source";
            }
            @Override
            public String getSyslogPath() {
                return logPath;
            }
        });
    }

    @Test
    public void log() {
        logger.log(Syslog.LogLevel.ERROR,"This is biggest error","YandexWeatherParser","parse");
        org.junit.Assert.assertTrue("Should write to log file",Files.exists(Paths.get(logPath+"/error.log")));
    }

    @After
    public void finish() {
        Path path =  Paths.get(logPath+"/error.log");
        if (Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
