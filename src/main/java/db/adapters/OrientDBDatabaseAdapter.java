package db.adapters;

import com.orientechnologies.orient.jdbc.OrientJdbcConnection;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

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

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        if (config == null) return;
        this.host = config.getOrDefault("host","").toString();
        this.port = config.getOrDefault("port","").toString();
        this.username = config.getOrDefault("username","").toString();
        this.password = config.getOrDefault("password","").toString();
        this.database = config.getOrDefault("database","").toString();
    }

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    void connect() {
        String url = "jdbc:orient:remote:"+host+":"+port+"/"+database;
        Properties info = new Properties();
        info.put("user",username);
        info.put("password",password);
        try {
            this.connection = (OrientJdbcConnection)DriverManager.getConnection(url,info);
        } catch (SQLException e) {
            syslog.logException(e,this,"connect");
        }
    }


}
