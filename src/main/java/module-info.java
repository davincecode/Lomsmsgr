module com.davincecode.onlime {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.davincecode.onlime to javafx.fxml;
    exports com.davincecode.onlime;
}