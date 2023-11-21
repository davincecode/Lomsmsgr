package controller;

import database.OnLimeDB;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML
    public TextField usernameTextField;

    @FXML
    public TextField passwordTextField;

    @FXML
    private PasswordField hiddenPasswordTextField;

    @FXML
    private CheckBox showPassword;

    @FXML
    private Label notificationLabel;

    private final Encryptor encryptor = new Encryptor();
    private final OnLimeDB databaseConnector = new OnLimeDB();
    private Dotenv dotenv = Dotenv.load();

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
                        loadMainScreen(event, username);
                        System.out.println("Login successful.");
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
    void createAccount(ActionEvent event) throws SQLException, NoSuchPaddingException,
            InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {
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
    void loadMainScreen(ActionEvent event, String username) throws IOException {
        // Load the dashboard
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainScreen.fxml"));
        Parent mainScreen = loader.load();
        // Get the controller
        ClientController controller = loader.getController();
        // Set the client name
        controller.setClientName(username);
        // Get the current stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        // Set the scene to the dashboard
        stage.setScene(new Scene(mainScreen));
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
}
