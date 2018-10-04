package readers;

import main.Syslog;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.NavigableMap;

import static org.junit.Assert.*;

public class FileDataReaderTest implements Syslog.Loggable {

    private String filePath = "/home/andrey/logger/yandex_weather_golubitskaya";
    private FileDataReader reader = new FileDataReader(filePath,new Syslog(this));

    @Test
    public void getFilesList() throws IOException {
        NavigableMap<Long,Path> list = reader.getFilesList();
        assertNotNull("Should not be null",list);
        assertTrue("Result should contain items",list.size()>0);
        FileDataReader reader2 = new FileDataReader("/tmp/nonexists",new Syslog(this));
        assertNotNull("Should not be null",reader2.getFilesList());
        assertFalse("Result should contain 0 items",reader2.getFilesList().size()>0);
        list = reader.getFilesList(1538389455L,1538389800L);
        assertEquals(28,list.size());

    }

    @Test
    public void getDataStat() throws IOException {
        FileDataReader.DataStats stats = reader.getDataStats(false);
        assertEquals("Should return correct start date", 1538389471, (long) stats.range.startDate);
        assertEquals("Should return correct end date", 1538592030, (long) stats.range.endDate);
        assertEquals("Should return correct count of items", 25004, stats.count);
        stats = reader.getDataStats(1538389455L,1538389805L,false);
        assertEquals("Should return correct start date", 1538389471, (long) stats.range.startDate);
        assertEquals("Should return correct end date", 1538389805, (long) stats.range.endDate);
        assertEquals("Should return correct count of items", 32, stats.count);
    }

    @Test
    public void getData() throws IOException {
        NavigableMap<Long,HashMap<String,Object>> result = reader.getData(false);
        assertEquals("Should parse correct number of records", 25004, result.size());
        HashMap<String,Object> record = result.get(1538389710L);
        assertTrue("Should contain 'temperature' field",record.containsKey("temperature"));
        assertTrue("Should contain 'water_temperature' field",record.containsKey("water_temperature"));
        assertTrue("Should contain 'humidity' field",record.containsKey("humidity"));
        assertTrue("Should contain 'pressure' field",record.containsKey("pressure"));
        assertTrue("Should contain 'wind_speed' field",record.containsKey("wind_speed"));
        assertTrue("Should contain 'wind_direction' field",record.containsKey("wind_direction"));
        assertEquals("Should have correct data on start", 21.0, new Double(record.get("temperature").toString()), 0.0);
        record = result.get(1538421433L);
        assertEquals("Should have correct data on end", 19.0, new Double(record.get("temperature").toString()), 0.0);
        result = reader.getData(1538389455L,1538389805L,false);
        assertEquals("Should return correct count of items", 32, result.size());
        record = result.get(result.lastKey());
        assertEquals("Should have correct data on end", 5.6, new Double(record.get("wind_speed").toString()), 0.0);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getSyslogPath() {
        return null;
    }
}
