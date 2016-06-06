# BLE Vibrator: PCB

The PCB was designed in KiCAD. It is a singled sided board. I cut mine on a Denford PCB Engraver (the files for that specific machine are in the "DenfordPCBEngraver" folder). 
Gerbers are also included for edge cuts and front copper. Those were the only layers I needed. If you need something else (e.g. solder mask) then you'll need to generate the gerbers yourself.

Note that the PCB design has only been manufactured on the Denford. The footprints might be totally unacceptable for a proper fab house!

# BOM

A lot of the parts were selected purely based on what I had to hand/what was cheap.

* All the capacitors/resistors are 1206 SMD
* LED is a Bivar SM1204BC-R/G
* RFDuino is the SMT module variant
* Battery header is an S2B-PH-SM4-TB(LF)(SN)
* Slide switch is an ALPS SSSS810701
* Motor driver MOSFET is an MCH3479-TL-H
* Voltage regulator is a TPS737
* Vibration motor is a Precision Microdrives P/N: 307-103
* Battery is a 3.7 V, 400 mAh LiPo

I'd like to experiment with changing the vibration motor and the battery.

The vibration motor runs at quite a high RPM, which makes it feel quite a bit weaker when the unit is worn on top of clothes. It also has a pretty nasty 650 mA starting current. The 308-103 motor might give a stronger vibration when worn over clothes, but would require a case redesign.

The battery is probably unnecessarily large. Running the motor at 50%, the battery lasted for about 6h30m (the motor is the most significant power draw). In real use, the vibration motor will be running at much lower than this and won't even be on all the time. As it stands the battery is a nice fit to the PCB size, but in future iterations it's a prime target for minimisation.
