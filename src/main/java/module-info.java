module ProcessApplication {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.arsud.acbfinder to javafx.fxml;
    exports com.arsud.acbfinder;
}
