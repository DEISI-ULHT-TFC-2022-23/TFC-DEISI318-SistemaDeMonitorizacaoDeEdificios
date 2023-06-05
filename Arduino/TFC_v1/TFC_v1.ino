#include <Wire.h> 
#include <Servo.h>
#include <DHT.h> //Para leitura do sensor de humidade DHT 22
#include <Adafruit_BMP085.h> //Biblioteca para leitura do sensor BMP180
 
Adafruit_BMP085 bmp; //Objeto do tipo Adafruit_BMP085 para leitura do sensor BMP180
float temperatura = 0; //Temperatura aqdquirida pelo sensor BMP180
int tempMax = 25; int tempMin = 15; //Temperaturas enviadas pelo utiliador

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
unsigned int distance; //Para medições do sensor ultrassônico

bool tempAlarm = false; //Determina se o alarme de temperatura está ativo ou não
bool lumAlarm = false; //Determina se o alarme de luminosidade está ativo ou não
bool portaAlarm = false; //Determina se o alarme de abertura de porta está ativo ou não



void setup(){
  Serial.begin(9600); 
  bmp.begin(); //Inicializa sensor BMP180 
  dht.begin();//Inicializa sensor DHT
  pinMode(redLed_pin, OUTPUT); //LED em modo output
  pinMode(photoresistor_pin, INPUT); //Photoresistor em modo Input
  pinMode(buzzer_pin, OUTPUT); //Buzzer em modo Output
  servo.attach(servo_pin); //Anexa o pin do servo ao objeto
  pinMode(triggerPin, OUTPUT); // Pin trigger em modo Output
  pinMode(echoPin, INPUT); //Pin echo em modo input
}
   
void loop() {
  readSensors();

  if (Serial.available()) {
    String mensagem = Serial.readStringUntil('\n');
    int index = mensagem.indexOf(':');
    if (index != -1) {
      int comando = mensagem.substring(0, index).toInt();
      int valor = mensagem.substring(index + 1).toInt();
      switch (comando) {
        case 1: // Acionar servo
          startServo();
          break;
        case 2: // Acionar Buzzer
          startBuzzer();
          break;
        case 3: // Acionar led
          redLedToggle();
          break;
        case 4: // Ler temperatura
          readTemp();
          break;
        case 5: // Ler luminosidade
          readPhotoresistor();
          break;
        case 6: // Ler humidade
          readHumidity();
          break;
        case 7: // Ler sensor ultrassônico
          readUltrasonic();
          break;
        case 8: // Ativar alarme de Temp
          tempAlarm = true;
          break;
        case 9: // Desligar alarme de Temp
          tempAlarm = false;
          ledOff();
          break;
        case 10: // Ativar alarme de Luminosidade
          lumAlarm = true;
          break;
        case 11: // Desligar alarme de Luminosidade
          lumAlarm = false;
          ledOff();
          break;
        case 12: // Ativar LED
          ledOn();
          break;
        case 13: // Desligar LED
          ledOff();
          break;
        case 14: // Ligar alarme de portas
          portaAlarm = true;
          break;
        case 15: // Desligar alarme de portas
          portaAlarm = false;
          ledOff();
          break;
        case 16: // Receber temperatura máxima
          tempMax = valor;
          break;
        case 17: // Receber temperatura mínima
          tempMin = valor;
          break;
        case 18: // Ativar alarme
          alarm();
          break;
        default:
          break;
      }
    }
  }
  
  if (tempAlarm) {
    checkTempAlarm();
  }

  if (lumAlarm) {
    checkLumAlarm();
  }

  if (portaAlarm) {
    checkDoorAlarm();
  }    

  delay(3000);
}


//Função para ler todos os sensores
void readSensors(){
  readHumidity();
  delay(500);
  readPhotoresistor();
  delay(500);
  readTemp();
  delay(500);
  //readUltrasonic();
}

//Função para ler o Photoresistor 
void readPhotoresistor(){
  photoresistor_value = analogRead(photoresistor_pin); //Armazena o valor na variável 
  Serial.print("L");
  Serial.print(photoresistor_value);
  Serial.println(";");
}

//Função para ler o sensor de temperatura BMP180
void readTemp(){
  temperatura = bmp.readTemperature(); //Armazena o valor na variável temperatura
  Serial.print("T");
  Serial.print(temperatura);
  Serial.println(";");
}

//Função para ligar o LED vermelho
void ledOn(){
  digitalWrite(redLed_pin, HIGH);   
  redLed_state = 1;   
}

//Função para ligar o LED e o Buzzer
void alarm(){
  ledOn();
  startBuzzer();
}

//Função para desligar o LED vermelho
void ledOff(){
  digitalWrite(redLed_pin, LOW);
  redLed_state = 0;  
}

//Função para ligar ou desligar o LED vermelho
void redLedToggle(){

  if(redLed_state == 0){
    ledOn(); 
  }else{
    ledOff(); 
  }
	

}

//Verifica se a temperatura está acima ou abaixo dos limites
void checkTempAlarm(){
  if(temperatura > tempMax){
    alarm();
    Serial.print("A1");
    Serial.println(";");
  }
  else if(temperatura < tempMin){
    alarm();
    Serial.print("A1");
    Serial.println(";");
  }
  else if(redLed_state == 1){
    ledOff();
  }
}

//Verifica se a luminosidade está acima dos limites
void checkLumAlarm(){
  if(photoresistor_value > 1000){
    alarm();
    Serial.print("A2");
    Serial.println(";");
  }else if(redLed_state == 1){
    ledOff();
  }
}

void checkDoorAlarm(){
    readUltrasonic();

    if(distance <= 5){
      alarm();
      Serial.print("A3");
      Serial.println(";");
    }else if(redLed_state == 1){
    ledOff();
  }
}

//Função para ler sensor de humidade
void readHumidity(){
    float hum = dht.readHumidity();
    while(isnan(hum)){
      hum = dht.readHumidity();
    }

    humidity_value = hum;
    Serial.print("H");
    Serial.print(humidity_value); 
    Serial.println(";");

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

  long duration = pulseIn(echoPin, HIGH);
  distance = duration / 58; //Converter para CM

  Serial.print("D");
  Serial.print(distance);
  Serial.println(";");

}