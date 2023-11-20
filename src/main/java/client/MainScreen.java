package client;

import database.OnLimeDB;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MainScreen {

    @FXML
    private ListView<String> messageListView;

    @FXML
    private TextField messageTextField;

    private final OnLimeDB databaseConnector = new OnLimeDB();
    private final int senderUserId = 1; // Replace with the actual user ID of the sender
    private final int receiverUserId = 2; // Replace with the actual user ID of the receiver

    @FXML
    void initialize() {
        // Load existing messages from the database
        loadMessagesFromDatabase();
    }

//    @FXML
//    void sendMessage() {
//        // Get the typed message from the TextField
//        String newMessage = messageTextField.getText();
//
//        // Display the new message in the ListView
//        messageListView.getItems().add("You: " + newMessage);
//
//        // Insert the new message into the database
//        insertMessageIntoDatabase(senderUserId, receiverUserId, newMessage);
//
//        // Clear the TextField after sending the message
//        messageTextField.clear();
//    }

    private void loadMessagesFromDatabase() {
        try (Connection connection = databaseConnector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)")) {
            preparedStatement.setInt(1, senderUserId);
            preparedStatement.setInt(2, receiverUserId);
            preparedStatement.setInt(3, receiverUserId);
            preparedStatement.setInt(4, senderUserId);

            // Execute the select statement
            var resultSet = preparedStatement.executeQuery();

            // Display the retrieved messages in the ListView
            while (resultSet.next()) {
                int senderId = resultSet.getInt("sender_id");
                String content = resultSet.getString("content");
                messageListView.getItems().add("Sender " + senderId + ": " + content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//    private void insertMessageIntoDatabase(int senderId, int receiverId, String content) {
//        try (Connection connection = databaseConnector.getConnection();
//             PreparedStatement preparedStatement = connection.prepareStatement(
//                     "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)")) {
//            preparedStatement.setInt(1, senderId);
//            preparedStatement.setInt(2, receiverId);
//            preparedStatement.setString(3, content);
//
//            // Execute the insert statement
//            preparedStatement.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
}
