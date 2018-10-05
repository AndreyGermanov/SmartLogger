package readers;

import com.google.gson.Gson;
import main.ISyslog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class provide functions to get information from downloaded data folder.
 */
public class FileDataReader implements IDataReader {

    // Path of root directory
    private String filePath;
    // Cached list of file paths. Key of map is 'timestamp', value is full file path
    private NavigableMap<Long,Path> filesList = new TreeMap<>();
    // Instance of internal error logger used to log exceptions and other issues to file
    private ISyslog syslog;

    /**
     * Class constructor
     * @param filePath Full path to root folder
     */
    public FileDataReader(String filePath, ISyslog syslog) {
        this.filePath = filePath;
        this.syslog = syslog;
    }

    /**
     * Method fills "filesList" with all valid files inside folder
     * @return HashMap of files, ordered by timestamp
     */
    NavigableMap<Long,Path> getFilesList() {
        return getFilesList(false);
    }

    /**
     * Method fills "filesList" with data files inside folder which are inside specified date range
     * @param startDate Timestamp of start date
     * @param endDate Timestamp of end date
     * @return HashMap of files, ordered by timestamp
     */
    NavigableMap<Long,Path> getFilesList(Long startDate, Long endDate) {
        NavigableMap<Long,Path> result = new TreeMap<>();
        if (startDate>endDate) return result;
        DataRange range = getRangeBounds(startDate,endDate);
        if (range.startDate == 0 || range.endDate == 0) return result;
        try {
            result = getFilesList().subMap(range.startDate, true, range.endDate, true);
        } catch (Exception e) {};
        return result;
    }

    /**
     * Base method to fill "filesList" with all valid files inside folder
     * @param refreshCache Should this method reread files list from filesystem or just return cached one
     * @return HashMap of files, ordered by timestamp
     */
    private NavigableMap<Long,Path> getFilesList(boolean refreshCache) {
        NavigableMap<Long,Path> result = filesList;
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return result;
        try {
            if (refreshCache || filesList.size() == 0) {
                result = Files.walk(path)
                        .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
                        .collect(Collectors.toMap(this::getPathTimestamp, p -> p, (v1, v2) -> {
                            throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                        }, TreeMap::new));
                filesList = result;
            }
        } catch (IOException e) {
            syslog.logException(e,this,"getFilesList");
        }
        return result;
    }

    /**
     * Method construct timestamp of data file using it full path (if path correctly formatted)
     * @param path: Source path
     * @return Generated timestamp
     */
    private Long getPathTimestamp(Path path) {
        String[] parts = path.toString().split("/");
        int second = new Integer(parts[parts.length-1].split("\\.")[0]);
        int minute = new Integer(parts[parts.length-2]);
        int hour = new Integer(parts[parts.length-3]);
        int day = new Integer(parts[parts.length-4]);
        int month = new Integer(parts[parts.length-5]);
        int year = new Integer(parts[parts.length-6]);
        return LocalDateTime.of(year,month,day,hour,minute,second).toEpochSecond(ZoneOffset.UTC);
    }

    /**
     * Method returns closest date range of data, based on provided date range
     * @param startDate Timestamp of start
     * @param endDate Timestamp of end
     * @return Calculated date range
     */
    private DataRange getRangeBounds(Long startDate,Long endDate)  {
        return new DataRange(getStartRangeBound(startDate),getEndRangeBound(endDate));
    }

    /**
     * Method finds timestamp of record, which is closest to specified date (same date or later)
     * @param value Input timestamp
     * @return Closest timestamp of data (can be the same timestamp or later closest one)
     */
    private Long getStartRangeBound(Long value) {
        return getRangeBound(value,true);
    }

    /**
     * Method finds timestamp of record, which is closest to specified date (same date or earlier)
     * @param value Input timestamp
     * @return Closest timestamp of data (can be the same timestamp or earlier closest one)
     */
    private Long getEndRangeBound(Long value) {
        return getRangeBound(value,false);
    }

    /**
     * Base method which returns timestamp of record which is closest to provided one
     * @param value Input timestamp
     * @param findHigher : Direction: if true, than will return later closest timestamp, otherwise earlier closest timestamp
     * @return Closest timestamp of data based on provided options
     */
    private Long getRangeBound(Long value, boolean findHigher) {
        NavigableMap<Long,Path> source = getFilesList();
        if (source.size()==0) return 0L;
        if (source.containsKey(value)) return value;
        Long result = findHigher ? source.firstKey() : source.lastKey();
        Long closeKey = findHigher ? source.higherKey(value) : source.lowerKey(value);
        if (closeKey != null) result = closeKey;
        return result;
    }

    /**
     * Returns Time range of data. Includes first date and last date
     * @return Range object with timestamp of first record and timestamp of last record
     */
    @Override
    public DataRange getRange() {
        NavigableMap<Long,Path> source = getFilesList();
        if (source.size()==0) return new DataRange();
        return new DataRange(source.firstKey(),source.lastKey());
    }

    /**
     * Returns statistical information about data: Date range and number of records
     * @return DataStats object with start timestamp, end timestamp and number of records
     */
    @Override
    public DataStats getDataStats(boolean refreshCache) {
        return getDataStats(0L,0L,refreshCache);
    }

    /**
     * Returns statistical information about data inside requested date range, it includes timestamp of first
     * record, timestamp of last record and number of records in this period.
     * @param startDate Start date
     * @param endDate End date
     * @return DataStats object with start timestamp, end timestamp and number of records
     */
    @Override
    public DataStats getDataStats(Long startDate, Long endDate, boolean refreshCache) {
        if (refreshCache) getFilesList(refreshCache);
        DataRange range = getRangeBounds(startDate,endDate);
        return new DataStats(range,getFilesList(startDate,endDate).size());
    }

    /**
     * Method read data from all files and returns it as a HashMap, ordered by timestamp
     * @return HashMap with timestamp as key and data record (HashMap<String,Object>) as value
     */
    @Override
    public NavigableMap<Long,HashMap<String,Object>> getData(boolean refreshCache) {
        DataRange range = getRange();
        return getData(range.startDate,range.endDate,refreshCache);
    }

    /**
     * Method read data from files inside specified date range and returns it as a HashMap, ordered by timestamp
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return HashMap with timestamp as key and data record (HashMap<String,Object>) as value
     */
    @Override
    public NavigableMap<Long,HashMap<String,Object>> getData(Long startDate, Long endDate, boolean refreshCache) {
        NavigableMap<Long,HashMap<String,Object>> result = new TreeMap<>();
        DataStats stats = getDataStats(startDate,endDate,refreshCache);
        if (stats.count==0) return result;
        getFilesList(startDate,endDate).entrySet().parallelStream().forEach( entry -> {
            HashMap<String,Object> record = getDataRecord(entry.getValue());
            if (record == null) return;
            Long timestamp = new Long(record.get("timestamp").toString());
            record.remove("timestamp");
            synchronized (this) {
                result.put(timestamp, record);
            }
        });
        return result;
    }

    /**
     * Method used to read single record from data file
     * @param path Path to datafile
     * @return record as HashMap<String,Object> or null in case of errors
     */
    private HashMap<String,Object> getDataRecord(Path path) {
        Gson gson = new Gson();
        try {
            if (!Files.exists(path) || Files.size(path) == 0) return null;
            String content = Files.newBufferedReader(path).readLine();
            if (content.isEmpty()) return null;
            HashMap<String,Object> record = gson.fromJson(content,HashMap.class);
            if (record.size()==0 || !record.containsKey("timestamp")) return null;
            return record;
        } catch (Exception e) {
            syslog.logException(e,this,"getDataRecord");
            return null;
        }
    }

    /**
     * Class which holds information about date range
     */
    public static class DataRange {
        public Long startDate=0L;
        public Long endDate=0L;
        DataRange(Long startDate, Long endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
        DataRange() {}
    }

    /**
     * Class which holds statistical information about data folder
     */
    public class DataStats {
        public DataRange range;
        public int count;
        DataStats(DataRange range,int count) {
            this.range = range;
            this.count = count;
        }
    }
}