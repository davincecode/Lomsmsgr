package controller;

import database.OnLimeDB;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class ClientController {

    @FXML
    private AnchorPane ap_main;
    @FXML
    private ScrollPane sp_main;
    @FXML
    private VBox vb_messages;
    @FXML
    private TextField txtMsg;
    @FXML
    private Text txtLabel;
    private final HashMap<String, String> loginInfo = new HashMap<>();
    private final Encryptor encryptor = new Encryptor();
    private final OnLimeDB databaseConnector = new OnLimeDB();
    private Dotenv dotenv = Dotenv.load();
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String clientName = "Username";

    public void initialize() {
        txtLabel.setText(clientName);

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 3307);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                System.out.println("Client connected");

                // Send a message to the server indicating that the client has joined
                dataOutputStream.writeUTF(clientName + " joined.");
                dataOutputStream.flush();
                System.out.println("Sent message: " + clientName + " joined.");

                while (socket.isConnected()) {
                    try {
                        String receivingMsg = dataInputStream.readUTF();
                        receiveMessage(receivingMsg, vb_messages);
                    } catch (EOFException e) {
                        System.out.println("Client disconnected");
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        vb_messages.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) -> sp_main.setVvalue((Double) newValue));
    }

    public void setClientName(String username) {
        this.clientName = username;
        txtLabel.setText(clientName);
    }

    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }

    public void sendButtonOnAction(ActionEvent actionEvent) {
        sendMsg(txtMsg.getText());
    }

    private void sendMsg(String msgToSend) {
        if (!msgToSend.isEmpty()) {
            HBox hBox = createMessageHBox(msgToSend);

            vb_messages.getChildren().addAll(hBox, createTimeHBox());

            try {
                dataOutputStream.writeUTF(clientName + "-" + msgToSend);
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            txtMsg.clear();
        }
    }

    private HBox createMessageHBox(String msgToSend) {
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
        return hBox;
    }

    private HBox createTimeHBox() {
        HBox hBoxTime = new HBox();
        hBoxTime.setAlignment(Pos.CENTER_RIGHT);
        hBoxTime.setPadding(new Insets(0, 5, 5, 10));
        String stringTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        Text time = new Text(stringTime);
        time.setStyle("-fx-font-size: 8");

        hBoxTime.getChildren().add(time);
        return hBoxTime;
    }

    public static void receiveMessage(String msg, VBox vb_messages) throws IOException {
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

        Platform.runLater(() -> {
            vb_messages.getChildren().add(hBoxName);
            vb_messages.getChildren().add(hBox);
        });
    }


    public void retrieveMessages(ActionEvent event) throws SQLException {
        String query = "SELECT sender, content FROM messages WHERE receiver = ?";
        try (PreparedStatement preparedStatement = databaseConnector.getConnection().prepareStatement(query)) {
            preparedStatement.setString(1, getUsername());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String sender = resultSet.getString("sender");
                    String content = resultSet.getString("content");

                    System.out.println("Message from " + sender + ": " + content);
                }
            }
        }
    }

    public void markAsRead(ActionEvent event) throws SQLException {
        String query = "UPDATE messages SET status = 'read' WHERE receiver = ?";
        try (PreparedStatement preparedStatement = databaseConnector.getConnection().prepareStatement(query)) {
            preparedStatement.setString(1, getUsername());

            preparedStatement.executeUpdate();

        }
    }

    private String getUsername() {
        return txtLabel.getText();
    }

    public void closeConnection() {
        databaseConnector.closeConnection();
    }


    @FXML
    private void addUserOnAction(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainScreen.fxml"));
        Parent mainScreen = loader.load();

        // Get the controller (which is assumed to be ClientController in MainScreen.fxml)
        ClientController mainScreenController = loader.getController();

        // Pass the username to the MainScreenController
        mainScreenController.setClientName(getUsername());

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(ap_main.getScene().getWindow());
        stage.setScene(new Scene(mainScreen));
        stage.setTitle("OffLime Chat");
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.show();
    }

}
