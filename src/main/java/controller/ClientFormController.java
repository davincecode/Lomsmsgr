package controller;

import database.OnLimeDB;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import server.Server;

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
    public TextField txtMsg;
    public Text txtLabelUR;
    public Text txtLabelBL;

    private Server server;
    private static int userId;
    private OnLimeDB onLimeDB;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private StringProperty clientNameProperty = new SimpleStringProperty("Client");

    @FXML
    private ListView<String> usersList;

    /**
     * Initializes the client controller, establishes a connection to the server, and sets up the user interface.
     */
    public void initialize() {
        onLimeDB = new OnLimeDB();

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
        sendMsg(txtMsg.getText());
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
                onLimeDB.storeMessageInDB(clientNameProperty.get(), null, msgToSend, new Timestamp(System.currentTimeMillis()), 1);
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
        String name = msg.split("-")[0];
        String msgFromServer = msg.split("-")[1];

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5, 5, 5, 10));

        HBox hBoxName = new HBox();
        hBoxName.setAlignment(Pos.CENTER_LEFT);
        Text textName = new Text(name);
        TextFlow textFlowName = new TextFlow(textName);
        hBoxName.getChildren().add(textFlowName);

        Text text = new Text(msgFromServer);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle("-fx-background-color: #abb8c3; -fx-font-weight: bold; -fx-background-radius: 20px");
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        text.setFill(Color.color(0, 0, 0));

        hBox.getChildren().add(textFlow);

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            onLimeDB.deleteMessage(messageId, userId);
            vBox.getChildren().remove(hBox);
        });

        hBox.getChildren().add(deleteButton);

        Platform.runLater(() -> {
            vBox.getChildren().add(hBoxName);
            vBox.getChildren().add(hBox);
        });
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
        Platform.runLater(() -> {
            // Clear the usersList
            usersList.getItems().clear();
            // Add all usernames to the usersList
            usersList.getItems().addAll(allUsernames);
        });
    }

}