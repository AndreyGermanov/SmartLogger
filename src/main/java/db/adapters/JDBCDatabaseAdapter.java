package db.adapters;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

abstract public class JDBCDatabaseAdapter extends DatabaseAdapter {

    protected Connection connection;

    abstract void connect();

    public Integer processUpdateQuery(String collectionName, ArrayList<HashMap<String,Object>> data, boolean isNew) {
        if (connection == null) this.connect();
        if (connection == null) return null;
        String updateStatement = prepareUpdateBatchSQL(collectionName,data,isNew);
        return updateStatement.isEmpty() ? null : executeUpdateQuery(updateStatement);
    }

    Integer executeUpdateQuery(Object updateStatement) {
        String updateString = updateStatement.toString();
        try {
            Statement statement = connection.createStatement();
            Arrays.stream(updateString.split(";")).forEach(sql -> {
                try {
                    statement.addBatch(sql);
                } catch (Exception e) {
                    syslog.logException(e,this,"executeUpdateQuery");
                };
            });
            return Arrays.stream(statement.executeBatch()).reduce((i,i1) -> i+i1).getAsInt();
        } catch (SQLException e) {
            syslog.logException(e,this,"executeUpdateQuery");
            return null;
        }
    }

    String prepareUpdateBatchSQL(String collectionName,ArrayList<HashMap<String,Object>> data,boolean isNew) {
        Optional<String> result = data.stream()
                .filter((row) -> row.size()>0)
                .map(row -> prepareUpdateSQL(collectionName,row,isNew))
                .filter(string -> !string.isEmpty())
                .reduce((s,s1) -> s+";"+s1);
        return result.orElse("");
    }

    String prepareUpdateSQL(String collectionName,HashMap<String,Object> row, boolean isNew) {
        HashMap<String,String> fields = row.keySet().stream()
                .filter((fieldName) -> formatFieldValueForSQL(collectionName,fieldName,row.get(fieldName)) != null )
                .collect(Collectors.toMap(fieldName -> fieldName,fieldName ->
                     formatFieldValueForSQL(collectionName,fieldName,row.get(fieldName)),(s1,s2) -> s1,HashMap::new));
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
}
