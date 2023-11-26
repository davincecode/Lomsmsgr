package controller;

import database.OnLimeDB;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class LoginFormController {
    @FXML
    public TextField txtName;
    @FXML
    public TextField txtNameP;

    private OnLimeDB onLimeDB;
    private Encryptor encryptor;

    @FXML
    private CheckBox showPassword;
    @FXML
    private TextField passwordTextField;

    public void initialize() {

        onLimeDB = new OnLimeDB();
        encryptor = new Encryptor();
    }

    public void logInButtonOnAction(ActionEvent actionEvent) throws IOException {
        String username = txtName.getText();
        String enteredPassword = txtNameP.getText();

        int userId = onLimeDB.getUserId(username);
        if (userId != -1) {
            try {
                String storedPassword = onLimeDB.getPassword(username);
                String encryptedEnteredPassword = encryptor.encryptString(enteredPassword);
                if (encryptedEnteredPassword.equals(storedPassword)) {
                    // Password matches, proceed to ClientForm.fxml
                    if (!txtName.getText().isEmpty() && txtName.getText().matches("[A-Za-z0-9]+")) {
                        // Create the new stage for the client form
                        Stage primaryStage = new Stage();
                        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ClientForm.fxml"));

                        // Load the FXML file using the FXMLLoader
                        Parent root = fxmlLoader.load();

                        // Get the controller associated with the FXML file
                        ClientFormController controller = fxmlLoader.getController();
                        // controller.setClientName(txtName.getText());
                        controller.setClientName(username);
                        // Call after the user has logged in
                        // controller.userLoggedIn(username);

                        // Set up the new stage
                        primaryStage.setScene(new Scene(root));
                        // primaryStage.setTitle(txtName.getText());
                        primaryStage.setTitle(username);
                        primaryStage.setResizable(false);
                        primaryStage.centerOnScreen();

                        // Set the close request event
                        primaryStage.setOnCloseRequest(windowEvent -> controller.shutdown());

                        // Close the login stage
                        ((Stage) txtName.getScene().getWindow()).close();

                        // Show the new stage
                        Platform.runLater(primaryStage::show);

                        txtName.clear();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Please enter your name").show();
                    }
                } else {
                    // Password does not match, show an error message
                    new Alert(Alert.AlertType.ERROR, "Incorrect password").show();
                }
            } catch (NoSuchAlgorithmException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to encrypt password").show();
            }
        } else {
            new Alert(Alert.AlertType.ERROR, "User does not exist").show();
        }
    }

    /*
    *
    * This method is used to create an account
    * @param actionEvent
    *
    */
    public void createAccountOnAction(ActionEvent actionEvent) {
        String username = txtName.getText();
        String password = txtNameP.getText();

        try {
            String encryptedPassword = encryptor.encryptString(password);
            boolean accountCreated = onLimeDB.createAccount(username, encryptedPassword);
            if (accountCreated) {
                new Alert(Alert.AlertType.INFORMATION, "Account created successfully").show();
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to create account").show();
            }
        } catch (NoSuchAlgorithmException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to encrypt password").show();
        }
    }

    /*
    *
    * Hide and Show password text field
    * @param actionEvent
    *
     */
    public void changeVisibility(ActionEvent actionEvent) {
        if (showPassword.isSelected()) {
            // Show password as plain text
            passwordTextField.setText(txtNameP.getText());
            passwordTextField.setVisible(true);
            txtNameP.setVisible(false);
        } else {
            // Hide password (show asterisks)
            txtNameP.setText(passwordTextField.getText());
            txtNameP.setVisible(true);
            passwordTextField.setVisible(false);
        }
    }
}