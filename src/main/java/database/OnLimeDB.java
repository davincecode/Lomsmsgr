package database;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;

public class OnLimeDB {
    private Connection connection;

    public OnLimeDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Dotenv dotenv = Dotenv.load();
            String DB_URL = dotenv.get("DB_URL");
            String DB_USER = dotenv.get("DB_USER");
            String DB_PASSWORD = dotenv.get("DB_PASSWORD");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getUsername(String username) {
        String query = "SELECT username FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createAccount(String username, String encryptedPassword) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, encryptedPassword);
            int result = preparedStatement.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void storeMessageInDB(String senderUsername, String receiverUsername, String message, Timestamp timestamp) {
        String query = "INSERT INTO messages (sender_id, receiver_id, message_text, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int senderId = getUserId(senderUsername);
            int receiverId = getUserId(receiverUsername);
            if (senderId == -1 || receiverId == -1) {
                System.out.println("Sender or receiver not found in the database");
                return;
            }
            preparedStatement.setInt(1, senderId);
            preparedStatement.setInt(2, receiverId);
            preparedStatement.setString(3, message);
            preparedStatement.setTimestamp(4, timestamp);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getUserId(String username) {
        String query = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int userId = resultSet.getInt("user_id");
                    System.out.println("User ID for " + username + ": " + userId); // Debug print
                    return userId;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("User not found: " + username); // Debug print
        return -1; // return -1 if user not found
    }

    public String getReceiverUsername(String senderUsername) {
        String query = "SELECT receiver_id FROM messages WHERE sender_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, getUserId(senderUsername));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int receiverId = resultSet.getInt("receiver_id");
                    return getUsernameById(receiverId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUsernameById(int userId) {
        String query = "SELECT username FROM users WHERE user_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
