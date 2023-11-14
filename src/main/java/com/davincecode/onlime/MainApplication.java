package com.davincecode.onlime;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    private MainController mainController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainController = new MainController();

        Parent root = FXMLLoader.load(getClass().getResource("main-view.fxml"));
        String css = getClass().getResource("styles.css").toExternalForm();
        root.getStylesheets().add(css);

        primaryStage.setTitle("OnLime Chatter");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        mainController.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
