package controller;

import database.Message;
import database.OnLimeDB;
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

    /* Sending message */
    public TextField txtMsg;
    public TextField txtMsgFriends;
    public TextField txtMsgDM;

    /* Update Username */
    public Text txtLabelUR;
    public Text txtLabelBL;
    public Text txtLabelProfile;

    /* get the userId from the database */
    private static int userId;
    private static int senderId;

    /* Server */
    private Server server;
    private OnLimeDB onLimeDB;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private StringProperty clientNameProperty = new SimpleStringProperty("Client");

    /* Users list */
    @FXML
    private ListView<String> usersList;
    @FXML
    private ListView<String> friendsList;
    @FXML
    private ListView<String> directMessage;

    /* TabPane */
    @FXML
    private TabPane tabPane;

    /* Tabs */
    @FXML
    private Tab home;
    @FXML
    private Tab addFriends;
    @FXML
    private Tab directMsg;

    /* VBoxes */
    @FXML
    private VBox vBoxBroadcast;
    @FXML
    private VBox vBoxFriends;
    @FXML
    private VBox vBoxDM;


    /**
     * Initializes the client controller, establishes a connection to the server, and sets up the user interface.
     */
    public void initialize() {
        onLimeDB = new OnLimeDB();
        // Load friends list
//        loadFriendsList();

        // Load broadcast messages
        List<Message> broadcastMessages = onLimeDB.getAllBroadcastMessages();
        for (Message message : broadcastMessages) {
            Label messageLabel = new Label(message.getText());
            vBoxBroadcast.getChildren().add(messageLabel);
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
        txtLabelProfile.textProperty().bind(clientNameProperty);

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

        // Get all usernames from the database and add them to the friendsList
        List<Integer> allFriends = onLimeDB.getAllUserIdsFromFriends();
        for (Integer friend : allFriends) {
            String usernameFriend = onLimeDB.getUsernameById(friend);
            friendsList.getItems().add(usernameFriend);
        }

        // Load users list
        updateUsersList();
        loadFriendsList();

        // Todo: DM List

        // Adds users to friends and message list
        usersList.setCellFactory(param -> new UserListCell(friendsList, directMessage, dataOutputStream, txtMsg, txtMsgFriends, txtMsgDM, vBoxBroadcast, vBoxFriends, vBoxDM, tabPane, home, addFriends, directMsg, clientNameProperty, onLimeDB));

        // Add a listener to the loggedInUsers list in the server
        server.getLoggedInUsers().addListener((ListChangeListener<String>) change -> {
            // Update the usersList on the JavaFX application thread
            Platform.runLater(this::updateUsersList);
        });

        // Listener for the list of logged-in users
        server.getLoggedInUsers().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        updateUsersList();
                        loadFriendsList();
                    }
                });
            }
        });

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
                        receiveMessage(receivingMsg, this.vBoxBroadcast, onLimeDB, 0, username);
                    } catch (SocketException e) {
                        System.out.println("Socket was closed, exiting read loop");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        this.vBoxBroadcast.heightProperty().addListener((observableValue, oldValue, newValue) ->
                scrollPane.setVvalue((Double) newValue));
        this.vBoxFriends.heightProperty().addListener((observableValue, oldValue, newValue) ->
                scrollPane.setVvalue((Double) newValue));
        this.vBoxDM.heightProperty().addListener((observableValue, oldValue, newValue) ->
                scrollPane.setVvalue((Double) newValue));

    }

    // Load users friends list
    public void loadFriendsList() {
        // Fetch all user_ids from the friends table
        List<Integer> allUserIds = onLimeDB.getAllUserIdsFromFriends();

        // Fetch the username for each user_id and add it to the friendsList
        for (Integer userId : allUserIds) {
            String username = onLimeDB.getUsernameById(userId);
            if (username != null && !friendsList.getItems().contains(username)) {
                friendsList.getItems().add(username);
            }
        }
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

    // Handles the action event when the user presses the Enter key in the message text field.
    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }
    public void txtMsgOnActionFriends(ActionEvent actionEvent) {
        sendButtonOnActionFriends(actionEvent);
    }
    public void txtMsgOnActionDM(ActionEvent actionEvent) {
        sendButtonOnActionDM(actionEvent);
    }

   // Handles the action event when the user clicks the "Send" button.
   public void sendButtonOnAction(ActionEvent actionEvent) {
       String message = txtMsg.getText();
       if (!message.isEmpty()) {
           sendMsg(message, vBoxBroadcast);
           txtMsg.clear();
       }
   }

    public void sendButtonOnActionFriends(ActionEvent actionEvent) {
        String message = txtMsgFriends.getText();
        if (!message.isEmpty()) {
            sendMsg(message, vBoxFriends);
            txtMsgFriends.clear();
        }
    }

    public void sendButtonOnActionDM(ActionEvent actionEvent) {
        String selectedUser = directMessage.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            String message = txtMsgDM.getText();
            if (!message.isEmpty()) {
                sendMsg(message, vBoxDM);
                txtMsgDM.clear();
            }
        } else {
            System.out.println("No user selected.");
        }
    }

    private void displayMessageInVBox(String message, VBox destinationVBox) {
        try {
            System.out.println("displayMessageInVBox called");
            Platform.runLater(() -> {
                HBox hBox = new HBox();
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.setPadding(new Insets(5, 5, 0, 10));

                Text text = new Text(message);
                text.setStyle("-fx-font-size: 14");
                TextFlow textFlow = new TextFlow(text);

                textFlow.setStyle("-fx-background-color: #0693e3; -fx-font-weight: bold; -fx-color: white; -fx-background-radius: 20px");
                textFlow.setPadding(new Insets(5, 10, 5, 10));
                text.setFill(Color.color(1, 1, 1));

                // Senders Delete Button
                Button deleteButton = new Button("Delete");
                deleteButton.setOnAction(event -> {
                    // Remove the message from the VBox
                    destinationVBox.getChildren().remove(hBox);

                    // Delete the message from the database
                    // You need to pass the correct messageId and senderId
                    // onLimeDB.deleteBroadcastMessage(messageId, senderId);
                });

                // Add the delete button before the textFlow
                hBox.getChildren().addAll(deleteButton, textFlow);
                System.out.println("hBox children: " + hBox.getChildren());
                destinationVBox.getChildren().add(hBox);
                System.out.println("destinationVBox children: " + destinationVBox.getChildren());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the server.
     *
     * @param msgToSend The message to send.
     */
    private void sendMsg(String msgToSend, VBox destinationVBox) {
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

            destinationVBox.getChildren().add(hBox);
            destinationVBox.getChildren().add(hBoxTime);

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
    public void receiveMessage(String msg, VBox vBox, OnLimeDB onLimeDB, int messageId, String username) throws IOException {
        // Get the currently selected tab
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        // Get the username from the message
        String[] parts = msg.split("-");
        String senderName = parts[0];
        String msgFromServer = parts.length > 1 ? parts[1] : "";
        if (msgFromServer == null) {
            msgFromServer = "";  // set to empty string or some default value
        }

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
                int receiverId = onLimeDB.getUserId(username);
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
        ListView<String> sourceList = (ListView<String>) event.getSource();
        if (sourceList == usersList || sourceList == friendsList) {
            // disable click
            event.consume();
        } else if (sourceList == directMessage) {
            System.out.println("Clicked Username");
        }
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

            // Display the sent message on the sender's vBoxFriends
            displayMessageInVBox(clientNameProperty.get() + ": " + message, vBoxFriends);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to handle direct messages
    public void handleDirectMessage(String username, String message) {
        try {
            // Send the message to the specific user in the directMessage ListView
            dataOutputStream.writeUTF(username + "-" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}