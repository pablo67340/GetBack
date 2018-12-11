/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import getback.GetBack;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Bryce
 */
public final class SQLSaver {

    private Connection connection;

    private final String host, database, username, password;
    private final int port;
    
    public SQLSaver(String host, String database, String username, String password, int port){
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;
        runDatabase();
    }

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
            // Notice how useSSL is disabled. We should fix this later on as im sure users would love to use their secure bois
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database+"?useSSL=false", this.username, this.password);
        }
    }

    
    public void runDatabase() {
        try {
            openConnection();
            Statement statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            GetBack.getInstance().getLogger().log("Error connecting to MYSQL. Stack: "+e.getMessage());
            System.exit(0);
        }
    }
    
    public Connection getConnection(){
        return connection;
    }

}
