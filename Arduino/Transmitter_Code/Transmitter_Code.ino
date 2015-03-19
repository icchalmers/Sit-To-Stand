char incomingByte;
const int ledPin = 13; // the pin that the LED is attached to

//Used to clear buffer in case of multiple input
void serialFlush(){
  while(Serial.available() > 0) {
    char t = Serial.read();
  }
} 

void setup()
{
  Serial.begin(9600);
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
  if(content == "High"){
    digitalWrite(ledPin,HIGH);
  }
  
  if (content == "Low") {
    digitalWrite(ledPin, LOW);
  }
}
