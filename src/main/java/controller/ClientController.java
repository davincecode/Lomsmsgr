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
    private TextField tf_message;

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

    public ClientController() {
    }

    public void initialize() {
        txtLabel.setText(clientName);

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 3306);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                System.out.println("Client connected");
                ServerController.receiveMessage(clientName + " joined.");

                while (socket.isConnected()) {
                    String receivingMsg = dataInputStream.readUTF();
                    receiveMessage(receivingMsg, vb_messages);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        vb_messages.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) -> sp_main.setVvalue((Double) newValue));
    }

    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }

    public void usernameUpdate(ActionEvent actionEventEvent) {
        System.out.println("Username update clicked");
    }

    public void sendButtonOnAction(ActionEvent actionEvent) {
        sendMsg(tf_message.getText());
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

            tf_message.clear();
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

    @FXML
    void receiveMessage(String message, VBox vb_messages) {
        Platform.runLater(() -> {
            String[] messageArray = message.split("-");
            String sender = messageArray[0];
            String content = messageArray[1];

            HBox hBox = createMessageHBox(content);
            vb_messages.getChildren().addAll(hBox, createTimeHBox());
        });
    }

    @FXML
    void retrieveMessages(ActionEvent event) throws SQLException {
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

    @FXML
    void markAsRead(ActionEvent event) throws SQLException {
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

    public void setClientName(String name) {
        clientName = name;
    }

    @FXML
    private void addUserOnAction(MouseEvent event) throws IOException {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(ap_main.getScene().getWindow());
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/view/MainScreen.fxml"))));
        stage.setTitle("OffLime Chat");
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.show();
    }
}
