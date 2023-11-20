package client;

import controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class ClientLauncher extends Application {

    private MainController mainController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainController = new MainController();

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/views/LoginScreen.fxml")));
        String css = Objects.requireNonNull(getClass().getResource("/views/styles.css")).toExternalForm();
        root.getStylesheets().add(css);

        primaryStage.setTitle("OnLime Chatter");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() {
        mainController.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
