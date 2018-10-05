package aggregators;

import org.junit.Test;

import java.util.HashMap;

public class SimpleFileDataAggregatorTest {

    @Test
    public void aggregate() {
        SimpleFileDataAggregator aggregator = new SimpleFileDataAggregator("yandex_weather_golubitskaya_5",
                "/home/andrey/logger/yandex_weather_golubitskaya/data");
        HashMap<String,Object> config = new HashMap<>();
        config.put("aggregationPeriod",5);
        HashMap<String,HashMap<String,Object>> fieldDefs = new HashMap<>();
        HashMap<String,Object> field_temperature_avg = new HashMap<>();
        field_temperature_avg.put("name","temperature_avg");
        field_temperature_avg.put("aggregate_function","average");
        field_temperature_avg.put("expression","temperature");
        fieldDefs.put("temperature_avg",field_temperature_avg);
        HashMap<String,Object> field_temperature_sum = new HashMap<>();
        field_temperature_sum.put("name","temperature_sum");
        field_temperature_sum.put("aggregate_function","sum");
        field_temperature_sum.put("expression","temperature");
        fieldDefs.put("temperature_sum",field_temperature_sum);
        HashMap<String,Object> field_temperature_count = new HashMap<>();
        field_temperature_count.put("name","temperature_count");
        field_temperature_count.put("aggregate_function","count");
        field_temperature_count.put("expression","temperature");
        fieldDefs.put("temperature_count",field_temperature_count);
        HashMap<String,Object> field_temperature_avg_fg = new HashMap<>();
        field_temperature_avg_fg.put("name","temperature_avg_F");
        field_temperature_avg_fg.put("aggregate_function","average");
        field_temperature_avg_fg.put("expression","temperature*1.8+32");
        fieldDefs.put("temperature_avg_F",field_temperature_avg_fg);
        config.put("fields",fieldDefs);
        aggregator.configure(config);
        aggregator.aggregate();
    }
}
