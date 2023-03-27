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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.*;
import java.io.IOException;
import java.util.ArrayList;

public class Controller implements SerialPortDataListener{
    SerialPort port;
    double temp = 0;
    double lum = 0;
    String buffer = "";
    ArrayList<String> bufferData = new ArrayList<String>();

    /**Função para conectar ao Arduino. É chamada ao carregar no botão "Conectar" na plataforma*/
    @FXML
    private void conectar() throws SQLException{
        port = SerialPort.getCommPorts()[0];
        port.setComPortParameters(9600, 8, 1, 0);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        if (port.openPort()){
            System.out.println(port.getPortDescription() + " port opened.");
        }else{
            System.out.println("Couldn't open port");
        }
        //PacketListener listener = new PacketListener();
        port.addDataListener(this);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
        connectToDb();
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

    /**Função para conectar-se à base de dados*/
    private void connectToDb() throws SQLException{
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        String sql = "SELECT * FROM dados";

        try (conn;
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            /*
            while (rs.next()) {
                System.out.println(rs.getString("data") + "\t" +
                        rs.getString("temperatura")  + "\t" +
                        rs.getString("luminosidade"));

            }*/
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }


    @FXML
    protected void displayData(ArrayList<String> dados){
        if(dados.size() >= 2){
            System.out.println("Temp " + dados.get(0));
            temp = Double.parseDouble(dados.get(0));
            updateLabel(temp_id,dados.get(0) + "");

            System.out.println("Lum " + dados.get(1));
            lum = Double.parseDouble(dados.get(1));
            //luminosidade_id.setText("Alta");

            dados.removeAll(dados);
        }
    }

    /**Função para atualizar o texto em um campo de texto de JavaFX
     * @param txtLabel: text label a ser atualizada
     * @param text: novo texto da label
     * */
    @FXML
    protected void updateLabel(Text txtLabel, String text){
        Platform.runLater(() -> {
            txtLabel.setText(text);
        });
    }

    @FXML
    protected void displayLuminosidade(String quantidade){
        int number = Integer.parseInt(quantidade);

        if(number <= 200){
            luminosidade_id.setText("Baixa");
        }else if(number> 200 && number <1000){
            luminosidade_id.setText("Média");
        }else{
            luminosidade_id.setText("Alta");
        }

    }

    /**Função para enviar um sinal ao Arduino para que os Estores (servo motor) sejam acionados*/
    @FXML
    protected void acionarEstores(){
        byte[] data = "4".getBytes();
        if(port != null){
            port.writeBytes(data, data.length);
        }
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

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {

        displayData(bufferData);

        byte[] newData = event.getReceivedData();
        for(byte i: newData){
            char c = (char) i;
            if(c=='\n'){
                bufferData.add(buffer);
                buffer = "";
            }else{
                buffer += c;
            }
        }
        /*byte[] buffer = new byte[event.getSerialPort().bytesAvailable()];
        event.getSerialPort().readBytes(buffer, buffer.length);

        try {
            String data = new String(buffer);
            double temp = Double.parseDouble(data.trim());
            if(temp != 0){
                temperature = temp;
            }
        } catch (NumberFormatException e) {

        }

        // Update the temperature label on the JavaFX Application Thread
        Platform.runLater(() -> {
            temp_id.setText(temperature + "°C");
        });*/

    }
    @FXML
    private AnchorPane mainPane;
    @FXML
    private Text temp_id;
    @FXML
    private Text humidade_id;
    @FXML
    private Text luminosidade_id;
    @FXML
    private Text grausCelcius;
}