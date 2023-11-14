package com.davincecode.onlime;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 *
 * Author Vincent Ybanez;
 * StudentID: 239445310;
 * Subject: COSC3406 - SE;
 * Created: November 14, 2023;
 *
 */

public class MainApplication extends Application {

    private MainController mainController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainController = new MainController();

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("main-view.fxml")));
        String css = Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm();
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
