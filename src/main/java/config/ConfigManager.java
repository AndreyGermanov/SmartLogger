package config;


import java.util.HashMap;

public class ConfigManager {

    private ConfigManager() {}

    private static ConfigManager instance;

    public HashMap<String,HashMap<String,Object>> getDatabaseAdapters() {
        HashMap<String,HashMap<String,Object>> result = new HashMap<>();
        HashMap<String,Object> mysql_local = new HashMap<>();
        mysql_local.put("name","mysql_local");
        mysql_local.put("type","mongodb");
        mysql_local.put("path","/home/andrey/logger/weather");
        mysql_local.put("host","127.0.0.1");
        mysql_local.put("port","27017");
        mysql_local.put("username","root");
        mysql_local.put("password","111111");
        mysql_local.put("database","yandex_weather");

        HashMap<String,HashMap<String,Object>> collections = new HashMap<>();
        HashMap<String,Object> weather_collection = new HashMap<>();
        HashMap<String,HashMap<String,Object>> weather_fields = new HashMap<>();
        HashMap<String,Object> temperature_avg_field = new HashMap<>();
        temperature_avg_field.put("name","temperature_avg");
        temperature_avg_field.put("type","decimal");
        temperature_avg_field.put("length",15);
        temperature_avg_field.put("precision",2);
        HashMap<String,Object> temperature_sum_field = new HashMap<>();
        temperature_sum_field.put("name","temperature_sum");
        temperature_sum_field.put("type","decimal");
        temperature_sum_field.put("length",15);
        temperature_sum_field.put("precision",2);
        HashMap<String,Object> temperature_avg_f_field = new HashMap<>();
        temperature_avg_f_field.put("name","temperature_avg_f");
        temperature_avg_f_field.put("type","decimal");
        temperature_avg_f_field.put("length",15);
        temperature_avg_f_field.put("precision",2);
        HashMap<String,Object> wind_direction_field = new HashMap<>();
        wind_direction_field.put("name","wind_direction");
        wind_direction_field.put("type","string");
        wind_direction_field.put("length",255);
        HashMap<String,Object> id_field = new HashMap<>();
        id_field.put("name","id");
        id_field.put("type","integer");
        weather_fields.put("id",id_field);
        weather_fields.put("temperature_avg",temperature_avg_field);
        weather_fields.put("temperature_avg_f",temperature_avg_f_field);
        weather_fields.put("temperature_sum",temperature_sum_field);
        weather_collection.put("name","weather");
        weather_collection.put("idField","id");
        weather_collection.put("fields",weather_fields);
        collections.put("weather",weather_collection);
        mysql_local.put("collections",collections);

        result.put("mysql_local",mysql_local);
        return result;
    }

    public HashMap<String,Object> getDatabaseAdapter(String name) {
        HashMap<String,HashMap<String,Object>> adapters = getDatabaseAdapters();
        if (adapters.containsKey(name)) return adapters.get(name);
        return null;
    }

    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }
}
