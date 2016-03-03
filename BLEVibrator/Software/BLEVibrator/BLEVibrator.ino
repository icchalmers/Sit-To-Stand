/************************************************************************
* FILENAME : BLEVibrator.ino
*
* DESCRIPTION :
*     RFDuino sketch to drive a vibration motor
* 
* Usage : 
*     
* 
* AUTHOR : Iain Chalmers
*
* START DATE : 3rd March 2016
*
* Copyright Iain Chalmers 2016. All rights reserved.
*
* CHANGES :
*     
*/

#include <RFduinoBLE.h>

// Module variables - useful for quick changes between boards
#define MODULE_NAME "Vib001" // Must be a max of 15 bytes long
#define ADVERTISEMENT_INTERVAL 500 // in milliseconds
#define TX_POWER_LEVEL 0  // -20dbM to +4 dBm. Must be a multiple of 4
#define MINIMUM_ON_VALUE 15 // the motor used needs a value of at least 15
                            // before it will turn on

// Pin definitions
#define LED_ADVERTISING 3 // The red LED on the PCB
#define LED_CONNECTED 2 // The green LED on the PCB
#define MOTOR_SIGNAL 4 // PWM signal to drive the vibration motor

// LEDs are active low. 
#define LEDON LOW
#define LEDOFF HIGH

void setup() {
  // Initialise the output pins
  digitalWrite(LED_ADVERTISING, LEDOFF);
  digitalWrite(LED_CONNECTED, LEDOFF);
  analogWrite(MOTOR_SIGNAL, 0);
  
  // Set the pin directions
  pinMode(LED_ADVERTISING, OUTPUT);
  pinMode(LED_CONNECTED, OUTPUT);
  pinMode(MOTOR_SIGNAL, OUTPUT);

  // Setup the BLE stack
  RFduinoBLE.deviceName = MODULE_NAME;
  RFduinoBLE.advertisementInterval = MILLISECONDS(ADVERTISEMENT_INTERVAL);
  RFduinoBLE.txPowerLevel = TX_POWER_LEVEL;

  // start the BLE stack
  RFduinoBLE.begin();
}

void loop() {
  // Nothing to do, so enter low power mode
  RFduino_ULPDelay(INFINITE);
}

void RFduinoBLE_onAdvertisement(bool start)
{
  // turn the red led on if we start advertisement, and turn it
  // off if we stop advertisement
  if (start){
    digitalWrite(LED_ADVERTISING, LEDON);
  }
  else{
    digitalWrite(LED_ADVERTISING, LEDOFF);
  }
}

void RFduinoBLE_onConnect()
{
  // When a device connects, turn on the green LED
  digitalWrite(LED_CONNECTED, LEDON);
}

void RFduinoBLE_onDisconnect()
{
  // When a device disconnects, make sure the motor is off and the green
  // LED is turned off again.
  digitalWrite(LED_CONNECTED, LEDOFF);
  analogWrite(MOTOR_SIGNAL, 0);
}


void RFduinoBLE_onReceive(char *data, int len)
{
  // The first byte of any received packed is used to set the PWM
  // duty to set the vibration intensity. The 307-103 motor used in the 
  // project needs a minimum value of about 15 before it will start, so
  // any received value from 1 to 14 gets increased to 15.
  uint8_t motor_value = (uint8_t)data[0];
  if ((motor_value >= 1) & (motor_value < MINIMUM_ON_VALUE)) {
    motor_value = MINIMUM_ON_VALUE;
  }
  analogWrite(MOTOR_SIGNAL, motor_value);
}
