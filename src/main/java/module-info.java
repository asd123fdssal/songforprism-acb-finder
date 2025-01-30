module com.arsud.acbfinder {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens com.arsud.acbfinder to javafx.fxml;
    exports com.arsud.acbfinder;
}