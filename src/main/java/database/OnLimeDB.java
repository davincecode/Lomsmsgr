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

    // Checks if the username exists in the database
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

    // Creates a new user account in the database
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


    // Store message into the database
    public void storeMessageInDB(String senderUsername, String receiverUsername, String message, Timestamp timestamp, int teamId) {
        String query;
        if (teamIdIsValid(teamId)) {
            if (receiverUsername == null) {
                // Broadcast message
                query = "INSERT INTO messages (sender_id, message_text, timestamp, team_id) VALUES (?, ?, ?, ?)";
            } else {
                // Direct message
                query = "INSERT INTO messages (sender_id, receiver_id, message_text, timestamp, team_id) VALUES (?, ?, ?, ?, ?)";
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                int senderId = getUserId(senderUsername);
                if (senderId == -1) {
                    System.out.println("Sender not found in the database");
                    return;
                }
                preparedStatement.setInt(1, senderId);
                if (receiverUsername != null) {
                    int receiverId = getUserId(receiverUsername);
                    if (receiverId == -1) {
                        System.out.println("Receiver not found in the database");
                        return;
                    }
                    preparedStatement.setInt(2, receiverId);
                    preparedStatement.setString(3, message);
                    preparedStatement.setTimestamp(4, timestamp);
                    preparedStatement.setInt(5, teamId);
                } else {
                    preparedStatement.setString(2, message);
                    preparedStatement.setTimestamp(3, timestamp);
                    preparedStatement.setInt(4, teamId);
                }

                System.out.println("Executing SQL Query: " + preparedStatement.toString());

                preparedStatement.executeUpdate();

                // Retrieve the generated message_id
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int messageId = generatedKeys.getInt(1);
                        System.out.println("Message ID for the inserted message: " + messageId);
                        // Todo: store this messageId for further use.
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid teamId: " + teamId);
        }
    }

    // Check if the teamId exists in the teams table
    private boolean teamIdIsValid(int teamId) {
        String query = "SELECT 1 FROM teams WHERE team_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, teamId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next(); // Returns true if the teamId exists, false otherwise
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Return false in case of an exception
    }



    // Delete message from the database
    public void deleteMessage(int messageId, int userId) {
        String query = "UPDATE messages SET is_deleted_receiver = true WHERE message_id = ? AND receiver_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, messageId);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
