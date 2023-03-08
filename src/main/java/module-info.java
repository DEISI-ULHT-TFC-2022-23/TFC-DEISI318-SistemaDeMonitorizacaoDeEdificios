module tfc.plataforma.projeto_v1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.fazecast.jSerialComm;
    requires java.desktop;

    opens tfc.plataforma.projeto_v1 to javafx.fxml;
    exports tfc.plataforma.projeto_v1;
}