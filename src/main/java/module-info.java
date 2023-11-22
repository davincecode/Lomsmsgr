module onLimeChatter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.protobuf;
    requires java.desktop;
    requires javafx.graphics;
    requires java.dotenv;

    opens client;
    opens server;
    opens controller;

    exports client;
    exports server;
    exports controller;
}
