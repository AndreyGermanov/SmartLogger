package loggers;
import com.google.gson.Gson;
import config.ConfigManager;
import main.LoggerApplication;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import utils.DataMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

class YandexWeatherLoggerMoc extends YandexWeatherLogger {

    YandexWeatherLoggerMoc(String name, String placeName) {
        super(name, placeName);
    }
    YandexWeatherLoggerMoc(HashMap<String,Object> config) {
        super(config);
    }

    public HashMap<String,Object> fakeLastRecord;

    public HashMap<String,Object> getLastRecord() {
        if (fakeLastRecord != null)
            return fakeLastRecord;
        else
            return super.getLastRecord();
    }
}

public class YandexWeatherLoggerTest {

    @Before
    public void init() {
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.loadConfig();
        LoggerApplication.getInstance().configure(configManager.getConfig());
    }

    private YandexWeatherLoggerMoc logger = new YandexWeatherLoggerMoc(DataMap.create("name","yandex_weather_golubitskaya","place","golubitskaya"));
    private Gson gson = new Gson();

    @Test
    public void getJson() {
        HashMap<String,Object> record = logger.readRecord();
        String json = logger.getJson(record);
        HashMap<String,Object> result = gson.fromJson(json,HashMap.class);
        org.junit.Assert.assertNotNull("Should return not null result",result);
        org.junit.Assert.assertTrue("Should contain 'timestamp' of record",result.containsKey("timestamp"));
        org.junit.Assert.assertTrue("Should contain 'temperature' field",result.containsKey("temperature"));
        org.junit.Assert.assertTrue("Should contain 'water_temperature' field",result.containsKey("water_temperature"));
        org.junit.Assert.assertTrue("Should contain 'humidity' field",result.containsKey("humidity"));
        org.junit.Assert.assertTrue("Should contain 'pressure' field",result.containsKey("pressure"));
        org.junit.Assert.assertTrue("Should contain 'wind_speed' field",result.containsKey("wind_speed"));
        org.junit.Assert.assertTrue("Should contain 'wind_direction' field",result.containsKey("wind_direction"));
    }

    @Test
    public void getRecordPath() {
        String cachePath = LoggerApplication.getInstance().getCachePath();
        HashMap<String,Object> record = logger.readRecord();
        org.junit.Assert.assertNotNull("Should return not null result",record);
        String path = logger.getRecordPath(record);
        LocalDateTime date = LocalDateTime.ofEpochSecond(new Long(record.get("timestamp").toString()),0,ZoneOffset.UTC);
        String expectedPath = cachePath+"/"+logger.getName()+"/data/"+date.getYear()+"/"+date.getMonthValue()+"/"
                + date.getDayOfMonth() + "/" + date.getHour() + "/" + date.getMinute() + "/" + date.getSecond()+".json";
        org.junit.Assert.assertEquals("Should contain correct path",path,expectedPath);
    }

    @Test
    public void writeRecord() throws IOException {
        HashMap<String,Object> record = logger.readRecord();
        org.junit.Assert.assertNotNull("Should return not null result",record);
        String cachePath = LoggerApplication.getInstance().getCachePath();
        LocalDateTime date = LocalDateTime.ofEpochSecond(new Long(record.get("timestamp").toString()),0,ZoneOffset.UTC);
        String expectedPath = cachePath+"/"+logger.getName()+"/data/"+date.getYear()+"/"+date.getMonthValue()+"/"
                + date.getDayOfMonth() + "/" + date.getHour() + "/" + date.getMinute() + "/" + date.getSecond()+".json";
        logger.writeRecord(record);
        Path path = Paths.get(expectedPath);
        BufferedReader reader = Files.newBufferedReader(path);
        String wroteRecord = reader.lines().reduce((s1,s2) -> s1+s2).get();
        Assert.assertEquals("Should write correct record",logger.getJson(record),wroteRecord);
    }

    @Test
    public void isRecordChanged() {
        HashMap<String,Object> lastRecord = new HashMap<>();
        logger.fakeLastRecord = lastRecord;
        org.junit.Assert.assertFalse("Should return 'false' if compare two empty records",
                logger.isRecordChanged(new HashMap<>()));
        HashMap<String,Object> record = new HashMap<>();
        record.put("temperature",15.0);
        record.put("water_temperature",18.0);
        org.junit.Assert.assertTrue("Should return 'true' if compare empty record with not empty record",
                logger.isRecordChanged(record));
        lastRecord.put("temperature",15.0);
        lastRecord.put("water_temperature",18.0);
        org.junit.Assert.assertFalse("Should return 'false' if compare records with the same values",
                logger.isRecordChanged(record));
        lastRecord.put("boo",null);
        org.junit.Assert.assertTrue("Should return true if previous record contains more fields than current",
                logger.isRecordChanged(record));
    }

    @Test
    public void log() {
        logger.log();
    }

    @Test
    public void readRecord() {
        HashMap<String,Object> result = logger.readRecord();
        org.junit.Assert.assertNotNull("Should not return null",result);
        org.junit.Assert.assertTrue("Should contain 'timestamp' field",result.containsKey("timestamp"));
        org.junit.Assert.assertTrue("Should contain 'temperature' field",result.containsKey("temperature"));
        org.junit.Assert.assertTrue("Should contain 'water_temperature' field",result.containsKey("water_temperature"));
        org.junit.Assert.assertTrue("Should contain 'humidity' field",result.containsKey("humidity"));
        org.junit.Assert.assertTrue("Should contain 'wind_speed' field",result.containsKey("wind_speed"));
        org.junit.Assert.assertTrue("Should contain 'wind_direction' field",result.containsKey("wind_direction"));
    }

    @After
    public void finish() {
        logger.fakeLastRecord = null;
    }
}
