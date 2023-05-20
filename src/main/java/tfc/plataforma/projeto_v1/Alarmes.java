package tfc.plataforma.projeto_v1;

import java.sql.*;

/**Classe para armazenar os alarmes*/
public class Alarmes {
    int alarmeTemp,alarmeLum,alarmePorta;

    public Alarmes(int a1, int a2, int a3){
        alarmeTemp = a1;
        alarmeLum = a2;
        alarmePorta = a3;
    }

    public Alarmes(){}

    public void setAlarmeTemp(int value){alarmeTemp = value;}
    public void setAlarmeLum(int value){alarmeLum = value;}
    public void setAlarmePorta(int value){alarmePorta = value;}

    public int getAlarmeTemp(){return alarmeTemp;}
    public int getAlarmeLum(){return alarmeLum;}
    public int getAlarmePorta(){return alarmePorta;}

    /**Função para atualizar o estado dos alarmes na base de dados
     * @param alarm: alarmes a ser atualizado (pode ser alarme1, alarme2 ou alarme3);
     * 1 = alarme de temperatura; 2 = alarme de luminosidade; 3 = alarme de portas;
     * @param state: estado do alarme; 1 = ligado; 2 = desligado*/
    public void updateAlarm(int alarm, int state) throws SQLException {

        switch (alarm) {
            case 1 -> setAlarmeTemp(state);
            case 2 -> setAlarmeLum(state);
            case 3 -> setAlarmePorta(state);
            default -> {}
        }

        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tfc","root","12151829");
        String sql = "UPDATE utilizadores SET alarme? = ?";

        try (conn){

            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); {

                pstmt.setInt(1, alarm); //Parameter index 1 é o primeiro "?" da query
                pstmt.setInt(2, state);//Segundo "?" da query

                if(pstmt.executeUpdate() == 1)
                {
                    System.out.println("Row Updated");
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
