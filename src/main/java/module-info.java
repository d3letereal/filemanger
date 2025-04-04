module com.example.filemanger {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    requires com.almasb.fxgl.all;

    opens com.example.filemanger to javafx.fxml;
    exports com.example.filemanger;
    exports com.example.filemanager;
    opens com.example.filemanager to javafx.fxml;
}