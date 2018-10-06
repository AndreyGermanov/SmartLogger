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

public class MongoDatabaseAdapter extends DatabaseAdapter {

    MongoClient connection;
    String host = "localhost";
    Integer port = 27017;
    String database = "";
    MongoDatabase db;

    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        this.host = config.getOrDefault("host",this.host).toString();
        this.port = Integer.valueOf(config.getOrDefault("port",this.port).toString());
        this.database = config.getOrDefault("database",this.database).toString();
    }

    void connect() {
        try {
            this.connection = MongoClients.create("mongodb://" + this.host + ":" + this.port);
            this.db = this.connection.getDatabase(this.database);
        } catch (Exception e) {
            this.connection = null;
            syslog.logException(e,this,"connect");
        }
    }

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

    Integer executeUpdateQuery(String collectionName, List<WriteModel<Document>> updateStatement) {
        try {
            BulkWriteResult result = db.getCollection(collectionName).bulkWrite(updateStatement);
            return result.getModifiedCount() + result.getInsertedCount();
        } catch (Exception e) {
            syslog.logException(e,this,"executeUpdateQuery");
            return null;
        }
    }

    List<WriteModel<Document>> prepareUpdateStatement(String collectionName, ArrayList<HashMap<String,Object>> data) {
        return data.stream()
                .map(row -> prepareInsertDocumentStatement(collectionName,row))
                .collect(Collectors.toList());
    }

    WriteModel<Document> prepareInsertDocumentStatement(String collectionName,HashMap<String,Object> row) {
        Document result = new Document();
        row.keySet().forEach(fieldName -> {
            Object value = formatFieldValue(collectionName,fieldName,row.get(fieldName));
            if (value != null) result.append(fieldName,value);
        });
        return new InsertOneModel<>(result);
    }

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
