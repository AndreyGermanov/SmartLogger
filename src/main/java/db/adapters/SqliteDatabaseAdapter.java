package db.adapters;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Database adapter for SQLite databases
 */
public class SqliteDatabaseAdapter extends JDBCDatabaseAdapter {

    // Path to database file
    private String path = "";

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        if (config == null) return;
        this.path = config.getOrDefault("path","").toString();
    }

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    void connect() {
        String url = "jdbc:sqlite://"+path;
        try {
            this.connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            syslog.logException(e,this,"connect");
        }
    }
}
