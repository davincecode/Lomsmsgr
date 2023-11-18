module OnLimeChatter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires java.dotenv;

    opens controller to javafx.fxml;
    opens client to javafx.fxml;

    exports client;
}