#include <Wire.h> 
#include <Servo.h>
#include <DHT.h> //Para leitura do sensor de humidade DHT 22
#include <Adafruit_BMP085.h> //Biblioteca para leitura do sensor BMP180
 
Adafruit_BMP085 bmp; //Objeto do tipo Adafruit_BMP085 para leitura do sensor BMP180
float temperatura = 0; //Temperatura aqdquirida pelo sensor BMP180

int photoresistor_pin = A0; // Pin do Photoresistor
float photoresistor_value = 0; //Valor da leitura do photoresistor

int redLed_pin = 7; //Pin do led Vermelho
int redLed_state = 0; //Estado do LED; 0 = desligado e 1 = ligado

int buzzer_pin = 6; //Pin do buzzer
int servo_pin = 8; //Pin do servo motor utilizado para simular estores

DHT dht(2,DHT22); //Inicializar sensor de humidade DHT22
float humidity_value = 0; //Valor da leitura do sensor de humidade

Servo servo; //Objeto do tipo servo
int pos_inicial = 0;//Posição inicial do servo

int triggerPin = 10; // Pin do trigger do sensor ultrassônico
int echoPin = 9; //Pin do echo do sensor ultrassônico
long duration, distance; //Para medições do sensor ultrassônico


void setup(){
  Serial.begin(9600); 
  bmp.begin(); //Inicializa sensor BMP180 
  pinMode(redLed_pin, OUTPUT); //LED em modo output
  pinMode(photoresistor_pin, INPUT); //Photoresistor em modo Input
  pinMode(buzzer_pin, OUTPUT); //Buzzer em modo Output
  servo.attach(servo_pin); //Anexa o pin do servo ao objeto
  pinMode(triggerPin, OUTPUT); // Pin trigger em modo Output
  pinMode(echoPin, INPUT); //Pin echo em modo input
}
   
void loop(){
    readSensors();

    if (Serial.available()) {
      char msg = Serial.read();
      if (msg == '1') { //Acionar servo
        startServo();
        //Serial.println("Servo Acionado");
      }else if(msg == '2'){//Acionar Buzzer
        startBuzzer();
        //Serial.println("Buzzer Acionado");        
      }else if(msg == '3'){//Acionar led
        redLedToggle();
        //Serial.println("LED Acionado");        
      }else if(msg == '4'){//Ler temperatura
        readTemp();
      }else if(msg == '5'){//Ler luminosidade
        readPhotoresistor();
      }else if(msg == '6'){//Ler humidade
        readHumidity();
      }else if(msg == '7'){//Ler sensor ultrassônico
        readUltrasonic();
      }
    }
    
    //Serial.print("Temperatura: ");
    //Serial.print(temperatura); 
    /*Serial.print("°C");
    Serial.println();
    Serial.print("Humidade: ");
    Serial.print(humidity_value); 
    Serial.print("%");
    Serial.println();
    Serial.print("Luminosidade: ");
    Serial.print(photoresistor_value); */
    Serial.begin(9600);
	  delay(5000);
}

//Função para ler todos os sensores
void readSensors(){
  readTemp();
  delay(500);
  readPhotoresistor();
  delay(500);
  //readHumidity();
  //readUltrasonic();
}

//Função para ler o Photoresistor 
void readPhotoresistor(){
  photoresistor_value = analogRead(photoresistor_pin); //Armazena o valor na variável 
  Serial.println(photoresistor_value);
}

//Função para ler o sensor de temperatura BMP180
void readTemp(){
  temperatura = bmp.readTemperature(); //Armazena o valor na variável temperatura
  Serial.println(temperatura);
}

//Função para ligar ou desligar o LED vermelho
void redLedToggle(){

  if(redLed_state == 0){
    digitalWrite(redLed_pin, HIGH);   
    redLed_state = 1; 
  }else{
    digitalWrite(redLed_pin, LOW);
    redLed_state = 0;
  }
	

}

//Função para ler sensor de humidade
void readHumidity(){
    float temp = dht.readHumidity();
    while(isnan(temp)){
      temp = dht.readHumidity();
    }

    humidity_value = temp;
    Serial.println(humidity_value); 

}

//Função para ativar o Buzzer
void startBuzzer(){
  tone(buzzer_pin, 50);
  delay(1000);
  noTone(buzzer_pin);
  delay(1000);
}

//Função para acionar o motor servo
void startServo(){
  for (pos_inicial = 0; pos_inicial <= 180; pos_inicial += 1) { 
    servo.write(pos_inicial);              
    delay(15);                       
  }
  for (pos_inicial = 180; pos_inicial >= 0; pos_inicial -= 1) { 
    servo.write(pos_inicial);              
    delay(15);                       
  }
}

//Função para ler o sensor de distância ultrassônico
void readUltrasonic(){
  digitalWrite(triggerPin, LOW);
  delayMicroseconds(2);
  digitalWrite(triggerPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(triggerPin, LOW);

  pinMode(echoPin, INPUT);
  duration = pulseIn(echoPin, HIGH);  

  distance = (duration/2) / 29.1; //Distância em centímetros

    
  Serial.print("Distância: ");
  Serial.print(distance);
  Serial.println(" cm");
}