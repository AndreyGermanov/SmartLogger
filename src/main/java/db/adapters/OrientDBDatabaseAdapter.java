package db.adapters;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mashape.unirest.http.Unirest;
import utils.DataList;
import utils.DataMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Database adapter for OrientDB databases
 */
public class OrientDBDatabaseAdapter extends JDBCDatabaseAdapter {

    // Connection credentials

    private String host = "";
    private String port = "";
    private String username = "";
    private String password = "";
    private String database = "";
    private WorkMode mode = WorkMode.jdbc;
    private Gson gson = new Gson();

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    @Override
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        if (config == null) return;
        this.host = config.getOrDefault("host","").toString();
        this.port = Integer.toString(Double.valueOf(config.getOrDefault("port","").toString()).intValue());
        this.username = config.getOrDefault("username","").toString();
        this.password = config.getOrDefault("password","").toString();
        this.database = config.getOrDefault("database","").toString();
        try {
            this.mode = WorkMode.valueOf(config.getOrDefault("mode", "jdbc").toString());
        } catch (Exception e) { e.printStackTrace();}
    }

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    @Override
    void connect() {
        if (this.mode != WorkMode.jdbc) return;
        String url = "jdbc:orient:remote:"+host+":"+port+"/"+database;
        Properties info = new Properties();
        info.put("user",username);
        info.put("password",password);
        try {
            this.connection = DriverManager.getConnection(url,info);
        } catch (SQLException e) {
            syslog.logException(e,this,"connect");
        }
    }

    /**
     * Base method, used to insert or update data in database
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    @Override
    public Integer processUpdateQuery(String collectionName, ArrayList<HashMap<String,Object>> data, boolean isNew) {
        if (mode == WorkMode.jdbc) return super.processUpdateQuery(collectionName,data,isNew);
        String updateStatement = prepareUpdateBatchSQL(collectionName,data,isNew);
        return updateStatement.isEmpty() ? null : executeUpdateQuery(updateStatement);
    }

    /**
     * Method used to execute specified update query in database and return number of affected rows
     * @param updateStatement Query statement to execute
     * @return Number of affected rows
     */
    @Override
    Integer executeUpdateQuery(Object updateStatement) {
        if (mode == WorkMode.jdbc) return executeUpdateQuery(updateStatement);
        HashMap<String,Object> options = DataMap.create("batch",true);
        String result = execOrientDBRequest(updateStatement.toString(),options);
        if (result.isEmpty()) return 0;
        HashMap resultJson = gson.fromJson(result,HashMap.class);
        if (!resultJson.containsKey("result")) return 0;
        return ((ArrayList)resultJson.getOrDefault("result",new ArrayList())).size();
    }

    /**
     * Method used to prepare set of SQL queries to UPDATE or INSERT multiple records to database
     * @param collectionName Name of collection to update
     * @param data Array or data rows to UPDATE or INSERT
     * @param isNew if true, then method will return INSERT queries, otherwise will return UPDATE queries
     * @return Set of INSERT or UPDATE query lines, delimited by ';' symbol
     */
    @Override
    String prepareUpdateBatchSQL(String collectionName, ArrayList<HashMap<String,Object>> data, boolean isNew) {
        if (mode == WorkMode.jdbc) return super.prepareUpdateBatchSQL(collectionName,data,isNew);
        if (data.size() == 0) return "";
        String keys = getCollectionFields(collectionName).stream()
                .filter(key->!key.equals(getIdFieldName(collectionName)))
                .reduce((s, s1) -> s+","+s1).orElse("");
        return (isNew ? "INSERT INTO "+collectionName+" ("+keys+") VALUES (" : "") +
                joinSqlLines(collectionName,data,isNew,isNew ? "),(" : ";") + (isNew ? ")" : "");
    }

    /**
     * Returns INSERT or UPDATE query statement for provided data row
     * @param collectionName Name of collection to update
     * @param row Row which is a set of fields
     * @param isNew if true, then method will return INSERT query, otherwise will return UPDATE query
     * @return
     */
    @Override
    String prepareUpdateSQL(String collectionName,HashMap<String,Object> row, boolean isNew) {
        if (mode == WorkMode.jdbc) super.prepareUpdateSQL(collectionName,row,isNew);
        HashMap<String,String> fields = prepareDataForSql(collectionName,row);
        if (fields.isEmpty()) return null;
        Set<String> keys = getCollectionFields(collectionName);
        if (isNew)
            return keys.stream()
                    .filter(key-> !key.equals(getIdFieldName(collectionName)))
                    .map(key -> fields.getOrDefault(key, "null"))
                    .reduce((s, s1) -> s+","+s1).orElse("");
        String fieldString = fields.keySet().stream().reduce((s,s1) -> s+"="+fields.get(s1)).orElse("");
        String idValue = formatFieldValueForSQL(collectionName,getIdFieldName(collectionName),
                row.get(getIdFieldName(collectionName)));
        if (fieldString.isEmpty() || idValue == null) return "";
        return "UPDATE "+collectionName+" SET "+fieldString+" WHERE id="+idValue;
    }

    /**
     * Method used to execute REST request to OrientDB database server
     * @param sql SQL query to send to server
     * @param options Various request options. (batch mode or single insert mode and others)
     * @return Response from server after request as a string
     */
    String execOrientDBRequest(String sql,HashMap<String,Object> options) {
        try {
            OrientDBRequest request = preparesOrientDBRequest(sql,options);
            return new BufferedReader(
                new InputStreamReader(Unirest.post(request.url).basicAuth(username, password)
                        .header("Accept-Encoding","gzip,deflate")
                        .body(request.body).asString().getRawBody())).lines().reduce((s,s1) -> s+"\n"+s1).orElse("");
        } catch (Exception e) {
            e.printStackTrace();
            syslog.logException(e,this,"execOrientDBRequest");
            return "";
        }
    }

    /**
     * Prepares REST Request to OrientDB server according to input options
     * @param sql SQL text of query
     * @param options Various request options. (batch mode or single insert mode and others)
     * @return Object with 2 fields: "url" - Request URL and "body" - POST body of request
     */
    OrientDBRequest preparesOrientDBRequest(String sql, HashMap<String,Object> options) {
        OrientDBRequest result = new OrientDBRequest(host+":"+port,sql);
        String command = "/command/" + database + "/sql";
        if (options == null || options.size()==0) {
            result.url += command;
            return result;
        }
        HashMap<String,Object> request;
        if (Boolean.parseBoolean(options.getOrDefault("batch",true).toString())) {
            request = DataMap.create("transaction",false,"operations",
                DataList.create(DataMap.create("type", "script", "language", "sql", "script", sql))
            );
            command = "/batch/"+database;
            result.body = gson.toJson(request);
        }
        result.url += command;
        return result;
    }

    /**
     * Databases specific method to send SELECT query to server and return RAW result
     * @param sql SQL query text
     * @return RAW result from server
     */
    @Override
    Object executeSelectQuery(String sql) {
        if (mode == WorkMode.jdbc) return super.executeSelectQuery(sql);
        try {
            Object result = execOrientDBRequest(sql,null);
            if (result.toString().isEmpty()) return null;
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method used to transform RAW query result to array of rows (without transofrming field values)
     * @param result Query result to transform
     * @return
     */
    @Override
    ArrayList<Map<String,Object>> parseQueryResult(Object result) {
        if (result==null) return null;
        HashMap resultJson = gson.fromJson(result.toString(),HashMap.class);
        if (resultJson == null || !resultJson.containsKey("result")) return null;
        ArrayList<Map<String, Object>> rawRows = (ArrayList<Map<String, Object>>) resultJson.get("result");
        return rawRows;
    }

    // Possible OrientDB server communication modes: JDBC or REST
    public enum WorkMode {jdbc,rest}

    /**
     * Object represents Request to OrientDB server in REST mode
     */
    private class OrientDBRequest {
        // Request URL
        String url;
        // POST body
        String body;
        // Class contructor
        OrientDBRequest(String url,String body) {
            this.url = url;
            this.body=body;
        }
        // Returns object as a String
        public String toString() {return "URL: "+url+",BODY="+body;}
    }
}