package db.adapters;

import java.sql.*;
import java.util.HashMap;

/**
 * Database adapter for MySQL databases
 */
public class MysqlDatabaseAdapter extends JDBCDatabaseAdapter {

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
        String url = "jdbc:mysql://"+host+":"+port+"/"+database+"?serverTimezone=UTC";
        try {
            this.connection = DriverManager.getConnection(url,username,password);
        } catch (SQLException e) {
            syslog.logException(e,this,"connect");
        }
    }
}