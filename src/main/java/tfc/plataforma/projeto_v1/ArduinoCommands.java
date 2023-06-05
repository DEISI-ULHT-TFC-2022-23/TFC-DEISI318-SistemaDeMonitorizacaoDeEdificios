package tfc.plataforma.projeto_v1;

import com.fazecast.jSerialComm.SerialPort;
import javafx.fxml.FXML;

/**Classe para enviar commandos ao Arduino*/
public class ArduinoCommands {
    SerialPort port;

    public ArduinoCommands( SerialPort port){
        this.port = port;
    }

    /**Função para enviar um sinal ao Arduino para que os Estores (servo motor) sejam acionados*/
    public void acionarEstores(){
        if(port != null){
            String command = "1:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função para enviar um sinal ao Arduino para acionar o Buzzer*/
    public void acionarBuzzer(){
        if(port != null){
            String command = "2:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**
     Função para enviar um sinal ao Arduino para acionar ou desligar o LED
     Se o LED já estiver ligado, este será desligado e vice-versa
     */
    public void toggleLED(){
        if(port != null){
            String command = "3:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**
     Função para enviar um sinal ao Arduino para acionar LED
     */
    public void acionarLED(){
        if(port != null){
            String command = "12:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**
     Função para enviar um sinal ao Arduino para desligar LED
     */
    public void desligarLED(){
        if(port != null){
            String command = "13:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para acionar o alarme de Temperatura*/
    public void tempAlarmOn(){
        if(port != null){
            String command = "8:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Temperatura*/
    public void tempAlarmOff(){
        if(port != null){
            String command = "9:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para acionar o alarme de Luminosidade*/
    public void lumAlarmOn(){
        if(port != null){
            String command = "10:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Luminosidade*/
    public void lumAlarmOff(){
        if(port != null){
            String command = "11:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Porta*/
    public void doorAlarmOff(){
        if(port != null){
            String command = "15:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para ligar o alarme de Porta*/
    public void doorAlarmOn(){
        if(port != null){
            String command = "14:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /** Função que enviar temperatura máxima */
    public void tempMax(int temp) {
        if (port != null) {
            String command = "16:" + temp + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }



    /**Função que enviar temperatura mínima*/
    public void tempMin(int temp){
        if (port != null) {
            String command = "17:" + temp + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para ligar o alarme genérico (aciona buzzer e led)*/
    public void alarm(){
        if(port != null){
            String command = "18:" + "\n";
            byte[] data = command.getBytes();
            port.writeBytes(data, data.length);
        }
    }

}
