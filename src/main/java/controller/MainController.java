package controller;

import database.OnLimeDB;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class MainController {
    public AnchorPane ap_main;
    public ScrollPane sp_main;
    public VBox vb_messages;
    public TextField tf_message;
    public Text txtLabel;

    @FXML
    public TextField usernameTextField;
    @FXML
    public TextField passwordTextField;
    @FXML
    private Label notificationLabel;
    @FXML
    private PasswordField hiddenPasswordTextField;
    @FXML
    private CheckBox showPassword;

    private final HashMap<String, String> loginInfo = new HashMap<>();
    private final Encryptor encryptor = new Encryptor();
    private final OnLimeDB databaseConnector = new OnLimeDB();
    private Dotenv dotenv = Dotenv.load();
    private Connection connection;

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String clientName = "Client";



    public MainController() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String DB_URL = dotenv.get("DB_URL");
            String DB_USER = dotenv.get("DB_USER");
            String DB_PASSWORD = dotenv.get("DB_PASSWORD");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    // Client Controller
    public void initialize(){
        txtLabel.setText(clientName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socket = new Socket("localhost", 3001);
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    System.out.println("Client connected");
                    ServerController.receiveMessage(clientName+" joined.");

                    while (socket.isConnected()){
                        String receivingMsg = dataInputStream.readUTF();
                        receiveMessage(receivingMsg, ClientFormController.this.vb_messages);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();

        this.vb_messages.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                sp_main.setVvalue((Double) newValue);
            }
        });

    }

    // Clean up after closing the application
    public void shutdown() {
        ServerController.receiveMessage(clientName+" left.");
    }

    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }

    public void sendButtonOnAction(ActionEvent actionEvent) {
        sendMsg(tf_message.getText());
    }

    private void sendMsg(String msgToSend) {
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

            vb_messages.getChildren().add(hBox);
            vb_messages.getChildren().add(hBoxTime);

            try {
                dataOutputStream.writeUTF(clientName + "-" + msgToSend);
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            tf_message.clear();
        }
    }


    @FXML
    void changeVisibility(ActionEvent event) {
        if (showPassword.isSelected()) {
            passwordTextField.setText(hiddenPasswordTextField.getText());
            passwordTextField.setVisible(true);
            hiddenPasswordTextField.setVisible(false);
            return;
        }
        hiddenPasswordTextField.setText(passwordTextField.getText());
        hiddenPasswordTextField.setVisible(true);
        passwordTextField.setVisible(false);
    }

    @FXML
    void loginHandler(ActionEvent event) throws Exception {
        String username = usernameTextField.getText();
        String password = getPassword();

        String query = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = databaseConnector.getConnection().prepareStatement(query)) {
            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String dbPassword = resultSet.getString("password");
                    if (encryptor.encryptString(password).equals(dbPassword)) {
                        // Load the dashboard
                        Parent mainScreen = FXMLLoader.load(getClass().getResource("/views/MainScreen.fxml"));
                        // Get the current stage
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        // Set the scene to the dashboard
                        stage.setScene(new Scene(mainScreen));
                    } else {
                        showNotification("Invalid credentials. Please try again.", true);
                    }
                } else {
                    showNotification("Invalid credentials. Please try again.", true);
                }
            }
        }
    }

    @FXML
    void createAccount(ActionEvent event) throws SQLException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
            String username = usernameTextField.getText();
            String password = getPassword();

            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = databaseConnector.getConnection().prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, encryptor.encryptString(password));

                preparedStatement.executeUpdate();

                // Always show notification after creating a user
                showNotification("User has been registered.", false);
            }
    }

    @FXML
    void sendMessage(ActionEvent event) throws SQLException {
        // Implement the logic to send a message to everyone
        String messageContent = tf_message.getText();

        // Retrieve all users from the database
        String query = "SELECT username FROM users";
        try (PreparedStatement preparedStatement = databaseConnector.getConnection().prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                String recipient = resultSet.getString("username");

                // Skip sending a message to the sender (optional)
                if (recipient.equals(getUsername())) {
                    continue;
                }

                // Insert the message into the database
                String insertQuery = "INSERT INTO messages (sender, receiver, content) VALUES (?, ?, ?)";
                try (PreparedStatement insertStatement = databaseConnector.getConnection().prepareStatement(insertQuery)) {
                    insertStatement.setString(1, getUsername());
                    insertStatement.setString(2, recipient);
                    insertStatement.setString(3, messageContent);

                    insertStatement.executeUpdate();
                }
            }

            showNotification("Message sent to everyone successfully.", false);
        }
    }

    // Receive message
    @FXML
    void receiveMessage(String message, VBox vb_messages) {
        Platform.runLater(() -> {
            String[] messageArray = message.split("-");
            String sender = messageArray[0];
            String content = messageArray[1];

            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setPadding(new Insets(5, 10, 0, 5));

            Text text = new Text(content);
            text.setStyle("-fx-font-size: 14");
            TextFlow textFlow = new TextFlow(text);

            textFlow.setStyle("-fx-background-color: #dfe6e9; -fx-font-weight: bold; -fx-color: white; -fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            text.setFill(Color.color(1, 1, 1));

            hBox.getChildren().add(textFlow);

            HBox hBoxTime = new HBox();
            hBoxTime.setAlignment(Pos.CENTER_LEFT);
            hBoxTime.setPadding(new Insets(0, 10, 5, 5));
            String stringTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Text time = new Text(stringTime);
            time.setStyle("-fx-font-size: 8");

            hBoxTime.getChildren().add(time);

            vb_messages.getChildren().add(hBox);
            vb_messages.getChildren().add(hBoxTime);
        });
    }

    @FXML
    void retrieveMessages(ActionEvent event) throws SQLException {
        // Implement the logic to retrieve messages
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
        // Implement the logic to mark messages as read
        String query = "UPDATE messages SET status = 'read' WHERE receiver = ?";
        try (PreparedStatement preparedStatement = databaseConnector.getConnection().prepareStatement(query)) {
            preparedStatement.setString(1, getUsername());

            preparedStatement.executeUpdate();

            showNotification("Messages marked as read.", false);
        }
    }

    private String getUsername() {
        return usernameTextField.getText();
    }

    private String getPassword() {
        if (passwordTextField.isVisible()) {
            return passwordTextField.getText();
        } else {
            return hiddenPasswordTextField.getText();
        }
    }

    @FXML
    void showNotification(String message, boolean isError) {
        notificationLabel.setText(message);

        if (isError) {
            notificationLabel.setStyle("-fx-text-fill: red; -fx-alignment: center;");
        } else {
            notificationLabel.setStyle("-fx-text-fill: green; -fx-alignment: center");
        }

        notificationLabel.setVisible(true);
    }

    // Close the database connection when the application is closed
    public void closeConnection() {
        databaseConnector.closeConnection();
    }

    public void setClientName(String name) {
        clientName = name;
    }
}
