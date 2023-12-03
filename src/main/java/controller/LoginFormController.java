package controller;

import database.OnLimeDB;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    private void showErrorMessage(String message) {
        Platform.runLater(() -> {
            try {
                // Create a custom dialog
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Error");
                dialog.setHeaderText(null);
                dialog.setContentText(message);

                // Set up the dialog content
                Label contentLabel = new Label(message);
                contentLabel.setStyle("-fx-alignment: center;");
                dialog.getDialogPane().setContent(contentLabel);

                // Set width and height
                dialog.getDialogPane().setPrefWidth(300);
                dialog.getDialogPane().setPrefHeight(100);

                // Add OK button to the dialog
                ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().add(okButton);

                // Set custom styling for the dialog
                DialogPane dialogPane = dialog.getDialogPane();
                // dialogPane.getStylesheets().add(getClass().getResource("../view/styles.css").toExternalForm());

                // Show the dialog and wait for user input
                dialog.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
                        // Set the client's username
                        controller.setClientName(username);

                        // Set up the new stage
                        primaryStage.setScene(new Scene(root));
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
                        showErrorMessage("Please enter your name");
                    }
                } else {
                    // Password does not match, show an error message
                    showErrorMessage("Incorrect password");
                }
            } catch (NoSuchAlgorithmException e) {
                showErrorMessage("Failed to encrypt password");
            }
        } else {
            showErrorMessage("User does not exist");
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
                showErrorMessage("Oops! The username " + username + " is already in use." + "\n" + "Please try another one.");
            }
        } catch (NoSuchAlgorithmException e) {
            showErrorMessage("Failed to encrypt password");
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