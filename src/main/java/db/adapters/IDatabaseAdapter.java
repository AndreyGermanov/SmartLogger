package db.adapters;

import java.util.ArrayList;
import java.util.HashMap;

interface IDatabaseAdapter {
    void configure(HashMap<String,Object> config);
    Integer insert(String collectionName,ArrayList<HashMap<String,Object>> data);
    Integer update(String collectionName,ArrayList<HashMap<String,Object>> data);
}
