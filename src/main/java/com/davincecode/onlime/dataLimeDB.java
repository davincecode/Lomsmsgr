package com.davincecode.onlime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class dataLimeDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/onLimeUsers";
        String username = "root";
        String password = "password";
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement();

        // Create users table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT, " +
                "username VARCHAR(255), " +
                "status VARCHAR(255), " +
                "PRIMARY KEY(id)" +
                ")";
        statement.execute(createUsersTable);

        // Create messages table
        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INT AUTO_INCREMENT, " +
                "sender_id INT, " +
                "receiver_id INT, " +
                "message TEXT, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY(id), " +
                "FOREIGN KEY (sender_id) REFERENCES users(id), " +
                "FOREIGN KEY (receiver_id) REFERENCES users(id)" +
                ")";
        statement.execute(createMessagesTable);

        // Create groups table
        String createGroupsTable = "CREATE TABLE IF NOT EXISTS groups (" +
                "id INT AUTO_INCREMENT, " +
                "name VARCHAR(255), " +
                "PRIMARY KEY(id)" +
                ")";
        statement.execute(createGroupsTable);

        // Create user_group table
        String createUserGroupTable = "CREATE TABLE IF NOT EXISTS user_group (" +
                "user_id INT, " +
                "group_id INT, " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (group_id) REFERENCES groups(id)" +
                ")";
        statement.execute(createUserGroupTable);

        statement.close();
        connection.close();
    }
}
