package controller;

import client.ClientHandler;
import com.jfoenix.controls.JFXListView;
import database.OnLimeDB;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;


public class ClientFormController implements Initializable {
    public Text txtLabelAllMessageUR;
    public Text txtLabelAllMessageBL;
    public AnchorPane pane;
    public ScrollPane chatBox;
    public VBox vBox;
    public TextField txtMsg;
    public JFXListView<String> usersList;


    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String clientName = "Client";
    private Server server;
    private OnLimeDB databaseConnector;
    private String receiverUsername;
    private List<ClientHandler> clients;


    public void updateUsersList() {
        List<String> loggedInUsers = server.getLoggedInUsers();
        usersList.getItems().clear();
        usersList.getItems().addAll(loggedInUsers);
        usersList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {

            }
        });

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtLabelAllMessageUR.setText(clientName);
        txtLabelAllMessageBL.setText(clientName);

        databaseConnector = new OnLimeDB();
//        User selectedUser = getSelectedUser();
//        receiverUsername = selectedUser.getUsername();

        // Add a mouse click event listener to the userList
        usersList.setOnMouseClicked(event -> clickedUsername(event));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socket = new Socket("localhost", 3001);
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    System.out.println("Client connected");
                    ServerFormController.receiveMessage(clientName+" joined.");

                    // Pass the VBox object and this ClientFormController instance to the ClientHandler constructor
                    ClientHandler clientHandler = new ClientHandler(socket, clients, vBox, ClientFormController.this, clientName);
                    clientHandler.setClientName(clientName);



                    while (socket.isConnected()){
                        String receivingMsg = dataInputStream.readUTF();
                        receiveMessage(receivingMsg, ClientFormController.this.vBox);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();

        this.vBox.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                chatBox.setVvalue((Double) newValue);
            }
        });

        try {
            server = Server.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

        updateUsersList();

    }

    public void shutdown() {
        // cleanup code here...
        ServerFormController.receiveMessage(clientName+" left.");
    }

    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }

    public void clickedUsername(MouseEvent event) {
        // Get the clicked item
        String clickedUsername = usersList.getSelectionModel().getSelectedItem();
        // Set the receiverUsername to the clicked username
        receiverUsername = clickedUsername;
    }

    public void sendButtonOnAction(ActionEvent actionEvent) {
        if (receiverUsername == null || receiverUsername.isEmpty()) {
            System.out.println("No receiver selected. Broadcasting to all.");
//            sendMsg(txtMsg.getText(), null);
            sendMsg("BROADCAST-" + txtMsg.getText(), null);
        } else {
            System.out.println("Sender: " + clientName);
            System.out.println("Receiver: " + receiverUsername);
//            sendMsg(txtMsg.getText(), receiverUsername);
            sendMsg(clientName + "-" + receiverUsername + "-" + txtMsg.getText(), receiverUsername);
        }
    }


    private void sendMsg(String msgToSend, String receiverUsername) {
        if (!msgToSend.isEmpty()){
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
                if (receiverUsername != null) {
                    // Send the message only to the user with receiverUsername
                    dataOutputStream.writeUTF(clientName + "-" + receiverUsername + "-" + msgToSend);
                } else {
                    // Broadcast the message to all users
                    dataOutputStream.writeUTF(clientName + "-" + msgToSend);
                }
                dataOutputStream.flush();

                // Store the message in the database
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                databaseConnector.storeMessageInDB(clientName, receiverUsername, msgToSend, timestamp);

                // Display the message with the sender's name
                displayMessage(clientName, msgToSend, vBox);
            } catch (IOException e) {
                e.printStackTrace();
            }

            txtMsg.clear();
        }
    }

    // Method to display a message in the user interface
    private static void displayMessage(String senderName,String message, VBox vBox) {
        // Create a new Text object with the message
//        Text text = new Text(message);
        Text text = new Text(senderName + ": " + message);


        // Add the Text object to the vBox
        Platform.runLater(() -> vBox.getChildren().add(text));
    }

    public static void receiveMessage(String msg, VBox vBox) throws IOException {
        String[] parts = msg.split("-");
        if (parts.length < 2) {
            System.out.println("Invalid message format");
            return;
        }

        String name = parts[0];
        String msgFromServer = parts[1];

        // Pass the vBox to the displayMessage method
        displayMessage(name, msgFromServer, vBox);

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5,5,5,10));

        HBox hBoxName = new HBox();
        hBoxName.setAlignment(Pos.CENTER_LEFT);
        Text textName = new Text(name);
        TextFlow textFlowName = new TextFlow(textName);
        hBoxName.getChildren().add(textFlowName);

        Text text = new Text(msgFromServer);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle("-fx-background-color: #abb8c3; -fx-font-weight: bold; -fx-background-radius: 20px");
        textFlow.setPadding(new Insets(5,10,5,10));
        text.setFill(Color.color(0,0,0));

        hBox.getChildren().add(textFlow);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                vBox.getChildren().add(hBoxName);
                vBox.getChildren().add(hBox);
            }
        });
    }

    public void setClientName(String name) {
        clientName = name;
    }

}
