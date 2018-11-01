package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import main.ISyslog;
import main.LoggerApplication;
import utils.DataMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used to manage system configuration
 */
public class ConfigManager implements ISyslog.Loggable {

    // Link to single instance of this object
    private static ConfigManager instance;
    // Link to loaded configuration object
    private HashMap<String,Object> config;
    // Path to main config file
    private String configPath = "config/main.json";

    /**
     * Class constructor
     */
    private ConfigManager() { }

    /**
     * Method used to load singleton instance of this class
     * @return instance of Config manager
     */
    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    /**
     * Method used to set path to main config file, which overrides default config path "config/main.json"
     * @param configPath Path to config file
     */
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    /**
     * Method which used to load configuration from files to "config" object
     */
    public void loadConfig() {
        Path path = Paths.get(configPath).toAbsolutePath();
        if (!Files.exists(path)) {
            System.err.println("Config file not found: '"+path.toString()+"'");
            System.exit(0);
        } else {
            config = readConfigFile(path);
        }
    }

    /**
     * Method defines default empty configuration, if no configuration found on disk
     * @return HashMap with default configuration object
     */
    private HashMap<String,Object> getDefaultConfig() {
        return DataMap.create("loggers",new HashMap<String,Object>(),
                "aggregators", new HashMap<String,Object>(),
                "adapters", new HashMap<String,Object>(),
                "persisters", new HashMap<String,Object>(),
                "archivers", new HashMap<String,Object>(),
                "extractors", new HashMap<String,Object>());
    }

    /**
     * Method writes configuration object to file as JSON
     * @param path Path to file
     * @param config Configuration object
     */
    private void writeConfigToFile(Path path, HashMap<String,Object> config) {
        if (config == null) return;
        try {
            if (!Files.exists(path.getParent()))
                Files.createDirectories(path.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            BufferedWriter writer = Files.newBufferedWriter(path,StandardOpenOption.CREATE);
            writer.write(gson.toJson(config));
            writer.close();
        } catch (IOException e) {
            System.err.println("Could not write config file '"+path.toString()+"'. "+
                    "Error message: '"+e.getMessage()+"'");
            System.exit(1);
        }
    }

    /**
     * Method used to read configuration from file
     * @param path Path to configuration file
     * @return Configuration object as HashMap
     */
    private HashMap<String,Object> readConfigFile(Path path) {
        if (Files.notExists(path)) return null;
        try {
            BufferedReader reader = Files.newBufferedReader(path);
            String configString = reader.lines().reduce((s,s1) -> s+=s1).orElse("");
            reader.close();
            Gson gson = new Gson();
            HashMap<String,Object>  configMap = gson.fromJson(configString,HashMap.class);
            if (configMap == null) return null;
            return parseConfig(path,(HashMap<String,Object>)configMap.clone());
        } catch (Exception e) {
            System.err.println("Could not parse config from file '"+path.toString()+"'. "+
            "Error message: "+ Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).reduce((s, s1)->s+="\n"+s1));
            System.exit(1);
            return null;
        }
    }

    /**
     * Method used to recursively parse configuration object to process all "#include" directives,
     * which includes content from additional files
     * @param rootPath: Path of file, from which source config loaded
     * @param config: Source config
     * @return Config object after processing and inclusion of additional files
     */
    private HashMap<String,Object> parseConfig(Path rootPath,Map config) {
        if (config == null) return null;
        HashMap<String,Object> result = new HashMap<>();
        for (Object key: config.keySet()) {
            Object configNode = config.get(key);
            if (key.toString().equals("#include")) {
                Path path = Paths.get(configNode.toString());
                if (!path.isAbsolute()) path = Paths.get(rootPath.getParent().toString(),path.toString());
                HashMap<String,Object> includeConfig = readConfigFile(path);
                if (includeConfig != null) result.putAll(includeConfig);
            } else if (configNode instanceof LinkedTreeMap) {
                result.put(key.toString(), parseConfig(rootPath, (LinkedTreeMap)configNode));
            } else if (configNode.toString().startsWith("#include")) {
                Path path = Paths.get(configNode.toString().replace("#include ", ""));
                if (!path.isAbsolute()) path = Paths.get(rootPath.getParent().toString(),path.toString());
                result.put(key.toString(), readConfigFile(path));
            } else {
                result.put(key.toString(), configNode);
            }
        }
        return result;
    }

    // Methods returns configuration for different type of object specified by it's name
    public HashMap<String,Object> getDatabaseAdapter(String name) { return getConfigNode("adapters",name);}
    public HashMap<String,Object> getDatabasePersister(String name) { return getConfigNode("persisters",name);}
    public HashMap<String,Object> getDataAggregator(String name) { return getConfigNode("aggregators",name);}
    public HashMap<String,Object> getDataLogger(String name) { return getConfigNode("loggers",name);}
    public HashMap<String,Object> getDataArchiver(String name) { return getConfigNode("archivers",name);}

    /**
     * Method used to return top level collection of configuration objects from root config
     * @param collectionName : Name of collection to return (adapters, persisters, loggers etc.)
     * @return Collection of objects
     */
    public HashMap<String,HashMap<String,Object>> getConfigCollection(String collectionName) {
        if (config==null || !config.containsKey(collectionName) || !(config.get(collectionName) instanceof HashMap)) return null;
        return (HashMap<String,HashMap<String,Object>>)config.get(collectionName);
    }

    /**
     * Method used to return configuration object with specified name from specified collection
     * @param collectionName Name of collection (adapters, persisters, loggers etc.)
     * @param nodeName Name of object
     * @return Configuration object as HashMap
     */
    public HashMap<String,Object> getConfigNode(String collectionName,String nodeName) {
        HashMap<String,HashMap<String,Object>> collection = getConfigCollection(collectionName);
        if (collection == null || !collection.containsKey(nodeName)) return null;
        return collection.get(nodeName);
    }

    @Override
    public String getName() {
        return "ConfigManager";
    }

    @Override
    public String getSyslogPath() {
        return LoggerApplication.getInstance().getLogPath()+"/"+this.getName();
    }

    @Override
    public HashMap<String, Object> getSyslogConfig() {
        return LoggerApplication.getInstance().getSyslogConfig();
    }

    public HashMap<String,Object> getConfig() { return config;}
}