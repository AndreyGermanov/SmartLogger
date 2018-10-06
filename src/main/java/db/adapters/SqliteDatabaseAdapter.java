package db.adapters;

import main.Syslog;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public class SqliteDatabaseAdapter extends JDBCDatabaseAdapter {

    private String path = "";

    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        if (config == null) return;
        this.path = config.getOrDefault("path","").toString();
    }

    void connect() {
        String url = "jdbc:sqlite://"+path;
        try {
            this.connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            syslog.logException(e,this,"connect");
        }
    }
}
