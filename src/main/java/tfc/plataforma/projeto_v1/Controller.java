package tfc.plataforma.projeto_v1;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;
import javafx.application.Platform;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.awt.*;
import java.sql.*;
import java.io.IOException;
import java.util.ArrayList;

public class Controller implements SerialPortDataListener{
    private SerialPort port = SerialPort.getCommPorts()[0];
    private double temp = 0;
    private double lum = 0;
    private double humidade = 0;
    private String buffer = "";
    private boolean firstRead = true, connectedToDb = false;
    private FXMLLoader homePage, alarmsPage, listPage;
    private Stage stage;
    private  Scene sceneHome, sceneAlarms, sceneList;
    private ArduinoCommands arduino = new ArduinoCommands(port);

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
        //PacketListener listener = new PacketListener();
        port.addDataListener(this);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
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
    @FXML
    private void connectToDb() throws SQLException{
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        String sql = "SELECT * FROM dados";
        connectedToDb = true;
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

    /**Função para inserir dados na base de dados*/
    @FXML
    protected void updateDb() throws SQLException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String data = dtf.format(now);
        String sql = "INSERT INTO dados(id,temperatura,humidade,luminosidade,data) "
                + "VALUES(?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");

             PreparedStatement pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);) {

            pstmt.setString(1, null);
            pstmt.setDouble(2, this.temp);
            pstmt.setDouble(3, this.humidade);
            pstmt.setDouble(4, this.lum);
            pstmt.setString(5, data);

            if(pstmt.executeUpdate() == 1)
            {
                System.out.println("Row Added");
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**Função para atualizar os dados e as labels após receber novas leituras do Arduino
     * @param dados: lista com os dados recebidos*/
    @FXML
    protected void displayData(String dados){
        char tipo = 0;
        for(int i = 0; i < dados.length(); i++){
            if(dados.charAt(i) == 'T' || dados.charAt(i) == 'H' || dados.charAt(i) == 'L'){
                tipo = dados.charAt(i);
                dados = dados.substring(i);
            }
        }
        dados = dados.substring(1);
        switch (tipo) {
            case 'T' -> {
                System.out.println("Temp " + dados);
                temp = Double.parseDouble(dados);
                updateLabel(temp_id, temp + " °C");
            }
            case 'H' -> {
                System.out.println("Hum " + dados);
                humidade = Double.parseDouble(dados);
                updateLabel(humidade_id, humidade + "%");
            }
            case 'L' -> {
                System.out.println("Lum " + dados);
                lum = Double.parseDouble(dados);
                updateLabel(luminosidade_id, getLuminosidade(lum));
            }
            default -> System.out.println("Nenhum dado encontrado: " + dados);
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

    /**Recebe os dados de luminosidade e retorna se a luminosidade está "Alta", "Baixa" ou "Média" para
     * disponibilizar na plataforma posteriormente
     * @param quantidade: luminosidade obtida pelo sensor
     * @return String com o valor a ser disponibilizado
     */
    @FXML
    public String getLuminosidade(Double quantidade){
        if(quantidade <= 200){
            return "Baixa";
        }else if(quantidade> 201 && quantidade <1000){
            return "Média";
        }else{
            arduino.acionarEstores();
            return "Alta";
        }
    }



    /**Invoca a função loadScene para mudar a Scene para a página de alarmes*/
    @FXML
    public void switchToAlarms() {
        loadScene("alarmes.fxml","Alarmes");
    }

    /**Invoca a função loadScene para mudar a Scene para a página de dados*/
    @FXML
    public void switchToList() throws SQLException {
        loadScene("lista.fxml","Dados Armazenados");
        if(!connectedToDb){
            connectToDb();
        }
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
        FXMLLoader loader = switch (fxmlFileName) {
            case "view.fxml" -> homePage;
            case "lista.fxml" -> listPage;
            default -> alarmsPage;
        };

        Scene scene = switch (fxmlFileName) {
            case "view.fxml" -> sceneHome;
            case "lista.fxml" -> sceneList;
            default -> sceneAlarms;
        };

        stage.setTitle(name);
        stage.setScene(scene);
        stage.show();
    }



    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {

        //displayData(bufferData);

        byte[] newData = event.getReceivedData();
        for(byte i: newData){
            char c = (char) i;
            System.out.print(c);
            if(c==';'){
                //bufferData.add(buffer);
                if(firstRead){
                    firstRead = false;
                    buffer = "";
                }else if(buffer.length() > 8){
                    System.out.println("Erro de leitura do Buffer.");
                }else{
                    displayData(buffer);
                    buffer = "";
                }

            }else{
                buffer += c;
            }
        }
    }

    /**Setters para variáveis da classe Controller*/
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setHomePage(FXMLLoader loader) throws IOException {
        this.homePage = loader;
    }

    public void setSceneHome(Scene scene){
        this.sceneHome = scene;
    }

    public void setAlarmsPage(FXMLLoader loader) throws IOException {
        this.alarmsPage = loader;
        alarmsPage.setController(this);
        this.sceneAlarms = new Scene(alarmsPage.load());
    }
    public void setListPage(FXMLLoader loader) throws IOException {
        this.listPage = loader;
        listPage.setController(this);
        this.sceneList = new Scene(listPage.load());
    }

    /**Variáveis FXML usadas nas páginas*/
    @FXML
    private AnchorPane mainPane;
    @FXML
    private Text temp_id;
    @FXML
    private Text humidade_id;
    @FXML
    private Text luminosidade_id;
    @FXML
    private RadioButton tempOn;
    @FXML
    private RadioButton tempOff;
    @FXML
    private RadioButton lumOn;
    @FXML
    private RadioButton lumOff;
    @FXML
    private ToggleGroup tempGroup;
    @FXML
    private ToggleGroup lumGroup;

}