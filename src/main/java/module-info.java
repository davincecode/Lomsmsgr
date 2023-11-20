module OnLimeChatter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.protobuf;
    requires java.desktop;
    requires javafx.graphics;
    requires java.dotenv;

    exports server;
    exports controller;
}
