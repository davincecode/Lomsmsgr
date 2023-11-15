package com.davincecode.onlime;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HashMap;

public class MainController {

    @FXML
    public TextField usernameTextField;
    @FXML
    public TextField passwordTextField;
    @FXML
    private Label notificationLabel; // Add this line for the notification label
    @FXML
    private PasswordField hiddenPasswordTextField;
    @FXML
    private CheckBox showPassword;

    private final HashMap<String, String> loginInfo = new HashMap<>();
    private final Encryptor encryptor = new Encryptor();
    private final OnLimeDB databaseConnector = new OnLimeDB();

    // Database connection from ENV
    private Dotenv dotenv = Dotenv.load();
    private Connection connection;

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
                        Parent dashboard = FXMLLoader.load(getClass().getResource("dashboard.fxml"));
                        // Get the current stage
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        // Set the scene to the dashboard
                        stage.setScene(new Scene(dashboard));
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
}
