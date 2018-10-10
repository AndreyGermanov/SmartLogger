package readers;

import java.util.HashMap;
import java.util.NavigableMap;

/**
 * Interface which all data readers must implement
 */
public interface IDataReader {
    static FileDataReader.DataRange getDataRange(Long startDate, Long endDate) {
        return new FileDataReader.DataRange(startDate,endDate);
    }
    FileDataReader.DataRange getRange();
    FileDataReader.DataStats getDataStats(boolean refreshCache);
    FileDataReader.DataStats getDataStats(Long startDate, Long endDate, boolean refreshCache);
    NavigableMap<Long,HashMap<String,Object>> getData(boolean refreshCache);
    NavigableMap<Long,HashMap<String,Object>> getData(Long startDate,boolean refreshCache);
    NavigableMap<Long,HashMap<String,Object>> getData(Long startDate, Long endDate, boolean refreshCache);
}
