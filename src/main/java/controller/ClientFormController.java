package controller;

import database.OnLimeDB;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientFormController {
    public AnchorPane pane;
    public ScrollPane scrollPane;
    public VBox vBox;
    public TextField txtMsg;
    public Text txtLabelUR;
    public Text txtLabelBL;
    
    private static int userId;
    private OnLimeDB onLimeDB;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private StringProperty clientNameProperty = new SimpleStringProperty("Client");

    @FXML
    private ListView<String> userListView;

    public void initialize() {
        onLimeDB = new OnLimeDB();
        // String clientName = clientNameProperty.get();

        // Fetch the userId when initializing the controller
        txtLabelUR.textProperty().bind(clientNameProperty);
        txtLabelBL.textProperty().bind(clientNameProperty);


        // Fetch the userId when initializing the controller
        String username = "yourUsername";
        userId = onLimeDB.getUserId(username);

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

    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerFormController.receiveMessage(clientNameProperty.get() + " left.");
    }

    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }

    public void sendButtonOnAction(ActionEvent actionEvent) {
        sendMsg(txtMsg.getText());
    }

    private void sendMsg(String msgToSend) {
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

    public void clickedUsername(MouseEvent event) {
        System.out.println("Clicked Username");
        // Get the clicked item
        // System.out.println("Clicked on " + usersList.getSelectionModel().getSelectedItem());
        // Set the receiverUsername to the clicked username
        // receiverUsername = clickedUsername;

        // Add the clicked username to the usersListDM ListView
        // usersListDM.getItems().add(clickedUsername);
    }

    public void setClientName(String name) {
        clientNameProperty.set(name);
    }
}
