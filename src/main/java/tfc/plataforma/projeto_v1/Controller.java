package tfc.plataforma.projeto_v1;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class Controller {
    SerialPort port = SerialPort.getCommPorts()[0]; //Encontra o SerialPort do Arduino

    @FXML
    private AnchorPane mainPane;

    /**Função para conectar ao Arduino. É chamada ao carregar no botão "Conectar" na plataforma*/
    @FXML
    private void conectar(){
        port.setComPortParameters(9600, 8, 1, 0);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        if (port.openPort()){
            System.out.println(port.getPortDescription() + " port opened.");
        }else{
            System.out.println("Couldn't open port");
        }
        PacketListener listener = new PacketListener();
        port.addDataListener(listener);

    }

    /**Função para disconectar o Arduino. É chamada através do botão "Disconectar" na plataforma.*/
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
    protected void displayData(String teste){
        System.out.println("Temp: " + teste + " C");
    }

    /**Função para enviar um sinal ao Arduino para que os Estores (servo motor) sejam acionados*/
    @FXML
    protected void acionarEstores(){
        byte[] data = "1".getBytes();
        port.writeBytes(data, data.length);
    }

    /**Função para enviar um sinal ao Arduino para acionar o Buzzer*/
    @FXML
    protected void acionarBuzzer(){
        byte[] data = "2".getBytes();
        port.writeBytes(data, data.length);
    }

    /**
    Função para enviar um sinal ao Arduino para acionar ou desligar o LED
    Se o LED já estiver ligado, este será desligado e vice-versa
    */
    @FXML
    protected void acionarLED(){
        byte[] data = "3".getBytes();
        port.writeBytes(data, data.length);
    }

    /**Invoca a função loadScene para mudar a Scene para a página de alarmes*/
    @FXML
    public void switchToAlarms() {
        loadScene("alarmes.fxml","Alarmes");
    }

    /**Invoca a função loadScene para mudar a Scene para a página de dados*/
    @FXML
    public void switchToList() {
        loadScene("lista.fxml","Dados Armazenados");
    }

    /**Invoca a função loadScene para mudar a Scene para a página de dados*/
    @FXML
    public void switchToHome() {
        loadScene("view.fxml","Home");
    }

    /**Função que muda a Scene a ser mostrada ao utilizador. Recebe um nome de Ficheiro FXML e
     * carrega a página/scene a partir desse ficheiro
     * @param fxmlFileName: nome do ficheiro FXML a ser carregado
     * @param name: nome do ficheiro a ser disponibilizado como título da página
     */
    @FXML
    private void loadScene(String fxmlFileName, String name) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setTitle(name);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}