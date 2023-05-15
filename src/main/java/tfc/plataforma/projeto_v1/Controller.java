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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.awt.*;
import java.sql.*;
import java.io.IOException;
import java.util.ArrayList;

public class Controller implements SerialPortDataListener{
    private final SerialPort port = SerialPort.getCommPorts()[0];
    private double temp = 0, lum = 0, humidade = 0, dist = 0;
    private String buffer = "";
    private boolean firstRead = true, connectedToDb = false, connectedToArduino = false;
    private FXMLLoader homePage, alarmsPage, listPage, graphicsPage;
    private Stage stage;
    private  Scene sceneHome, sceneAlarms, sceneList, sceneGraphics;
    private final ArduinoCommands arduino = new ArduinoCommands(port);
    private ArrayList<BuildingData> dadosEdificio = new ArrayList<>();

    /**Função para conectar ao Arduino. É chamada ao carregar no botão "Conectar" na plataforma*/
    @FXML
    public void conectar(){
        port.setComPortParameters(9600, 8, 1, 0);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        if (port.openPort()){
            System.out.println(port.getPortDescription() + " port opened.");
            connectedToArduino = true;
            updateLabel(dispositivo_id, "Conectado", Color.GREEN);
        }else{
            System.out.println("Couldn't open port");
            connectedToArduino = false;
            updateLabel(dispositivo_id, "Desconectado", Color.RED);
        }
        //PacketListener listener = new PacketListener();
        port.addDataListener(this);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
    }

    /**Função para desconectar o Arduino. É chamada através do botão "Disconectar" na plataforma.*/
    @FXML
    private void desconectar(){
        port.disablePortConfiguration();
        if (port.closePort()) {
            System.out.println("Port is closed :)");
            connectedToArduino = false;
            updateLabel(dispositivo_id, "Desconectado", Color.RED);
        } else {
            System.out.println("Failed to close port :(");
        }
    }


    /**Função para conectar-se à base de dados*/
    @FXML
    public void connectToDb() throws SQLException{
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        connectedToDb = true;
        updateList(conn);
        createTable();
        fillGraphics();
    }

    /**Função para atualizar a lista de dados do edificio
     * @param conn: Conexão da Base de Dados
     * */
    public void updateList(Connection conn){
        String sql = "SELECT * FROM dados";
        try (conn;
             Statement stmt  = conn.createStatement();

             ResultSet rs    = stmt.executeQuery(sql)) {


            while (rs.next()) {

                dadosEdificio.add(new BuildingData(rs.getString("id"), rs.getString("data"),
                        Double.parseDouble(rs.getString("temperatura")),Double.parseDouble(rs.getString("luminosidade")),
                        Double.parseDouble(rs.getString("humidade"))));

            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**Função para preencher os gráficos de humidade e temperatura*/
    @FXML
    protected void fillGraphics(){
        XYChart.Series<String, Double> tempSeries = new XYChart.Series<>();
        XYChart.Series<String, Double> humiditySeries = new XYChart.Series<>();

        tempSeries.setName("Temperatura");
        humiditySeries.setName("Humidade");

        for (BuildingData dados: dadosEdificio) {
            tempSeries.getData().add(new XYChart.Data<String,Double>(dados.getData(),
                    Double.parseDouble(dados.getTemp())));
            humiditySeries.getData().add(new XYChart.Data<String,Double>(dados.getData(),
                    Double.parseDouble(dados.getHumidade())));

        }

        temp_graphics.getData().add(tempSeries);
        hum_graphic.getData().add(humiditySeries);
    }

    /**Função para preencher a tabela na página "Lista"*/
    @FXML
    protected void createTable(){
        TableColumn<BuildingData, String> column1 = new TableColumn<>("ID");
        column1.setCellValueFactory(new PropertyValueFactory<>("id"));
        column1.setPrefWidth(50);
        column1.setCellFactory(column -> new TableCell<BuildingData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item);
                }
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<BuildingData, String> column2 = new TableColumn<>("Data");
        column2.setCellValueFactory(new PropertyValueFactory<>("data"));
        column2.setPrefWidth(309);
        column2.setCellFactory(column -> new TableCell<BuildingData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item);
                }
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<BuildingData, String> column3 = new TableColumn<>("Temperatura");
        column3.setCellValueFactory(new PropertyValueFactory<>("temp"));
        column3.setPrefWidth(309);
        column3.setCellFactory(column -> new TableCell<BuildingData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item + "\u00B0C");
                }
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<BuildingData, String> column4 = new TableColumn<>("Humidade");
        column4.setCellValueFactory(new PropertyValueFactory<>("humidade"));
        column4.setPrefWidth(309);
        column4.setCellFactory(column -> new TableCell<BuildingData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(String.format("%.0f%%", Double.parseDouble(item)));
                }
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<BuildingData, String> column5 = new TableColumn<>("Luminosidade");
        column5.setCellValueFactory(new PropertyValueFactory<>("luminosidade"));
        column5.setPrefWidth(309);
        column5.setCellFactory(column -> new TableCell<BuildingData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item);
                }
                setAlignment(Pos.CENTER);
            }
        });


        table.getColumns().add(column1);
        table.getColumns().add(column2);
        table.getColumns().add(column3);
        table.getColumns().add(column4);
        table.getColumns().add(column5);

        fillTable(false);
    }

    /**Função para preencher a tabela com dados
     * @param lastItemOnly: Caso seja True, atualiza apenas o último item adicionado à lista
     * */
    public void fillTable(Boolean lastItemOnly){
        if(lastItemOnly){
            table.getItems().add(dadosEdificio.get(dadosEdificio.size()-1));
        }else{
            for(int i = 0; i < dadosEdificio.size(); i++){
                table.getItems().add(dadosEdificio.get(i));
            }
        }
    }

    /**Função para inserir dados na base de dados*/
    @FXML
    protected void updateDb() throws SQLException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String data = dtf.format(now);
        String sql = "INSERT INTO dados(id,temperatura,humidade,luminosidade,data,user_id) "
                + "VALUES(?,?,?,?,?,?)";
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        try (conn){

             PreparedStatement pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS); {

            pstmt.setString(1, null);
            pstmt.setDouble(2, this.temp);
            pstmt.setDouble(3, this.humidade);
            pstmt.setDouble(4, this.lum);
            pstmt.setString(5, data);
            pstmt.setInt(6, 1);

            if(pstmt.executeUpdate() == 1)
            {
                System.out.println("Row Added");
                dadosEdificio.clear();
                updateList(conn);
                fillTable(true);

            }
        }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**Função para atualizar os dados e as labels após receber novas leituras do Arduino
     * @param dados: string com os dados recebidos*/
    @FXML
    protected void displayData(String dados){
        //Dados são enviados no formato {Tipo}{Valor}; Exemplo: T25.00; T = Temperatura e Valor = 25'
        //Os dados podem ser do tipo T: Temperatura, H: Humidade, L: Luminosidade e D:Distância
        char tipo = 0;
        for(int i = 0; i < dados.length(); i++){
            if(dados.charAt(i) == 'T' || dados.charAt(i) == 'H' || dados.charAt(i) == 'L' || dados.charAt(i) == 'D'){
                //Percorre a String até encontrar um dos tipos
                tipo = dados.charAt(i); //Separa o tipo
                dados = dados.substring(i); //Separa os dados (restante dos valores da String até ;)
            }
        }
        dados = dados.substring(1);
        switch (tipo) {
            case 'T' -> {
                temp = Double.parseDouble(dados);
                updateLabel(temp_id, temp + " °C", Color.BLACK);
            }
            case 'D' -> {
                dist = Double.parseDouble(dados);
            }
            case 'H' -> {
                humidade = Double.parseDouble(dados);
                updateLabel(humidade_id, humidade + "%", Color.BLACK);
            }
            case 'L' -> {
                lum = Double.parseDouble(dados);
                String luminosidade = getLuminosidade(lum);
                if(luminosidade.equals("Baixa")){
                    updateLabel(luminosidade_id, luminosidade, Color.GREEN);
                }else if(luminosidade.equals("Média")){
                    updateLabel(luminosidade_id, luminosidade, Color.YELLOW);
                }else{
                    updateLabel(luminosidade_id, luminosidade, Color.RED);
                }
            }
            default -> System.out.println("Nenhum dado encontrado");
        }

    }

    /**Função para atualizar o texto em um campo de texto de JavaFX
     * @param txtLabel: text label a ser atualizada
     * @param text: novo texto da label
     * @param color: cor do texto da label
     * */
    @FXML
    protected void updateLabel(Text txtLabel, String text, Color color){
        Platform.runLater(() -> {
            txtLabel.setText(text);
            txtLabel.setFill(color);
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
    public void switchToHome() {
        loadScene("view.fxml","Home");
    }

    /**Invoca a função loadScene para mudar a Scene para a página de gráficos*/
    @FXML
    public void switchToGraphics() {
        loadScene("graficos.fxml","Gráficos");
    }

    /**Invoca a função loadScene para mudar a Scene para a página de dados*/
    @FXML
    public void switchToList() throws SQLException {
        loadScene("lista.fxml","Dados Armazenados");
        if(!connectedToDb){
            connectToDb();
        }
    }

    /**Função que muda a Scene a ser mostrada ao utilizador. Recebe um nome de Ficheiro FXML e
     * carrega a página/scene a partir desse ficheiro
     * @param fxmlFileName: nome do ficheiro FXML a ser carregado
     * @param name: nome do ficheiro a ser disponibilizado como título da página
     */
    @FXML
    private void loadScene(String fxmlFileName, String name) {
        FXMLLoader loader = switch (fxmlFileName) { //Definir o ficheiro fxml a carregar
            case "view.fxml" -> homePage;
            case "lista.fxml" -> listPage;
            case "graficos.fxml" -> graphicsPage;
            default -> alarmsPage;
        };

        Scene scene = switch (fxmlFileName) { //Definir a scene correta e disponibilizar
            case "view.fxml" -> sceneHome;
            case "lista.fxml" -> sceneList;
            case "graficos.fxml" -> sceneGraphics;
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

    /**Função para ler o Serial Port*/
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
                }else if(buffer.length() > 9){
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

    /**Funções para funcionamento de alarmes*/

    /**Função para enviar um sinal ao Arduino para que os Estores (servo motor) sejam acionados*/
    @FXML
    public void acionarEstores(){
        arduino.acionarEstores();
    }

    /**Função que envia sinal ao Arduino para acionar o alarme de Temperatura*/
    @FXML
    private void tempAlarmOn(){arduino.tempAlarmOn();}

    /**Função que envia sinal ao Arduino para desligar o alarme de Temperatura*/
    @FXML
    private void tempAlarmOff(){arduino.tempAlarmOff();}

    /**Função que envia sinal ao Arduino para acionar o alarme de Porta*/
    @FXML
    private void doorAlarmOn(){arduino.doorAlarmOn();}

    /**Função que envia sinal ao Arduino para desligar o alarme de Porta*/
    @FXML
    private void doorAlarmOff(){arduino.doorAlarmOff();}

    /**Função que envia sinal ao Arduino para acionar o alarme de Luminosidade*/
    @FXML
    private void lumAlarmOn(){arduino.lumAlarmOn();}

    /**Função que envia sinal ao Arduino para desligar o alarme de Luminosidade*/
    @FXML
    private void lumAlarmOff(){arduino.lumAlarmOff();}

    /**Fim das funções de alarmes*/



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

    public void setGraphicsPage(FXMLLoader loader) throws IOException {
        this.graphicsPage = loader;
        graphicsPage.setController(this);
        this.sceneGraphics = new Scene(graphicsPage.load());
    }
    /**Fim dos setters*/


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
    private Text dispositivo_id;
    @FXML
    private RadioButton tempOn;
    @FXML
    private RadioButton tempOff;
    @FXML
    private RadioButton lumOn;
    @FXML
    private RadioButton lumOff;
    @FXML
    private RadioButton doorOn;
    @FXML
    private RadioButton doorOff;
    @FXML
    private ToggleGroup tempGroup;
    @FXML
    private ToggleGroup lumGroup;
    @FXML
    private TableView table = new TableView();
    @FXML
    private LineChart<String, Double> temp_graphics;
    @FXML
    private LineChart<String, Double> hum_graphic;
}