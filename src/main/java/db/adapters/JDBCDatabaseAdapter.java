package db.adapters;

import main.ISyslog;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base class for all Database adapters, which is based on JDBC interface
 */
abstract public class JDBCDatabaseAdapter extends DatabaseAdapter {

    // Link to database connection
    protected Connection connection;

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    abstract void connect();

    /**
     * Base method, used to insert or update data in database
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    public Integer processUpdateQuery(String collectionName, ArrayList<HashMap<String,Object>> data, boolean isNew) {
        if (connection == null) this.connect();
        if (connection == null) return null;
        String updateStatement = prepareUpdateBatchSQL(collectionName,data,isNew);
        return updateStatement.isEmpty() ? null : executeUpdateQuery(updateStatement);
    }

    /**
     * Method used to execute specified update query in database and return number of affected rows
     * @param updateStatement Query statement to execute
     * @return Number of affected rows
     */
    Integer executeUpdateQuery(Object updateStatement) {
        String updateString = updateStatement.toString();
        Statement statement;
        try {
            statement = connection.createStatement();
            Arrays.stream(updateString.split(";")).forEach(sql -> {
                try {
                    syslog.log(ISyslog.LogLevel.DEBUG,"Adding SQL to batch: '"+sql+"'",this.getClass().getName(),"executeUpdateQuery");
                    statement.addBatch(sql);
                } catch (Exception e) {
                    syslog.logException(e,this,"executeUpdateQuery");
                }
            });
            syslog.log(ISyslog.LogLevel.DEBUG,"Executing SQL batch",this.getClass().getName(),"executeUpdateQuery");
            return Arrays.stream(statement.executeBatch()).reduce((i,i1) -> i+i1).orElse(0);
        } catch (Exception e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not execute batch query '"+updateString+"'",
                    this.getClass().getName(),"executeUpdateQuery");
            return null;
        }
    }

    /**
     * Method used to prepare set of SQL queries to UPDATE or INSERT multiple records to database
     * @param collectionName Name of collection to update
     * @param data Array or data rows to UPDATE or INSERT
     * @param isNew if true, then method will return INSERT queries, otherwise will return UPDATE queries
     * @return Set of INSERT or UPDATE query lines, delimited by ';' symbol
     */
    String prepareUpdateBatchSQL(String collectionName,ArrayList<HashMap<String,Object>> data,boolean isNew) {
        return joinSqlLines(collectionName,data,isNew,";");
    }

    /**
     * Method gets list of data rows to insert or update and returns concatentated
     * string of appropriate UPDATE or INSERT statements
     * @param collectionName Name of colleciton
     * @param data Data array
     * @param isNew Is it new rows (INSERT) or not (UPDATE)
     * @param delimiter - Delimiter of SQL statements (usually ';')
     * @return String with SQL statements delimited by delimiter
     */
    String joinSqlLines(String collectionName,ArrayList<HashMap<String,Object>> data,boolean isNew,String delimiter) {
        return data.stream()
                .filter((row) -> row.size()>0)
                .map(row -> prepareUpdateSQL(collectionName,row,isNew))
                .filter(string -> string != null && !string.isEmpty())
                .reduce((s,s1) -> s+delimiter+s1).orElse("");
    }

    /**
     * Returns INSERT or UPDATE query statement for provided data row
     * @param collectionName Name of collection to update
     * @param row Row which is a set of fields
     * @param isNew if true, then method will return INSERT query, otherwise will return UPDATE query
     * @return
     */
    String prepareUpdateSQL(String collectionName,HashMap<String,Object> row, boolean isNew) {
        HashMap<String,String> fields = prepareDataForSql(collectionName,row);
        if (isNew) {
            String keys = fields.keySet().stream().reduce((s, s1) -> s+","+s1).orElse("");
            String values = fields.values().stream().reduce((s, s1) -> s+","+s1).orElse("");
            if (keys.isEmpty()) return "";
            return "INSERT INTO "+collectionName+" ("+keys+") VALUES("+values+")";
        }
        String fieldString = fields.keySet().stream().reduce((s,s1) -> s+"="+fields.get(s1)).orElse("");
        String idValue = formatFieldValueForSQL(collectionName,getIdFieldName(collectionName),
                row.get(getIdFieldName(collectionName)));
        if (fieldString.isEmpty() || idValue == null) return "";
        return "UPDATE "+collectionName+" SET "+fieldString+" WHERE id="+idValue;
    }

    /**
     * Method returns row of fields, formatted according to configuration and ready to be used in
     * SQL statements
     * @param collectionName Name of collection
     * @param row Row of data
     * @return Row of data with field values, formatted according to their types
     */
    HashMap<String,String> prepareDataForSql(String collectionName,HashMap<String,Object> row) {
        return row.keySet().stream()
                .filter((fieldName) -> formatFieldValueForSQL(collectionName,fieldName,row.get(fieldName)) != null )
                .collect(Collectors.toMap(fieldName -> fieldName,fieldName ->
                        formatFieldValueForSQL(collectionName,fieldName,row.get(fieldName)),(s1,s2) -> s1,HashMap::new));
    }

    /**
     * Formats value for specified field for UPDATE or INSERT query, depending on type of this field, defined
     * in configuration file
     * @param collectionName Name of collection
     * @param fieldName Name of field
     * @param value Value of field to format
     * @return Properly formatted and escaped value to insert to SQL query line
     */
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
}