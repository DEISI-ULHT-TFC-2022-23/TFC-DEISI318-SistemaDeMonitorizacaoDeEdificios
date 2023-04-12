package tfc.plataforma.projeto_v1;

import com.fazecast.jSerialComm.SerialPort;
import javafx.fxml.FXML;

public class ArduinoCommands {
    SerialPort port;

    public ArduinoCommands( SerialPort port){
        this.port = port;
    }

    /**Função para enviar um sinal ao Arduino para que os Estores (servo motor) sejam acionados*/
    public void acionarEstores(){
        byte[] data = String.valueOf(1).getBytes();
        if(port != null){
            port.writeBytes(data, data.length);
        }
    }

    /**Função para enviar um sinal ao Arduino para acionar o Buzzer*/
    public void acionarBuzzer(){
        byte[] data = String.valueOf(2).getBytes();
        port.writeBytes(data, data.length);
    }

    /**
     Função para enviar um sinal ao Arduino para acionar ou desligar o LED
     Se o LED já estiver ligado, este será desligado e vice-versa
     */
    public void acionarLED(){
        byte[] data = String.valueOf(3).getBytes();
        port.writeBytes(data, data.length);
    }

    /**Função que envia sinal ao Arduino para acionar o alarme de Temperatura*/
    private void tempAlarmOn(){
        byte[] data = String.valueOf(8).getBytes();
        if(port != null){
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Temperatura*/
    private void tempAlarmOff(){
        byte[] data = String.valueOf(9).getBytes();
        if(port != null){
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para acionar o alarme de Luminosidade*/
    private void lumAlarmOn(){
        byte[] data = String.valueOf(10).getBytes();
        if(port != null){
            port.writeBytes(data, data.length);
        }
    }

    /**Função que envia sinal ao Arduino para desligar o alarme de Luminosidade*/
    private void lumAlarmOff(){
        byte[] data = String.valueOf(11).getBytes();
        if(port != null){
            port.writeBytes(data, data.length);
        }
    }

}
