package db.adapters;

import config.ConfigManager;
import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;

abstract public class DatabaseAdapter implements IDatabaseAdapter, ISyslog.Loggable {

    protected String name = "";
    protected HashMap<String,Object> collections = new HashMap<>();
    protected ISyslog syslog;

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
        }
        if (result != null) result.configure(config);
        return result;
    }

    public void configure(HashMap<String,Object> config) {
        this.collections = (HashMap<String,Object>)config.getOrDefault("collections",new HashMap<>());
        this.syslog = new Syslog(this);
    }

    public Integer insert(String collectionName,ArrayList<HashMap<String,Object>> data) {
        return processUpdateQuery(collectionName,data,true);
    }

    public Integer update(String collectionName,ArrayList<HashMap<String,Object>> data) {
        return processUpdateQuery(collectionName,data,false);
    }

    abstract Integer processUpdateQuery(String collectionName, ArrayList<HashMap<String,Object>> data, boolean isNew);

    String formatFieldValueForSQL(String collectionName,String fieldName,Object value) {
        if (!isValidFieldConfig(collectionName,fieldName)) return null;
        if (value == null) return null;
        String type = getFieldConfigValue(collectionName,fieldName,"type").toString();
        switch (type) {
            case "decimal":
                return value.toString();
            case "integer":
                return value.toString();
            case "string":
                return "'"+value.toString()+"'";
        }
        return null;
    }

    boolean isValidFieldConfig(String collectionName,String fieldName) {
        if (getFieldConfigValue(collectionName,fieldName,"name")==null) return false;
        if (getFieldConfigValue(collectionName,fieldName,"type")==null) return false;
        return true;
    }

    Object getFieldConfigValue(String collectionName,String fieldName,String variable) {
        HashMap<String,Object> field = getFieldConfig(collectionName,fieldName);
        if (field == null) return null;
        return field.containsKey(variable) ? field.get(variable) : null;
    }

    HashMap<String,Object> getFieldConfig(String collectionName,String fieldName) {
        HashMap<String,Object> collection = getCollectionConfig(collectionName);
        if (collection == null || !collection.containsKey("fields") ||
                !(collection.get("fields") instanceof HashMap<?,?>)) return null;
        HashMap<String,Object> fields = (HashMap<String,Object>)collection.get("fields");
        if (!fields.containsKey(fieldName) || !(collection.get("fields") instanceof HashMap<?,?>)) return null;
        return (HashMap<String,Object>)fields.get(fieldName);
    }

    HashMap<String,Object> getCollectionConfig(String collectionName) {
        if (!this.collections.containsKey(collectionName)) return null;
        return (HashMap<String,Object>)collections.get(collectionName);
    }

    String getIdFieldName(String collectionName) {
        HashMap<String,Object> collection = getCollectionConfig(collectionName);
        if (collection == null || !collection.containsKey("idField")) return null;
        String indexField = collection.get("idField").toString();
        if (!collection.containsKey("fields")) return null;
        HashMap<String,Object> fields = (HashMap<String,Object>)collection.get("fields");
        if (!fields.containsKey(indexField)) return null;
        return indexField;
    }

    public String getName() {
        return "Database_adapter-"+this.name;
    }

    public String getSyslogPath() {
        return LoggerApplication.getInstance().getCachePath()+"/logs/db/"+this.getName()+"/";
    }

    public void setSyslog(ISyslog syslog) {
        this.syslog = syslog;
    }
}
