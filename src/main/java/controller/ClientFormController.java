package controller;

import database.OnLimeDB;
import database.Message;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import server.Server;
import utils.UserListCell;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class ClientFormController {
    public AnchorPane pane;
    public ScrollPane scrollPane;
    public VBox vBox;
    public VBox vBoxFriends;
    public TextField txtMsg;
    public TextField txtMsgFriends;
    public TextField txtMsgDM;
    public Text txtLabelUR;
    public Text txtLabelBL;

    private Server server;
    private static int userId;
    private static int senderId;
    private OnLimeDB onLimeDB;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private StringProperty clientNameProperty = new SimpleStringProperty("Client");

    @FXML
    private ListView<String> usersList;
    @FXML
    private ListView<String> friendsList;
    @FXML
    private ListView<String> directMessage;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab addFriends;
    @FXML
    private Tab home;
    @FXML
    private Tab directMsg;
    @FXML
    private VBox vBoxDM;

    /**
     * Initializes the client controller, establishes a connection to the server, and sets up the user interface.
     */
    public void initialize() {
        onLimeDB = new OnLimeDB();

        // Load broadcast messages
        List<Message> broadcastMessages = onLimeDB.getAllBroadcastMessages();
        for (Message message : broadcastMessages) {
            Label messageLabel = new Label(message.getText());
            vBox.getChildren().add(messageLabel);
        }

        List<Message> friendsMessages = onLimeDB.getAllFriendsMessages();
        for (Message message : friendsMessages) {
            Label messageLabel = new Label(message.getText());
            vBoxFriends.getChildren().add(messageLabel);
        }

        List<Message> directMessages = onLimeDB.getAllDirectMessages();
        for (Message message : directMessages) {
            Label messageLabel = new Label(message.getText());
            vBoxDM.getChildren().add(messageLabel);
        }

        // Fetch the userId when initializing the controller
        txtLabelUR.textProperty().bind(clientNameProperty);
        txtLabelBL.textProperty().bind(clientNameProperty);


        // Fetch the userId when initializing the controller
        String username = "username";
        userId = onLimeDB.getUserId(username);

        try {
            server = Server.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get all usernames from the database and add them to the usersList
        List<String> allUsernames = onLimeDB.getAllUsernames();
        usersList.getItems().addAll(allUsernames);

        // Adds users to friends and message list
        usersList.setCellFactory(param -> new UserListCell(friendsList, directMessage, dataOutputStream, txtMsg, txtMsgFriends, txtMsgDM, vBox, vBoxFriends, vBoxDM, tabPane, home, addFriends, directMsg, clientNameProperty, onLimeDB));

        // Add a listener to the loggedInUsers list in the server
        server.getLoggedInUsers().addListener((ListChangeListener<String>) change -> {
            // Update the usersList on the JavaFX application thread
            Platform.runLater(() -> {
                updateUsersList();
            });
        });

        // Listener for the list of logged-in users
        server.getLoggedInUsers().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        updateUsersList();
                    }
                });
            }
        });

        // Update the users list when the client form is created
        updateUsersList();

        // Thread to connect to the server
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 3001);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                System.out.println("Client connected");
                ServerFormController.receiveMessage(clientNameProperty.get() + " joined.");
                // debug print
                System.out.println("CLIENT NAME" + clientNameProperty.get());

                while (socket.isConnected() && !socket.isClosed()) {
                    try {
                        String receivingMsg = dataInputStream.readUTF();
                        receiveMessage(receivingMsg, this.vBox, new OnLimeDB(), 0);
                    } catch (SocketException e) {
                        System.out.println("Socket was closed, exiting read loop");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        this.vBox.heightProperty().addListener((observableValue, oldValue, newValue) ->
                scrollPane.setVvalue((Double) newValue));

    }


    /**
     * Shuts down the client when the user closes the window.
     */
    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ServerFormController.receiveMessage(clientNameProperty.get() + " left the chat.");
    }

    /**
     * Handles the action event when the user presses the Enter key in the message text field.
     *
     * @param actionEvent The ActionEvent triggered by pressing the Enter key.
     */
    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }

    /**
     * Handles the action event when the user clicks the send button.
     *
     * @param actionEvent The ActionEvent triggered by clicking the send button.
     */
    public void sendButtonOnAction(ActionEvent actionEvent) {
        // Get the selected tab
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        // If the "Home" tab is selected, send a broadcast message
        if (selectedTab == home) {
            String message = txtMsg.getText();
            if (!message.isEmpty()) {
                sendMsg(message);
                displayMessageInVBox(message, vBox);
                txtMsg.clear();
            }
        }
        // If the "Friends" tab is selected, send a message to all friends
        else if (selectedTab == addFriends) {
            // Get the message from the txtMsgFriends text field
            String message = txtMsgFriends.getText();

            // If a message is entered, send it to each friend
            if (!message.isEmpty()) {
                for (String friend : friendsList.getItems()) {
                    handleFriendsMessage(friend, message);
                }

                // Display the sent message in the sender's vBoxFriends
                // displayMessageInVBox(clientNameProperty.get() + ": " + message, vBoxFriends);
                // txtMsgFriends.clear();

                displayMessageInVBox(message, vBoxFriends);
                txtMsgFriends.clear();
            }
        }
        // If the "Direct Message" tab is selected, send a direct message
        else if (selectedTab == directMsg) {
            // Get the selected user from the directMessage ListView
            String selectedUser = directMessage.getSelectionModel().getSelectedItem();

            // If a user is selected, send a message to that user
            if (selectedUser != null) {
                String message = txtMsgDM.getText();
                if (!message.isEmpty()) {
                    handleDirectMessage(selectedUser, message);
                    displayMessageInVBox(clientNameProperty.get() + ": " + message, vBoxDM);
                    txtMsgDM.clear();
                }
            } else {
                // If no user is selected, show an error message to the user
                System.out.println("No user selected.");
            }
        }
    }

    private void displayMessageInVBox(String message, VBox destinationVBox) {
        HBox hBox = createHBoxForMessage(message);
        destinationVBox.getChildren().add(hBox);
    }

    /**
     * Sends a message to the server.
     *
     * @param msgToSend The message to send.
     */
    private void sendMsg(String msgToSend) {
        System.out.println("sendMsg method called");
        if (!msgToSend.isEmpty()) {

            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_RIGHT);
            hBox.setPadding(new Insets(5, 5, 0, 10));

            Text text = new Text(msgToSend);
            text.setStyle("-fx-font-size: 14");
            TextFlow textFlow = new TextFlow(text);

            textFlow.setStyle("-fx-background-color: #0693e3; -fx-font-weight: bold; -fx-color: white; -fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            text.setFill(Color.color(1, 1, 1));

            hBox.getChildren().add(textFlow);

            HBox hBoxTime = new HBox();
            hBoxTime.setAlignment(Pos.CENTER_RIGHT);
            hBoxTime.setPadding(new Insets(0, 5, 5, 10));
            String stringTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Text time = new Text(stringTime);
            time.setStyle("-fx-font-size: 8");

            hBoxTime.getChildren().add(time);

            vBox.getChildren().add(hBox);
            vBox.getChildren().add(hBoxTime);

            try {
                dataOutputStream.writeUTF(clientNameProperty.get() + "-" + msgToSend);
                dataOutputStream.flush();

                // Store the message in the database
                onLimeDB.storeMessageInDB(clientNameProperty.get(), null, msgToSend, new Timestamp(System.currentTimeMillis()), "broadcast");
            } catch (IOException e) {
                e.printStackTrace();
            }

            txtMsg.clear();
        }
    }

    /**
     * Receives a message from the server and updates the client's UI.
     *
     * @param msg       The received message.
     * @param vBox      The VBox containing the chat messages.
     * @param onLimeDB  The database manager for storing messages.
     * @param messageId The ID of the received message.
     * @throws IOException If an I/O error occurs during message processing.
     */
    public void receiveMessage(String msg, VBox vBox, OnLimeDB onLimeDB, int messageId) throws IOException {
        // Get the currently selected tab
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        // Get the username from the message
        String[] parts = msg.split("-");
        String senderName = parts[0];
        String msgFromServer = parts.length > 1 ? parts[1] : "";

        // Create a new HBox for the message
        HBox hBox = createHBoxForMessage(msg);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5, 5, 5, 10));

        HBox hBoxName = new HBox();
        hBoxName.setAlignment(Pos.CENTER_LEFT);
        Text textName = new Text(senderName);
        TextFlow textFlowName = new TextFlow(textName);
        hBoxName.getChildren().add(textFlowName);

        Text text = new Text(msgFromServer);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle("-fx-background-color: #abb8c3; -fx-font-weight: bold; -fx-background-radius: 20px");
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        text.setFill(Color.color(0, 0, 0));

        hBox.getChildren().add(textFlow);

        /* Delete message from database */
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            // Determine which type of message to delete based on the VBox
            if (vBox == vBox) {
                int senderId = onLimeDB.getUserId(clientNameProperty.get());
                onLimeDB.deleteBroadcastMessage(messageId, senderId);
            } else if (vBox == vBoxFriends) {
                int senderId = onLimeDB.getUserId(clientNameProperty.get());
                int receiverId = onLimeDB.getUserId(senderName);
                onLimeDB.deleteFriendsMessage(messageId, senderId, receiverId);
            } else if (vBox == vBoxDM) {
                onLimeDB.deleteDirectMessage(messageId);
            }
            vBox.getChildren().remove(hBox);
        });

        hBox.getChildren().add(deleteButton);

        // If the "Home" tab is selected, add the message to vBox
        if (selectedTab != null && selectedTab.equals(home)) {
            Platform.runLater(() -> {
                vBox.getChildren().add(hBoxName);
                vBox.getChildren().add(hBox);
            });
        }
        // If the "Friends" tab is selected, add the message to vBoxFriends
        else if (selectedTab != null && selectedTab.equals(addFriends)) {
            // Check if the sender is a friend before adding the message
            if (friendsList.getItems().contains(senderName)) {
                Platform.runLater(() -> {
                    vBoxFriends.getChildren().add(hBoxName);
                    vBoxFriends.getChildren().add(hBox);
                });
            }
        }
        // If the "Direct Message" tab is selected, add the message to vBoxDM
        else if (selectedTab != null && selectedTab.equals(directMsg)) {
            // Check if the sender is the selected user before adding the message
            if (directMessage.getSelectionModel().getSelectedItem().equals(senderName)) {
                Platform.runLater(() -> {
                    vBoxDM.getChildren().add(hBoxName);
                    vBoxDM.getChildren().add(hBox);
                });
            }
        }
    }

    /**
     * Creates an HBox for a message.
     *
     * @param msg The message to create an HBox for.
     * @return The HBox containing the message.
     */
    private HBox createHBoxForMessage(String msg) {
        HBox hBox = new HBox();
        // ... code to set up the HBox with the message ...
        return hBox;
    }

    /**
     * Handles the event when the user clicks on a username in the users list.
     *
     * @param event The MouseEvent triggered by clicking on a username.
     */
    public void clickedUsername(MouseEvent event) {
        System.out.println("Clicked Username");
        // Todo: Implement private messaging
    }


    /**
     * Sets the client's username when the user logs in.
     * It's on the user area where the username is displayed.
     * @param name The username of the user that logged in.
     */
    public void setClientName(String name) {
        // Set the client's username
        clientNameProperty.set(name);

    }

    /**
     * Updates the users list with the currently logged-in users.
     */
    public void updateUsersList() {
        // Get all usernames from the database
        List<String> allUsernames = onLimeDB.getAllUsernames();
        allUsernames.remove(clientNameProperty.get());
        Platform.runLater(() -> {
            // Clear the usersList
            usersList.getItems().clear();
            // Add all usernames to the usersList
            usersList.getItems().addAll(allUsernames);
        });
    }

    /* Handles Messaging */

    // Method to handle broadcast messages
    private void broadcastMessage(String message) {
        // Implement the logic to handle a broadcast message
        System.out.println("Broadcast message: " + message);
        // TODO: Send the message to all users in the usersList ListView
    }

    // Method to handle friends messages
    public void handleFriendsMessage(String friendUsername, String message) {
        try {
            // Send the message to the server
            dataOutputStream.writeUTF(friendUsername + "-" + message);

            // Get sender and receiver IDs
            int senderId = onLimeDB.getUserId(clientNameProperty.get());
            int receiverId = onLimeDB.getUserId(friendUsername);

            // Insert the message into the database
            onLimeDB.insertFriendMessage(senderId, receiverId, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to handle direct messages
    private void handleDirectMessage(String username, String message) {
        // Implement the logic to handle a direct message
        System.out.println("Direct message to " + username + ": " + message);
        // TODO: Send the message to the specific user in the directMessage ListView
    }

}