int incomingByte;

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
  Serial.print("ON");
  delay(1000);
  Serial.print("OFF");
  delay(1000);
}
