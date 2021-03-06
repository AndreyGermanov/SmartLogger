package db.adapters;

import config.ConfigManager;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    protected HashMap<String,Object> syslogConfig;

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
        try {
            this.syslogConfig = (HashMap<String,Object>)config.getOrDefault("syslog",
                    LoggerApplication.getInstance().getSyslogConfig());
        } catch (Exception e) {e.printStackTrace();}
        this.syslog = new Syslog(this);
    }

    /**
     * Public method used by consumers to select data from data source
     * @param sql SQL query text
     * @param collectionName Collection name to which SQL applied, or null, if more than one collection
     *                       specified in SQL query
     * @return Result as array of rows
     */
    public ArrayList<HashMap<String,Object>> select(String sql,String collectionName) {
        return processQueryResult(executeSelectQuery(sql),collectionName);
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
     * Databases specific method to send SELECT query to server and return RAW result
     * @param sql SQL query text
     * @return RAW result from server
     */
    Object executeSelectQuery(String sql) { return null;}

    /**
     * Method used to transform RAW result of SELECT query returned by server to array of rows
     * @param result RAW result from database server
     * @param collectionName Name of collection queried, or null, if it was multi-table query
     * @return Result as array of rows
     */
    ArrayList<HashMap<String,Object>> processQueryResult(Object result,String collectionName) {
        ArrayList<Map<String,Object>> rawRows = parseQueryResult(result);
        if (rawRows == null || rawRows.size()==0) return null;
        ArrayList<HashMap<String,Object>> resultRows = new ArrayList<>();
        for (Map<String,Object> rawRow: rawRows) {
            HashMap<String,Object> resultRow = processQueryResultRow(rawRow,collectionName);
            if (resultRow.size()>0) resultRows.add(resultRow);
        }
        return resultRows;
    }

    /**
     * Method used to transform RAW query result to array of rows (without transofrming field values)
     * @param result Query result to transform
     * @return
     */
    ArrayList<Map<String,Object>> parseQueryResult(Object result) { return null;}

    /**
     * Method used to transform RAW row returned from database server to HashMap
     * @param rawRow Raw row from server
     * @param collectionName Collection to which this row belongs or nothing, if its unknown
     * @return Row with fields, transformed to appropriate format
     */
    HashMap<String,Object> processQueryResultRow(Map<String,Object> rawRow, String collectionName) {
        HashMap<String,Object> resultRow = new HashMap<>();
        for (String key: rawRow.keySet()) {
            Object value = rawRow.get(key);
            if (collectionName != null)
                value = formatFieldValue(collectionName,key,value);
            if (value != null) resultRow.put(key,value);
        }
        return resultRow;
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
        HashMap<String,Object> fields = getCollectionFieldsConfig(collectionName);
        if (fields == null || !(fields instanceof  HashMap<?,?>)|| !fields.containsKey(fieldName)) return null;
        return (HashMap<String,Object>)fields.get(fieldName);
    }

    /**
     * Method returns path of Collection configuration, related to fields of collection
     * @param collectionName Name of collection
     * @return HashMap with config related to fields
     */
    HashMap<String,Object> getCollectionFieldsConfig(String collectionName) {
        HashMap<String,Object> collection = getCollectionConfig(collectionName);
        if (collection == null || !collection.containsKey("fields") ||
                !(collection.get("fields") instanceof HashMap<?,?>)) return null;
        return (HashMap<String,Object>)collection.get("fields");
    }

    /**
     * Method returns list of field names in collection
     * @param collectionName Name of collection
     * @return Set of feild names
     */
    Set<String> getCollectionFields(String collectionName) {
        HashMap<String,Object> fields = getCollectionFieldsConfig(collectionName);
        Set<String> result = fields.keySet();
      //  result.remove(getIdFieldName(collectionName));
        return result;
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
     * Formats value for specified field for UPDATE or INSERT query, depending on type of this field, defined
     * in configuration file
     * @param collectionName Name of collection
     * @param fieldName Name of field
     * @param value Value of field to format
     * @return Properly formatted and escaped value to insert to SQL query line
     */
    Object formatFieldValue(String collectionName,String fieldName,Object value) {
        if (!isValidFieldConfig(collectionName,fieldName)) return null;
        if (value == null) return null;
        String type = getFieldConfigValue(collectionName,fieldName,"type").toString();
        try {
            switch (type) {
                case "decimal":
                    return Double.valueOf(value.toString());
                case "integer":
                    return Double.valueOf(value.toString()).intValue();
                case "string":
                    return value.toString();
            }
        } catch (Exception e) {
            syslog.log(ISyslog.LogLevel.WARNING,
                    "Could not format field value '"+value+"' of field '"+fieldName+"'"+
                            "in collection '"+collectionName+"'",
                    this.getClass().getName(),"formatFieldValue");
        }
        return null;
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

    public String getCollectionType() { return "adapters"; }

    @Override
    public HashMap<String, Object> getSyslogConfig() {
        return syslogConfig;
    }

}
