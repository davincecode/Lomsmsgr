package database;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OnLimeDB {
    private Connection connection;

    /**
     * Constructs an OnLimeDB instance and establishes a connection to the database.
     *
     * @throws ClassNotFoundException If the MySQL JDBC driver is not found
     * @throws SQLException           If a database access error occurs
     */
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

    /**
     * Retrieves the user ID from the database based on the provided username.
     *
     * @param username The username to check
     * @return The user ID if the username exists, -1 otherwise
     */
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

    /**
     * Creates a new account in the database if the username doesn't already exist.
     *
     * @param username           The username of the new account
     * @param encryptedPassword  The encrypted password of the new account
     * @return true if the account was created successfully, false otherwise
     */
    public boolean createAccount(String username, String encryptedPassword) {
        // Check if the username already exists
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        // If the username doesn't exist, insert the new user
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

    /**
     * Checks if the username exists in the database.
     *
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    private boolean usernameExists(String username) {
        String query = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next(); // Returns true if the username exists, false otherwise
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Checks if the username exists in the database
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public String getPassword(String username) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("password");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Stores a message in the database, either as a broadcast or a direct message.
     *
     * @param senderUsername    The username of the message sender
     * @param receiverUsername  The username of the message receiver (null for broadcast messages)
     * @param message           The text content of the message
     * @param timestamp         The timestamp of the message
     */
    public void storeMessageInDB(String senderUsername, String receiverUsername, String message, Timestamp timestamp, String messageType) {
        String query;
        switch (messageType) {
            case "broadcast":
                query = "INSERT INTO messages (sender_id, message_text, timestamp, team_id) VALUES (?, ?, ?, ?)";
                break;
            case "friends":
                query = "INSERT INTO messages (sender_id, receiver_id, message_text, timestamp, team_id) VALUES (?, ?, ?, ?, ?)";
                break;
            case "direct":
                query = "INSERT INTO direct_messages (sender_id, receiver_id, message_text, timestamp) VALUES (?, ?, ?, ?)";
                break;
            default:
                throw new IllegalArgumentException("Invalid message type: " + messageType);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int senderId = getUserId(senderUsername);
            if (senderId == -1) {
                System.out.println("Sender not found in the database");
                return;
            }
            preparedStatement.setInt(1, senderId);

            if ("direct".equals(messageType)) {
                int receiverId = getUserId(receiverUsername);
                if (receiverId == -1) {
                    System.out.println("Receiver not found in the database");
                    return;
                }
                preparedStatement.setInt(2, receiverId);
            }

            preparedStatement.setString("direct".equals(messageType) ? 3 : 2, message);
            preparedStatement.setTimestamp("direct".equals(messageType) ? 4 : 3, timestamp);

            if (!"broadcast".equals(messageType)) {
                // If messageType is not "broadcast", set teamId
                int teamId = getTeamId(senderUsername); // Assuming you have a method to retrieve teamId for a user
                preparedStatement.setInt("direct".equals(messageType) ? 5 : 4, teamId);
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
    }



    /**
     * Checks if the teamId exists in the database
     * @param teamId The teamId to check
     * @return true if the teamId exists, false otherwise
     */
    private boolean teamIdIsValid(int teamId) {
        String query = "SELECT 1 FROM teams WHERE team_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, teamId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                boolean teamExists = resultSet.next(); // Returns true if the teamId exists, false otherwise
                System.out.println("teamId: " + teamId + ", teamExists: " + teamExists); // Debug print
                return teamExists;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Deletes a message from the database based on the provided message ID and user ID.
     *
     * @param messageId The ID of the message to be deleted
     * @param userId    The ID of the user attempting to delete the message
     */
    public void deleteMessage(int messageId, int userId) {
        String query = "UPDATE user_messages SET is_deleted = true WHERE message_id = ? AND user_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, messageId);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Retrieves the username of the message receiver from the database based on the provided sender's username.
     *
     * @param senderUsername The username of the message sender
     * @return The username of the message receiver, or null if not found
     */
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

    /**
     * Retrieves the username of a user from the database based on the provided user ID.
     *
     * @param userId The ID of the user whose username is to be retrieved
     * @return The username of the user, or null if not found
     */
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

    /**
     * Retrieves a list of all usernames from the database.
     *
     * @return List of all usernames
     */
    public List<String> getAllUsernames() {
        List<String> usernames = new ArrayList<>();
        String query = "SELECT username FROM users";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                usernames.add(resultSet.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usernames;
    }

    /**
     * Closes the database connection if it is open.
     */
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