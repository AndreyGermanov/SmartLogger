package config;


import utils.DataMap;

import java.util.HashMap;

public class ConfigManager {

    private ConfigManager() {}

    private static ConfigManager instance;

    public HashMap<String,HashMap<String,Object>> getDatabaseAdapters() {
        HashMap<String,HashMap<String,Object>> result = new HashMap<>();
        HashMap<String,Object> collections = DataMap.create(
                "weather",DataMap.create(
                        "name","weather",
                        "idField","id",
                        "fields",DataMap.create(
                                "id",DataMap.create(
                                        "name","id",
                                        "type","integer"
                                ),
                                "timestamp",DataMap.create(
                                        "name", "timestamp",
                                        "type", "integer"
                                ),
                                "temperature_avg",DataMap.create(
                                        "name","temperature_avg",
                                        "type","decimal",
                                        "length", 15,
                                        "precision",2
                                ),
                                "temperature_sum",DataMap.create(
                                        "name","temperature_sum",
                                        "type","decimal",
                                        "length",15,
                                        "precision",2
                                ),
                                "temperature_avg_f",DataMap.create(
                                        "name","temperature_avg_f",
                                        "type","decimal",
                                        "length","15",
                                        "precision",2
                                ),
                                "wind_direction",DataMap.create(
                                        "name","wind_direction",
                                        "type","string",
                                        "length",255
                                )
                        )
                )
        );
        HashMap<String,Object> mysql_local = DataMap.create(
                "name","mysql_local",
                "type","mysql",
                "path","/home/andrey/logger/weather",
                "host","127.0.0.1",
                "port","3306",
                "username","root",
                "password","111111",
                "database","yandex_weather",
                "collections",collections
        );
        result.put("mysql_local",mysql_local);
        return result;
    }

    HashMap<String,HashMap<String,Object>> getDatabasePersisters() {
        return DataMap.create(
                "yandex_weather_golubitskaya_5",DataMap.create(
                    "name","yandex_weather_golubitskaya_5",
                    "databaseAdapter","mysql_local",
                    "collectionName","weather",
                    "sourcePath","/home/andrey/logger/aggregators/yandex_weather_golubitskaya_5/data",
                    "writeDuplicates", false,
                    "fillDataGaps", false,
                    "rowsPerRun", 0
            )
        );
    }

    public HashMap<String,Object> getDatabaseAdapter(String name) {
        HashMap<String,HashMap<String,Object>> adapters = getDatabaseAdapters();
        if (adapters.containsKey(name)) return adapters.get(name);
        return null;
    }

    public HashMap<String,Object> getDatabasePersister(String name) {
        HashMap<String,HashMap<String,Object>> persisters = getDatabasePersisters();
        if (persisters.containsKey(name)) return persisters.get(name);
        return null;
    }

    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }
}
