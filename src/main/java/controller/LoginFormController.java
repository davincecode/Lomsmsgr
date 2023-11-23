package controller;

import database.OnLimeDB;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.Server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class LoginFormController {
    public TextField txtName;
    public PasswordField txtPassword;
    private OnLimeDB databaseConnector = new OnLimeDB();
    private Encryptor encryptor = new Encryptor();
    private Server server;
    private String loggedInUser;

    public void initialize(){
        try {
            server = Server.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logInButtonOnAction(ActionEvent actionEvent) throws IOException {
        String username = txtName.getText();
        if (!username.isEmpty() && username.matches("[A-Za-z0-9]+")) {
            String usernameFromDB = databaseConnector.getUsername(username);
            if (usernameFromDB != null) {
                Stage primaryStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ClientForm.fxml"));

                ClientFormController controller = new ClientFormController();
                controller.setClientName(usernameFromDB); // set the client name
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

                // Notify the server that the user has logged in
                server.userLoggedIn(usernameFromDB);

                // Store the logged-in user's username
                loggedInUser = usernameFromDB;
            } else {
                new Alert(Alert.AlertType.ERROR, "Username not found in the database").show();
            }
        } else {
            new Alert(Alert.AlertType.ERROR, "Please enter a valid username").show();
        }
    }

    public void logOutButtonOnAction(ActionEvent actionEvent) {
        // Notify the server that the user has logged out
        server.userLoggedOut(loggedInUser);

        // Close the application
        ((Stage) txtName.getScene().getWindow()).close();
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

    public String getLoggedInUser() {
        return loggedInUser;
    }
}
