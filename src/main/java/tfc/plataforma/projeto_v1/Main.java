package tfc.plataforma.projeto_v1;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.*;

import java.awt.*;
import java.io.IOException;

public class Main extends Application{
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view.fxml"));
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth() ;
        double height = screenSize.getHeight() ;

        try {
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            stage.setTitle("Dashboard");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void start(Stage stage, String page) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(page));
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth() ;
        double height = screenSize.getHeight() ;

        try {
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            //stage.setTitle("Dashboard");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch();
    }
}
