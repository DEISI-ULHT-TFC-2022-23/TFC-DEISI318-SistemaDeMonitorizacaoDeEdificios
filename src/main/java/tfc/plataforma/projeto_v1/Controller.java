package tfc.plataforma.projeto_v1;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;

public class Controller implements SerialPortDataListener{
    SerialPort port = SerialPort.getCommPorts()[0]; //Encontra o SerialPort do Arduino

    @FXML
    private AnchorPane mainPane;

    /*Invoca a função loadScene para mudar a Scene para a página de alarmes*/
    @FXML
    public void switchToAlarms() {
        loadScene("alarmes.fxml");
    }

    /*Função que muda a Scene a ser mostrada ao utilizador. Recebe um nome de Ficheiro FXML e
    * carrega a página/scene a partir desse ficheiro
    */
    @FXML
    private void loadScene(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private Label temp_label;

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }


    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] newData = event.getReceivedData();
        //System.out.print(new String(newData));
        displayData(new String(newData));
    }

    /*Função para conectar ao Arduino. É chamada ao carregar no botão "Conectar" na plataforma*/
    @FXML
    private void conectar(){
        port.setComPortParameters(9600, 8, 1, 0); // default connection settings for Arduino
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0); // block until bytes can be written
        if (port.openPort()){
            System.out.println(port.getPortDescription() + " port opened.");
        }else{
            System.out.println("Couldn't open port");
        }

        port.addDataListener(this);


    }

    /*Função para disconectar o Arduino. É chamada através do botão "Disconectar" na plataforma.*/
    @FXML
    private void disconectar(){
        port.disablePortConfiguration();
        if (port.closePort()) {
            System.out.println("Port is closed :)");

        } else {
            System.out.println("Failed to close port :(");
        }
    }

    @FXML
    protected void onConnectButtonClick() {
        conectar();
    }

    @FXML
    protected void onDisconnectButtonClick() {
        disconectar();
    }

    @FXML
    protected void displayData(String teste){
        System.out.println(teste);
        String[] data = teste.split(";",0);
        System.out.println("Temp: " + data[0]);
        Platform.runLater(
                () -> {

                    temp_label.setText("data[0]");
                }
        );
    }

}