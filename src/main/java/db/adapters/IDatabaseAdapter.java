package db.adapters;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Interface which should implement any database adapter
 */
public interface IDatabaseAdapter {
    void configure(HashMap<String,Object> config);
    Integer insert(String collectionName,ArrayList<HashMap<String,Object>> data);
    Integer update(String collectionName,ArrayList<HashMap<String,Object>> data);
    ArrayList<HashMap<String,Object>> select(String sql,String collectionName);
}
