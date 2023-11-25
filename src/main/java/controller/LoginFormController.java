package controller;

import database.OnLimeDB;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginFormController {
    public javafx.scene.control.TextField txtName;
    public javafx.scene.control.TextField txtNameP;
    private OnLimeDB onLimeDB;

    public void initialize() {
        onLimeDB = new OnLimeDB();
    }

    public void logInButtonOnAction(ActionEvent actionEvent) throws IOException {
        String username = txtName.getText();
        String password = txtNameP.getText();

        int userId = onLimeDB.getUserId(username);
        if (userId != -1) {
            if (!txtName.getText().isEmpty() && txtName.getText().matches("[A-Za-z0-9]+")) {
                // Create the new stage for the client form
                Stage primaryStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ClientForm.fxml"));

                // Load the FXML file using the FXMLLoader
                Parent root = fxmlLoader.load();

                // Get the controller associated with the FXML file
                ClientFormController controller = fxmlLoader.getController();
                controller.setClientName(txtName.getText());

                // Set up the new stage
                primaryStage.setScene(new Scene(root));
                primaryStage.setTitle(txtName.getText());
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
            new Alert(Alert.AlertType.ERROR, "User does not exist").show();
        }
    }

    public void createAccountOnAction(ActionEvent actionEvent) {
        String username = txtName.getText();
        String password = txtNameP.getText();

        boolean accountCreated = onLimeDB.createAccount(username, password);
        if (accountCreated) {
            new Alert(Alert.AlertType.INFORMATION, "Account created successfully").show();
        } else {
            new Alert(Alert.AlertType.ERROR, "Failed to create account").show();
        }
    }
}