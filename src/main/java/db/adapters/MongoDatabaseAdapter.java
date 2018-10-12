package db.adapters;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import main.ISyslog;
import org.bson.Document;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Database adapter for MongoDB database
 */
public class MongoDatabaseAdapter extends DatabaseAdapter {

    // Link to database connection
    private MongoClient connection;
    // Database Host name
    private String host = "localhost";
    // Database port
    private Integer port = 27017;
    // Name of database
    private String database = "";
    // Database object to communicate with
    private MongoDatabase db;

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        this.host = config.getOrDefault("host",this.host).toString();
        this.port = Integer.valueOf(config.getOrDefault("port",this.port).toString());
        this.database = config.getOrDefault("database",this.database).toString();
    }

    /**
     * Method used to open database connection, based on currect configuration
     */
    void connect() {
        try {
            this.connection = MongoClients.create("mongodb://" + this.host + ":" + this.port);
            this.db = this.connection.getDatabase(this.database);
        } catch (Exception e) {
            this.connection = null;
            syslog.logException(e,this,"connect");
        }
    }

    /**
     * Base method for UPDATE and INSERT database queries
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    @Override
    Integer processUpdateQuery(String collectionName, ArrayList<HashMap<String, Object>> data, boolean isNew) {
        if (connection == null) this.connect();
        if (connection == null) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not connect to database",
                    this.getClass().getName(),"processUpdateQuery");
            return null;
        }
        return executeUpdateQuery(collectionName,prepareUpdateStatement(collectionName,data));
    }

    /**
     * Method which executes specified update query for specified collection in database
     * @param collectionName Name of collection
     * @param updateStatement Prepared query statement to execute
     * @return Number of affected records
     */
    Integer executeUpdateQuery(String collectionName, List<WriteModel<Document>> updateStatement) {
        try {
            BulkWriteResult result = db.getCollection(collectionName).bulkWrite(updateStatement);
            return result.getModifiedCount() + result.getInsertedCount();
        } catch (Exception e) {
            syslog.logException(e,this,"executeUpdateQuery");
            return null;
        }
    }

    /**
     * Method used to prepare list of database queries to update specified array of rows in database
     * @param collectionName Name of collection to update
     * @param data Array of rows to update
     * @return Array of prepared query statement
     */
    List<WriteModel<Document>> prepareUpdateStatement(String collectionName, ArrayList<HashMap<String,Object>> data) {
        return data.stream()
                .map(row -> prepareInsertDocumentStatement(collectionName,row))
                .collect(Collectors.toList());
    }

    /**
     * Utility method used to prepare database update query statement for specified data row
     * @param collectionName Name of collection to update
     * @param row Data row
     * @return Prepared query statement
     */
    WriteModel<Document> prepareInsertDocumentStatement(String collectionName,HashMap<String,Object> row) {
        Document result = new Document();
        row.keySet().forEach(fieldName -> {
            Object value = formatFieldValue(collectionName,fieldName,row.get(fieldName));
            if (value != null) result.append(fieldName,value);
        });
        return new InsertOneModel<>(result);
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
                    return Integer.valueOf(value.toString());
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
}
