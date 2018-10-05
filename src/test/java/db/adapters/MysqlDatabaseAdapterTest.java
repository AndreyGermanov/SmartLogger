package db.adapters;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class MysqlDatabaseAdapterTest {

    private IDatabaseAdapter adapter = DatabaseAdapter.get("mysql_local");

    @Test
    public void insert() {
        ArrayList<HashMap<String,Object>> rows = new ArrayList<>();
        HashMap<String,Object> row1 = new HashMap<>();
        row1.put("temperature_avg",14.5);
        row1.put("wind_direction","СВ");
        row1.put("temperature_sum",43);
        row1.put("temperature_avg_f",48);
        row1.put("fake_field","some val");
        HashMap<String,Object> row2 = new HashMap<>();
        row2.put("temperature_avg",11.3124534);
        row2.put("wind_direction","С");
        row2.put("temperature_sum",43.15);
        row2.put("temperature_avg_f",35);
        row2.put("fake_field","some val2");
        rows.add(row1);
        rows.add(row2);
        Integer result = adapter.insert("weather",rows);
        System.out.println(result);

    }
}
