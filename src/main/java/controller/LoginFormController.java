package controller;

import database.OnLimeDB;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class LoginFormController {
    public TextField txtName;
    public PasswordField txtPassword;
    private OnLimeDB databaseConnector = new OnLimeDB();
    private Encryptor encryptor = new Encryptor();

    public void initialize(){

    }

    public void logInButtonOnAction(ActionEvent actionEvent) throws IOException {
        String username = txtName.getText();
        if (!username.isEmpty() && username.matches("[A-Za-z0-9]+")) {
            String usernameFromDB = databaseConnector.getUsername(username);
            if (usernameFromDB != null) {
                Stage primaryStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ClientForm.fxml"));

                ClientFormController controller = new ClientFormController();
                controller.setClientName(usernameFromDB);
                fxmlLoader.setController(controller);

                primaryStage.setScene(new Scene(fxmlLoader.load()));
                primaryStage.setTitle(usernameFromDB);
                primaryStage.setResizable(false);
                primaryStage.centerOnScreen();

                primaryStage.setOnCloseRequest(windowEvent -> {
                    windowEvent.consume();
                    primaryStage.close();
                });

                primaryStage.show();

                // Close form on Login
                ((Stage) txtName.getScene().getWindow()).close();

                txtName.clear();
            } else {
                new Alert(Alert.AlertType.ERROR, "Username not found in the database").show();
            }
        } else {
            new Alert(Alert.AlertType.ERROR, "Please enter a valid username").show();
        }
    }

    public void createAccount(ActionEvent actionEvent) {
        String username = txtName.getText();
        String password = txtPassword.getText();

        if (!username.isEmpty() && !password.isEmpty() && username.matches("[A-Za-z0-9]+")) {
            try {
                String encryptedPassword = encryptor.encryptString(password);
                boolean isCreated = databaseConnector.createAccount(username, encryptedPassword);

                if (isCreated) {
                    new Alert(Alert.AlertType.INFORMATION, "Account created successfully").show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to create account").show();
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } else {
            new Alert(Alert.AlertType.ERROR, "Please enter a valid username and password").show();
        }
    }

    public void changeVisibility(ActionEvent actionEvent) {
        new Alert(Alert.AlertType.INFORMATION, "This feature is not available yet").show();
    }
}
