package tfc.plataforma.projeto_v1;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
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
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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
import java.util.Objects;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Controller implements SerialPortDataListener{
    private final SerialPort port = SerialPort.getCommPorts()[0];
    private double temp = 0,temperaturaMax = 25.0,temperaturaMin = 15.0, lum = 0, humidade = 0, dist = 0;
    private String buffer = "";
    private boolean firstRead = true, connectedToDb = false, popupAtivo = false, coletaAutomatica = false;
    private FXMLLoader homePage, alarmsPage, listPage, graphicsPage;
    private Stage stage;
    private  Scene sceneHome, sceneAlarms, sceneList, sceneGraphics;
    private final ArduinoCommands arduino = new ArduinoCommands(port);
    private ArrayList<BuildingData> dadosEdificio = new ArrayList<>(); //Dados do edifício sem filtro
    private ArrayList<BuildingData> dadosEdificioFiltrados = new ArrayList<>(); //Dados do edifício com filtro
    private Alarmes alarmes = new Alarmes();
    private Timer timer;

    /**Realiza ações para iniciar a aplicação*/
    public void start() throws SQLException {
        this.timer = null;

        conectar();
        connectToDb();
        fillGraphics();

        /*Inicializar Choice Boxes utilizadas nas páginas de gráficos e lista*/
        ano.getItems().addAll("2023");
        ano.setValue("2023");
        anoFiltro.getItems().addAll("2023");

        String[] meses = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        mes.getItems().addAll("Não Especificar");
        mes.getItems().addAll(meses);
        mes.setOnAction(event -> {
            try {
                sortGraphics((String) dia.getValue(),(String) mes.getValue());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        mesFiltro.getItems().addAll("Não Especificar");
        mesFiltro.getItems().addAll(meses);

        dia.getItems().addAll("Não Especificar");
        dia.getItems().addAll(IntStream.rangeClosed(1, 31).mapToObj(String::valueOf).toArray(String[]::new));
        dia.setOnAction(event -> {
            try {
                sortGraphics((String) dia.getValue(),(String) mes.getValue());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        diaFiltro.getItems().addAll("Não Especificar");
        diaFiltro.getItems().addAll(IntStream.rangeClosed(1, 31).mapToObj(String::valueOf).toArray(String[]::new));

    }

    /**Funções para funcionamento do Arduino*/

    /**Função para conectar ao Arduino. É chamada ao carregar no botão "Conectar" na plataforma*/
    @FXML
    public void conectar(){
        port.setComPortParameters(9600, 8, 1, 0);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        if (port.openPort()){
            System.out.println(port.getPortDescription() + " port opened.");
            updateLabel(dispositivo_id, "Conectado", Color.GREEN);
        }else{
            System.out.println("Couldn't open port");
            updateLabel(dispositivo_id, "Desconectado", Color.RED);
        }
        //PacketListener listener = new PacketListener();
        port.addDataListener(this);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
    }

    /**Função para desconectar o Arduino. É chamada através do botão "Desconectar" na plataforma.*/
    @FXML
    private void desconectar(){
        port.disablePortConfiguration();
        if (port.closePort()) {
            System.out.println("Port is closed :)");
            updateLabel(dispositivo_id, "Desconectado", Color.RED);
        } else {
            System.out.println("Failed to close port :(");
        }
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
    /**Fim das funções para funcionamento do Arduino*/

    /**Função para atualizar os dados e as labels após receber novas leituras do Arduino
     * @param dados: string com os dados recebidos*/
    @FXML
    protected void displayData(String dados){
        //Dados são enviados no formato {Tipo}{Valor}; Exemplo: T25.00; T = Temperatura e Valor = 25'
        //Os dados podem ser do tipo T: Temperatura, H: Humidade, L: Luminosidade e D:Distância
        char tipo = 0;
        int alarmeTipo;
        for(int i = 0; i < dados.length(); i++){
            if(dados.charAt(i) == 'T' || dados.charAt(i) == 'H' || dados.charAt(i) == 'L' || dados.charAt(i) == 'D'
            || dados.charAt(i) == 'A'){
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
            case 'A' -> {

                    alarmeTipo = Integer.parseInt(dados);
                    exibirPopupAlarme(alarmeTipo);

            }
            default -> System.out.println("Nenhum dado encontrado");
        }

    }

    /**Exibe um popUp quando um dos alarmes for acionado*/
    private void exibirPopupAlarme(int alarmeTipo) {
        String mensagem = "";
        if(popupAtivo){return;}
        switch (alarmeTipo) {
            case 1:
                if(temp > temperaturaMax){
                    mensagem = "Temperatura acima do limite!";
                }else if(temp < temperaturaMin){
                    mensagem = "Temperatura abaixo do limite!";
                }
                break;
            case 2:
                mensagem = "Luminosidade acima do limite!";
                break;
            case 3:
                mensagem = "Abertura de porta detetada!";
                break;
        }

        // Executar na thread da interface gráfica
        String finalMensagem = mensagem;
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Acionamento de Alarme");
            alert.setHeaderText(null);
            alert.setContentText(finalMensagem);

            // Adicionar botões personalizados
            ButtonType desligarButton = new ButtonType("Desligar Alarme e Fechar");
            ButtonType okButton = new ButtonType("Fechar");
            alert.getButtonTypes().setAll(desligarButton, okButton);

            // Obter o diálogo do Alert
            DialogPane dialogPane = alert.getDialogPane();

            // Obter os botões do Alert
            Button desligarButtonNode = (Button) dialogPane.lookupButton(desligarButton);
            Button okButtonNode = (Button) dialogPane.lookupButton(okButton);

            // Definir a ação para o botão "Fechar e Desligar"
            desligarButtonNode.setOnAction(event -> {
                try {
                    desligarAlarme(alarmeTipo);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                alert.close();
                popupAtivo = false;
            });

            // Fechar o pop-up ao clicar em "OK"
            okButtonNode.setOnAction(event -> {alert.close();popupAtivo=false;});
            popupAtivo = true;
            alert.showAndWait();
        });
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


    /**Função para conectar-se à base de dados*/
    @FXML
    public void connectToDb() throws SQLException{
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        connectedToDb = true;
        updateList(conn);
        createTable();
        getAlarmesFromDB(true);
    }

    /**Função para inserir dados na base de dados. Chamada ao carregar no botão de atualizar dados na
     * página de dados armazenados*/
    @FXML
    protected void atualizarDados() throws SQLException {
        updateDb();
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Sucesso");
            alert.setHeaderText(null);
            alert.setContentText("Dados Armazenados com Sucesso!");

            ButtonType okButton = new ButtonType("Fechar");
            alert.getButtonTypes().setAll(okButton);

            // Obter o diálogo do Alert
            DialogPane dialogPane = alert.getDialogPane();

            Button okButtonNode = (Button) dialogPane.lookupButton(okButton);

            // Fechar o pop-up ao clicar em "OK"
            okButtonNode.setOnAction(event -> {alert.close();popupAtivo=false;});
            popupAtivo = true;
            alert.showAndWait();
        });
    }

    /**Função para atualiar os dados na base de dados.*/
    @FXML
    protected void updateDb() throws SQLException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"); //Define formato para data
        LocalDateTime now = LocalDateTime.now(); //Recolhe a data e hora atual
        String data = dtf.format(now); //Formata para String
        String sql = "INSERT INTO dados(id,temperatura,humidade,luminosidade,data,user_id) "
                + "VALUES(?,?,?,?,?,?)"; //Query para inserir novos dados na DB, cada "?" será um valor da tabela dados
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        try (conn){

            PreparedStatement pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS); {

                pstmt.setString(1, null); //Parameter index 1 é o primeiro "?" da query
                pstmt.setDouble(2, this.temp);//Segundo "?" da query e assim por diante...
                pstmt.setDouble(3, this.humidade);
                pstmt.setDouble(4, this.lum);
                pstmt.setString(5, data);
                pstmt.setInt(6, 1);

                if(pstmt.executeUpdate() == 1)
                {
                    System.out.println("Row Added");
                    dadosEdificio.clear(); //Limpa os dados armazenados
                    updateList(conn); //Preenche a lista com os novos dados atualizados
                    table.getItems().clear();
                    fillTable(false); //Atualiza a tabela apenas com o último valor inserido

                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**Função para recuperar na base de dados os alarmes que estão acionados e desligados
     * @param ativarAlarmes: define se a função ativarAlarmes() vai ser chamada também ou não
     */
    public void getAlarmesFromDB(Boolean ativarAlarmes) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        String sql = "SELECT * FROM utilizadores";

        try (conn;
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) { //Executa a SQL query
            while (rs.next()) { //Para cada dado recebido da query, adiciona à lista
                alarmes.setAlarmeTemp(rs.getInt("alarme1"));
                alarmes.setAlarmeLum(rs.getInt("alarme2"));
                alarmes.setAlarmePorta(rs.getInt("alarme3"));
                coletaAutomatica = rs.getBoolean("coletaAutomatica");
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        if(ativarAlarmes){
            ativarAlarmes();
        }
    }

    /**Função ativar os alarmes de acordo com o que está salvo na base de dados
     */
    public void ativarAlarmes() throws SQLException {

        if(alarmes.getAlarmeTemp() == 1){
            tempAlarmOn();
            tempOn.setSelected(true);
            tempOff.setSelected(false);
        }

        if(alarmes.getAlarmeLum() == 1){
            lumAlarmOn();
            lumOff.setSelected(false);
            lumOn.setSelected(true);
        }

        if(alarmes.getAlarmePorta() == 1){
            doorAlarmOn();
            doorOn.setSelected(true);
            doorOff.setSelected(false);
        }

        if(coletaAutomatica){
            setPeriodo();
            periodoOn.setSelected(true);
            periodoOff.setSelected(false);
        }

    }

    /**Função para filtrar a lista de dados do edificio
     * */
    public void filterList() throws SQLException {
        String[] meses = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        ArrayList<BuildingData> dados = new ArrayList<BuildingData>();
        String dia = (String) diaFiltro.getValue();
        String mes =  (String )mesFiltro.getValue();
        String maxTemp = tempMaxFiltro.getText();
        String minTemp = tempMinFiltro.getText();
        String maxHum = humMaxFiltro.getText();
        String minHum = humMinFiltro.getText();
        String minLum = lumMinFiltro.getText();
        String maxLum = lumMaxFiltro.getText();
        int mesNum = 0;

        // Verificar se as variáveis são nulas e atribuir "" se necessário
        dia = (dia == null || dia.equals("Não Especificar")) ? "" : dia;
        mes = (mes == null || mes.equals("Não Especificar")) ? "" : mes;
        maxTemp = (maxTemp != null) ? maxTemp : "";
        minTemp = (minTemp != null) ? minTemp : "";
        maxHum = (maxHum != null) ? maxHum : "";
        minHum = (minHum != null) ? minHum : "";
        maxLum = (maxLum != null) ? maxLum : "";
        minLum = (minLum != null) ? minLum : "";

        if (!mes.equals("")) {
            for (int i = 0; i < meses.length; i++) {
                if (mes.equals(meses[i])) {
                    mesNum = i + 1;
                }
            }
        }

        // Construir a query SQL
        String query = "SELECT * FROM dados WHERE ";

        if (!dia.isEmpty()) {
            query += "DAY(data) = " + dia + " AND ";
        }
        if (!mes.isEmpty()) {
            query += "MONTH(data) = " + mesNum + " AND ";
        }
        if (!maxTemp.isEmpty()) {
            query += "temperatura < " + maxTemp + " AND ";
        }
        if (!minTemp.isEmpty()) {
            query += "temperatura > " + minTemp + " AND ";
        }
        if (!maxHum.isEmpty()) {
            query += "humidade < " + maxHum + " AND ";
        }
        if (!minHum.isEmpty()) {
            query += "humidade > " + minHum + " AND ";
        }
        if (!maxLum.isEmpty()) {
            query += "luminosidade < " + maxLum + " AND ";
        }
        if (!minLum.isEmpty()) {
            query += "luminosidade > " + minLum + " AND ";
        }

        if (query.endsWith("AND ")) {
            query = query.substring(0, query.length() - 4); // Remove o "AND " do final
        }

        dadosEdificioFiltrados.clear();
        dados = getSortedData(query);
        dadosEdificioFiltrados.addAll(dados);
        table.getItems().clear();
        fillTable(true);

    }

    /**Função para remover os filtros aplicados à tabela
     * */
    @FXML
    protected void removerFiltros(){
        table.getItems().clear();
        dadosEdificioFiltrados.clear();
        diaFiltro.setValue("");
        mesFiltro.setValue("");
        anoFiltro.setValue("");
        tempMaxFiltro.clear();
        tempMinFiltro.clear();
        humMinFiltro.clear();
        humMaxFiltro.clear();
        lumMinFiltro.clear();
        lumMaxFiltro.clear();
        fillTable(false);
    }


    /**Função para atualizar a lista de dados do edificio
     * @param conn: Conexão da Base de Dados
     * */
    public void updateList(Connection conn){
        String sql = "SELECT * FROM dados WHERE user_id = 1";
        try (conn;
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) { //Executa a SQL query
            while (rs.next()) { //Para cada dado recebido da query, adiciona à lista
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
        temp_graphics.getData().clear(); //Remove os dados do gráfico
        hum_graphic.getData().clear();

        XYChart.Series<String, Double> tempSeries = new XYChart.Series<>(); //Inicializa os gráficos
        XYChart.Series<String, Double> humiditySeries = new XYChart.Series<>();

        tempSeries.setName("Temperatura (°C)");
        humiditySeries.setName("Humidade (%)");

        for (BuildingData dados: dadosEdificio) { //Para cada dado armazenado, adiciona à Series

            tempSeries.getData().add(new XYChart.Data<String,Double>(dados.getData(),
                    Double.parseDouble(dados.getTemp())));
            humiditySeries.getData().add(new XYChart.Data<String,Double>(dados.getData(),
                    Double.parseDouble(dados.getHumidade())));

        }

        temp_graphics.getData().add(tempSeries); //Carrega a Series no gráfico
        hum_graphic.getData().add(humiditySeries);

    }

    /**Função para ordenar os gráficos de humidade e temperatura
     * @param dia: dia escolhido para disponibilizar nos gráficos
     * @param mes: mês escolhido para disponibilizar nos gráficos*/
    @FXML
    protected void sortGraphics(String dia, String mes) throws SQLException {
        ArrayList<BuildingData> dados = new ArrayList<BuildingData>();
        String sql = "", data ="";
        String[] meses = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        int mesNum = 0;

        if(mes == null){return;}

        if(dia != null && dia.equals("Não Especificar") && mes.equals("Não Especificar")){
            fillGraphics();
            temp_graphics.getXAxis().setTickLabelsVisible(false);
            hum_graphic.getXAxis().setTickLabelsVisible(false);
            return;
        }

        if (!mes.equals("")) {
            for (int i = 0; i < meses.length; i++) {
                if (mes.equals(meses[i])) {
                    mesNum = i + 1;
                }
            }
            // Recebe uma nova lista com os dados
            if (dia == null || dia.equals("Não Especificar")) {
                sql = "SELECT *" +
                        " FROM dados" +
                        " WHERE MONTH(data) = " + mesNum + " AND YEAR(data) = 2023";
            }else{
                data = "2023-" + mesNum + "-" + dia;
                sql = "SELECT * FROM dados WHERE DATE(data) = '"+data+"'";
            }
            dados = getSortedData(sql); // Recebe uma nova lista com os dados
        }

        temp_graphics.getData().clear(); //Limpa os dados dos gráficos
        hum_graphic.getData().clear();

        XYChart.Series<String, Double> tempSeries = new XYChart.Series<>();
        XYChart.Series<String, Double> humiditySeries = new XYChart.Series<>();

        tempSeries.setName("Temperatura (°C)");
        humiditySeries.setName("Humidade (%)");

        for (BuildingData d: dados) { //Adiciona os novos dados recebidos à Series
            tempSeries.getData().add(new XYChart.Data<String,Double>(d.getData(),
                    Double.parseDouble(d.getTemp())));
            humiditySeries.getData().add(new XYChart.Data<String,Double>(d.getData(),
                    Double.parseDouble(d.getHumidade())));

        }

        temp_graphics.getData().add(tempSeries); //Carrega a Series ao gráfico
        hum_graphic.getData().add(humiditySeries);

        temp_graphics.getXAxis().setTickLabelsVisible(true);
        hum_graphic.getXAxis().setTickLabelsVisible(true);
    }

    /**Função para obter os dados ordenados por mês ou dia
     * @param sql: query a ser executada
     * @return lista com os dados*/
    protected ArrayList<BuildingData> getSortedData(String sql)throws SQLException {
        ArrayList<BuildingData> data = new ArrayList<BuildingData>();
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");

        try (conn; Statement stmt  = conn.createStatement();ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {//Executa a query
                data.add(new BuildingData(rs.getString("id"), rs.getString("data"),
                        Double.parseDouble(rs.getString("temperatura")),
                        Double.parseDouble(rs.getString("luminosidade")),
                        Double.parseDouble(rs.getString("humidade"))));//Armazena os dados na ArrayList
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return data;//Retorna lista com os novos dados
    }

    /**Função para preencher a tabela na página "Lista"*/
    @FXML
    protected void createTable(){
        //Primeiro passo é a criação das colunas da tabela
        TableColumn<BuildingData, String> column1 = new TableColumn<>("ID"); //Nome da coluna
        column1.setCellValueFactory(new PropertyValueFactory<>("id")); //Tipo de dado da coluna
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

        //Após criação das colunas, estas são adicionadas à tabela
        table.getColumns().add(column1);
        table.getColumns().add(column2);
        table.getColumns().add(column3);
        table.getColumns().add(column4);
        table.getColumns().add(column5);

        fillTable(false);//Chama a função para preencher a tabela com os dados do ArrayList dadosEdificio
    }

    /**Função para preencher a tabela com dados
     * @param usarFiltros: Caso seja True, a tabela irá ser preenchida com dados filtrados
     * */
    public void fillTable (Boolean usarFiltros){
        if(!usarFiltros){
            for(int i = dadosEdificio.size()-1; i >= 0; i--){
                table.getItems().add(dadosEdificio.get(i));//Adiciona à tabela todos os dados
            }
        }else{
            for(int i = dadosEdificioFiltrados.size()-1; i >= 0; i--){
                table.getItems().add(dadosEdificioFiltrados.get(i));//Adiciona à tabela todos os dados
            }
        }
    }

    /**
     * Função para desligar coleta automática de dados
     */
    public void setPeriodoOff() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        String sql = "UPDATE utilizadores SET coletaAutomatica = 0";

        coletaAutomatica = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        /*Atualiza base de dados*/
        try (conn){
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); {
                if(pstmt.executeUpdate() == 1)
                {
                    System.out.println("Row Updated");
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Função para estabelecer período de coleta automática de dados
     */
    public void setPeriodo() throws SQLException{
        coletaAutomatica = true;
        timer = new Timer();
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        String sql = "UPDATE utilizadores SET coletaAutomatica = 1";
        Calendar calendar = Calendar.getInstance();

        // Horário 1: 10:00
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTime().after(new Date())) {
            scheduleDataCollection(calendar.getTime(), timer);
        }

        // Horário 2: 14:00
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTime().after(new Date())) {
            scheduleDataCollection(calendar.getTime(), timer);
        }

        // Horário 3: 18:00
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTime().after(new Date())) {
            scheduleDataCollection(calendar.getTime(), timer);
        }

        /*Atualiza base de dados*/
        try (conn){
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); {
                if(pstmt.executeUpdate() == 1)
                {
                    System.out.println("Row Updated");
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void scheduleDataCollection(Date firstExecution, Timer timer) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateDb();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                if (coletaAutomatica) {
                    Calendar nextExecution = Calendar.getInstance();
                    nextExecution.setTime(firstExecution);
                    nextExecution.add(Calendar.DAY_OF_YEAR, 1);
                    scheduleDataCollection(nextExecution.getTime(), timer);
                }
            }
        }, firstExecution);
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
        ordenar.setValue("Todos os dados");
        fillGraphics();
        temp_graphics.getXAxis().setTickLabelsVisible(false);
        hum_graphic.getXAxis().setTickLabelsVisible(false);
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

    /**Função para enviar um sinal ao Arduino para que os Estores (servo motor) sejam acionados*/
    @FXML
    public void acionarEstores(){
        arduino.acionarEstores();
    }

    /**Funções para funcionamento de alarmes*/

    /**Função que envia sinal ao Arduino para acionar o alarme de Temperatura*/
    @FXML
    private void tempAlarmOn() throws SQLException {
        tempOn.setSelected(true);
        tempOff.setSelected(false);
        arduino.tempAlarmOn();
        tempMin.setVisible(true);
        tempMax.setVisible(true);
        confirmTemp.setVisible(true);
        alarmes.updateAlarm(1, 1);
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Temperatura*/
    @FXML
    private void tempAlarmOff()throws SQLException{
        arduino.tempAlarmOff();
        tempMin.setVisible(false);
        tempMax.setVisible(false);
        confirmTemp.setVisible(false);
        tempOff.setSelected(true);
        tempOn.setSelected(false);
        alarmes.updateAlarm(1, 0);
    }

    /**Função que envia sinal ao Arduino para acionar o alarme de Porta*/
    @FXML
    private void doorAlarmOn()throws SQLException{
        doorOn.setSelected(true);
        doorOff.setSelected(false);
        arduino.doorAlarmOn();
        alarmes.updateAlarm(3, 1);
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Porta*/
    @FXML
    private void doorAlarmOff()throws SQLException{
        arduino.doorAlarmOff();
        doorOff.setSelected(true);
        doorOn.setSelected(false);
        alarmes.updateAlarm(3, 0);
    }

    /**Função que envia sinal ao Arduino para acionar o alarme de Luminosidade*/
    @FXML
    private void lumAlarmOn()throws SQLException{
        lumOn.setSelected(true);
        lumOff.setSelected(false);
        arduino.lumAlarmOn();
        alarmes.updateAlarm(2, 1);
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Luminosidade*/
    @FXML
    private void lumAlarmOff()throws SQLException{
        arduino.lumAlarmOff();
        lumOff.setSelected(true);
        lumOn.setSelected(false);
        alarmes.updateAlarm(2, 0);
    }

    /**Função para desligar o alarme passado como parâmetro*/
    private void desligarAlarme(int alarmeTipo) throws SQLException {
        switch (alarmeTipo) {
            case 1 -> tempAlarmOff();
            case 2 -> lumAlarmOff();
            case 3 -> doorAlarmOff();
        }
    }

    /**Função que envia as temperaturas mínimas e máximas colocadas pelo utilizador*/
    @FXML
    private void enviarTemperaturas(){
        String max = tempMax.getText();
        String min = tempMin.getText();

        if(max != null && !max.equals("")){
            arduino.tempMax(Integer.parseInt(max));
            temperaturaMax = Double.parseDouble(max);
            tempMax.clear();
        }

        if(min != null && !min.equals("")){
            arduino.tempMin(Integer.parseInt(min));
            temperaturaMin = Double.parseDouble(min);
            tempMin.clear();
        }
    }

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
    private AnchorPane popup;
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
    @FXML
    private TextField tempMin;
    @FXML
    private TextField tempMax;
    @FXML
    private TextField tempMinFiltro;
    @FXML
    private TextField tempMaxFiltro;
    @FXML
    private TextField humMinFiltro;
    @FXML
    private TextField humMaxFiltro;
    @FXML
    private TextField lumMinFiltro;
    @FXML
    private TextField lumMaxFiltro;
    @FXML
    private Button confirmTemp;
    @FXML
    private Button aplicarFiltros;
    @FXML
    private Button removerFiltros;
    @FXML
    private ChoiceBox ordenar;
    @FXML
    private ChoiceBox dia;
    @FXML
    private ChoiceBox mes;
    @FXML
    private ChoiceBox ano;
    @FXML
    private ChoiceBox diaFiltro;
    @FXML
    private ChoiceBox mesFiltro;
    @FXML
    private ChoiceBox anoFiltro;

    @FXML
    private RadioButton periodoOn;
    @FXML
    private RadioButton periodoOff;
    @FXML
    private ToggleGroup periodoGroup;
}