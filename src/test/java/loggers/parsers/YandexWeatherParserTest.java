package loggers.parsers;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class YandexWeatherParserTest {

    @Test
    public void parse() throws IOException {
        String path = System.getProperty("user.dir")+"/src/test/resources/yandex_weather.html";
        BufferedReader reader = new BufferedReader(new FileReader(path));

        String inputString = reader.lines().reduce("",(s1,s2) -> s1+s2);
        YandexWeatherParser parser = new YandexWeatherParser(inputString);
        HashMap<String,?> result = parser.parse();
        org.junit.Assert.assertTrue("Should contain 'temperature' field",result.containsKey("temperature"));
        org.junit.Assert.assertTrue("Should contain 'water_temperature' field",result.containsKey("water_temperature"));
        org.junit.Assert.assertTrue("Should contain 'humidity' field",result.containsKey("humidity"));
        org.junit.Assert.assertTrue("Should contain 'wind_speed' field",result.containsKey("wind_speed"));
        org.junit.Assert.assertTrue("Should contain 'wind_direction' field",result.containsKey("wind_direction"));
        org.junit.Assert.assertEquals("Should contain correct temperature",17.0,
                Double.valueOf(result.get("temperature").toString()),0);
        org.junit.Assert.assertEquals("Should contain correct water_temperature",18.0,
                Double.valueOf(result.get("water_temperature").toString()),0);
        org.junit.Assert.assertEquals("Should contain correct humidity",78.0,
                Double.valueOf(result.get("humidity").toString()),0);
        org.junit.Assert.assertEquals("Should contain correct wind_speed",4.8,
                Double.valueOf(result.get("wind_speed").toString()),0);
        org.junit.Assert.assertEquals("Should contain correct wind_direction","С",
                result.get("wind_direction").toString());
    }
}
