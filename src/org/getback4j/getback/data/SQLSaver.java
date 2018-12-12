package org.getback4j.getback.data;

import org.getback4j.getback.main.GetBack;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Bryce
 */
public final class SQLSaver {

    /**
     * Contains the active database connection
     *
     */
    private Connection connection;

    /**
     * Contains the credentials and connection information required to interact
     * with a MySQL Database.
     *
     */
    private final String host, database, username, password;
    private final int port;

    /**
     * Instantiates variables required for the SQLSaver to function. All of
     * these should be defined in each plugin's config.yml.
     *
     * @param host The IP address of the SQL server
     * @param database The database name to target
     * @param username The username to connect to the database
     * @param password The password associated with the username
     * @param port The port associated with the database
     */
    public SQLSaver(String host, String database, String username, String password, int port) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;
        runDatabase();
    }

    /**
     * Opens a connection to the database using the credentials provided at
     * Construction
     *
     * @throws SQLException on connection error/invalid credentials
     * @throws ClassNotFoundException when your java is not version 8.
     */
    public void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.cj.jdbc.Driver");
            // TODO
            // Notice how useSSL is disabled. We should fix this later on as i'm sure users would love to use their secure bois
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false", this.username, this.password);
        }
    }

    /**
     * Runs the openConnection method, handles the throwables to be logged into
     * console.
     *
     */
    public void runDatabase() {
        try {
            openConnection();
        } catch (ClassNotFoundException | SQLException e) {
            GetBack.getInstance().getLogger().log("Error connecting to MYSQL. Stack: " + e.getMessage());
            // TODO:
            // Remove the system termination on SQL fail. Instead we should disable the plugin
            // and pull the plug to its socket!
            //System.exit(0);
        }
    }

    /**
     * Gets the active connection assuming runDatabase() succeeded.
     *
     * @return Connection the active database connection.F
     */
    public Connection getConnection() {
        return connection;
    }

}
