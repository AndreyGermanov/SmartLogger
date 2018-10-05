package db.adapters;

import main.ISyslog;
import main.LoggerApplication;
import main.Syslog;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

public class MysqlDatabaseAdapter extends JDBCDatabaseAdapter {

    private String host = "";
    private String port = "";
    private String username = "";
    private String password = "";
    private String database = "";

    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        if (config == null) return;
        this.name = config.getOrDefault("name","").toString();
        this.host = config.getOrDefault("host","").toString();
        this.port = config.getOrDefault("port","").toString();
        this.username = config.getOrDefault("username","").toString();
        this.password = config.getOrDefault("password","").toString();
        this.database = config.getOrDefault("database","").toString();
    }

    void connect() {
        String url = "jdbc:mysql://"+host+":"+port+"/"+database+"?serverTimezone=UTC";
        try {
            this.connection = DriverManager.getConnection(url,username,password);
        } catch (SQLException e) {
            syslog.logException(e,this,"connect");
        }
    }
}