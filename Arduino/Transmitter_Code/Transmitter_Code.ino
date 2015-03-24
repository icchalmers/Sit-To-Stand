#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include "utility/Adafruit_PWMServoDriver.h"

// Create the motor shield object with the default I2C address
Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 

// Select which 'port' M1, M2, M3 or M4. In this case, M1
Adafruit_DCMotor *myMotor = AFMS.getMotor(1);


char incomingByte;
const int ledPin = 13; // the pin that the LED is attached to
const int buffAngle = 10; //angle allowed before vibration starts
const int strength = 5; //multiplier for ramping up speed when vibrating


//Used to clear buffer in case of multiple input
void serialFlush(){
  while(Serial.available() > 0) {
    char t = Serial.read();
  }
} 

void setup()
{
    // initialize serial communication:
  Serial.begin(9600);
  // initialize the LED pin as an output:
  pinMode(ledPin, OUTPUT);
  
  AFMS.begin();  // create with the default frequency 1.6KHz
  //AFMS.begin(1000);  // OR with a different frequency, say 1KHz
  
  // Set the speed to start, from 0 (off) to 255 (max speed)
  myMotor->setSpeed(150);
  myMotor->run(FORWARD);
  // turn on motor
  myMotor->run(RELEASE);
}

void loop()
{
  String content = "";
  while(Serial.available()>0){
    incomingByte = Serial.read();
    delay(2);
    content+= incomingByte;
  }
  Serial.print(content);
  if(content.length()>0){
  Serial.print("\n");
  if(content == "HIGH"){
    digitalWrite(ledPin,HIGH);
    myMotor->run(FORWARD);
  }
  else if(content =="LOW") {
    digitalWrite(ledPin, LOW);
    myMotor->run(RELEASE);
  }
  else{
      if(content.charAt(0)=='L'){
        String num = content.substring(1);
        //Convert string to int and take away buffer angle
      int s = num.toInt()-buffAngle;
      //Serial.print(s);
      //Multiply speed by vibration magnitude
      s = s*strength;
      if(s<0){
        myMotor->run(RELEASE);
      }
      else{
        myMotor->setSpeed(s);
        myMotor->run(FORWARD);
      }
  }
  else{
    myMotor->run(RELEASE);
  }
  }
}
content = "";
}

