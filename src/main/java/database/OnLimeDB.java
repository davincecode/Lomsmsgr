package database;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles all database operations.
 */
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

    /* Fetching Database */

    public List<Message> getAllBroadcastMessages() {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT message_id, message_text FROM broadcast_messages";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("message_id");
                String text = resultSet.getString("message_text");
                messages.add(new Message(id, text));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<Message> getAllFriendsMessages() {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT message_id, message_text FROM friends_messages";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("message_id");
                String text = resultSet.getString("message_text");
                messages.add(new Message(id, text));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<Message> getAllDirectMessages() {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT message_id, message_text FROM direct_messages";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("message_id");
                String text = resultSet.getString("message_text");
                messages.add(new Message(id, text));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
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
                // Handle broadcast message
                query = "INSERT INTO broadcast_messages (sender_id, message_text, timestamp) VALUES (?, ?, ?)";
                break;
            case "friends":
                // Handle friends message
                query = "INSERT INTO friends_messages (sender_id, receiver_id, message_text, timestamp) VALUES (?, ?, ?, ?)";
                break;
            case "direct":
                // Handle direct message
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

    public void insertFriendMessage(int senderId, int receiverId, String message) {
        String query = "INSERT INTO friends_messages (sender_id, receiver_id, message_text) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, senderId);
            preparedStatement.setInt(2, receiverId);
            preparedStatement.setString(3, message);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* Deletion of Message from Database */
    public void deleteBroadcastMessage(int messageId, int senderId) {
        String query = "DELETE FROM broadcast_messages WHERE message_id = ? AND sender_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, messageId);
            preparedStatement.setInt(2, senderId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteFriendsMessage(int messageId, int senderId, int receiverId) {
        String query = "DELETE FROM friends_messages WHERE message_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, messageId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteDirectMessage(int messageId) {
        String query = "DELETE FROM direct_messages WHERE message_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, messageId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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