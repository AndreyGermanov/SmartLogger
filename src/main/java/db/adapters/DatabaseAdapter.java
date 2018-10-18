package db.adapters;

import config.ConfigManager;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Base class for database adapters
 */
abstract public class DatabaseAdapter implements IDatabaseAdapter, ISyslog.Loggable {

    // Unique name of database adapter
    protected String name = "";
    // Set of collections (tables) with which this data adapter allows to work
    protected HashMap<String,Object> collections = new HashMap<>();
    // Link to System logger to write error and warning messages
    protected ISyslog syslog;

    /**
     * Factory method which returns concrete data adapter by unique name, using configuration file
     * @param name Name of adapter, defined in configuration file
     * @return Configured object
     */
    public static IDatabaseAdapter get(String name) {
        HashMap<String,Object> config = ConfigManager.getInstance().getDatabaseAdapter(name);
        if (config==null || !config.containsKey("type")) return null;
        IDatabaseAdapter result = null;
        switch (config.get("type").toString()) {
            case "mysql":
                result = new MysqlDatabaseAdapter();
                break;
            case "sqlite":
                result = new SqliteDatabaseAdapter();
                break;
            case "orientdb":
                result = new OrientDBDatabaseAdapter();
                break;
            case "mongodb":
                result = new MongoDatabaseAdapter();
        }
        if (result != null) result.configure(config);
        return result;
    }

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        this.name = config.getOrDefault("name","").toString();
        this.collections = (HashMap<String,Object>)config.getOrDefault("collections",new HashMap<>());
        this.syslog = new Syslog(this);
    }

    /**
     * Method used to insert set of records to specified collection in database
     * @param collectionName Name of collection
     * @param data List of records
     * @return Number of inserted records
     */
    public Integer insert(String collectionName,ArrayList<HashMap<String,Object>> data) {
        return processUpdateQuery(collectionName,data,true);
    }

    /**
     * Method used to update specified set of records in specified collection of database
     * @param collectionName Name of collection
     * @param data List of records
     * @return Number of updated records
     */
    public Integer update(String collectionName,ArrayList<HashMap<String,Object>> data) {
        return processUpdateQuery(collectionName,data,false);
    }

    /**
     * Base method, used to insert or update data in database
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    abstract Integer processUpdateQuery(String collectionName, ArrayList<HashMap<String,Object>> data, boolean isNew);

    /**
     * Utility Method checks if provided collection field has correct configuration
     * @param collectionName Name of collection
     * @param fieldName Field name
     * @return True if this field configured correctly or false otherwise
     */
    boolean isValidFieldConfig(String collectionName,String fieldName) {
        if (getFieldConfigValue(collectionName,fieldName,"name")==null) return false;
        if (getFieldConfigValue(collectionName,fieldName,"type")==null) return false;
        return true;
    }

    /**
     * Utility method which returns value of specified config parameter of specified field
     * @param collectionName Name of collection
     * @param fieldName Field name
     * @param variable Configuration key
     * @return Configuration value
     */
    Object getFieldConfigValue(String collectionName,String fieldName,String variable) {
        HashMap<String,Object> field = getFieldConfig(collectionName,fieldName);
        if (field == null) return null;
        return field.containsKey(variable) ? field.get(variable) : null;
    }

    /**
     * Returns configuration object with all configuration parameters for specified field from config file
     * @param collectionName Name of collection
     * @param fieldName Name of field
     * @return Configuration object as HashMap
     */
    HashMap<String,Object> getFieldConfig(String collectionName,String fieldName) {
        HashMap<String,Object> collection = getCollectionConfig(collectionName);
        if (collection == null || !collection.containsKey("fields") ||
                !(collection.get("fields") instanceof HashMap<?,?>)) return null;
        HashMap<String,Object> fields = (HashMap<String,Object>)collection.get("fields");
        if (!fields.containsKey(fieldName) || !(collection.get("fields") instanceof HashMap<?,?>)) return null;
        return (HashMap<String,Object>)fields.get(fieldName);
    }

    /**
     * Returns configuration object for specified collection, which includes configuration for whole collection
     * and for each field inside it
     * @param collectionName Name of collection
     * @return Configuration object as HashMap
     */
    HashMap<String,Object> getCollectionConfig(String collectionName) {
        if (!this.collections.containsKey(collectionName)) return null;
        return (HashMap<String,Object>)collections.get(collectionName);
    }

    /**
     * Returns name of field, which specified collection uses as ID field (as specified in configuration file)
     * @param collectionName Name of collection
     * @return Name of field
     */
    String getIdFieldName(String collectionName) {
        HashMap<String,Object> collection = getCollectionConfig(collectionName);
        if (collection == null || !collection.containsKey("idField")) return null;
        String indexField = collection.get("idField").toString();
        if (!collection.containsKey("fields")) return null;
        HashMap<String,Object> fields = (HashMap<String,Object>)collection.get("fields");
        if (!fields.containsKey(indexField)) return null;
        return indexField;
    }

    /**
     * Returns unique name of adapter
     * @return
     */
    public String getName() {
        return "Database_adapter-"+this.name;
    }

    /**
     * Returns path which System logger uses to write messages, related to this adapter
     * @return
     */
    public String getSyslogPath() {
        return LoggerApplication.getInstance().getLogPath()+"/db/"+this.getName()+"/";
    }

    /**
     * Method used to manually assing System logger object to this adapter
     * @param syslog
     */
    public void setSyslog(ISyslog syslog) {
        this.syslog = syslog;
    }

    protected String getCollectionType() { return "adapters"; }

}
