# BLE Vibrator: PCB

The PCB was designed in KiCAD, with the intention of using cheap 2-layer fab houses. Specifically, the board was designed with [OSH Park's design rules](http://docs.oshpark.com/design-tools/kicad/kicad-design-rules/).

# BOM 

The parts in the schematic all include a manufacturer’s part number for the exact component I have used. Many of the parts will work just fine with an equivalent e.g. a different brand of resister, a specification equivalent MOSFET etc.

I used [KiCost](https://github.com/xesscorp/KiCost) to generate a rough costing for the components. See [StrokeRehabilitation_KiCost.xlsx](./StrokeRehabilitation_KiCost.xlsx). Note that this is based on US price, and does not include the vibration motor or the LiPo battery. Note that a working unit also needs a case and some way of attaching it to the user - these costs are also not included.

Most of the passives are 0805, with the exception of the LiPo voltage measuring circuitry. The two resistors and one capacitor are 0603. This is purely based on the components I could source quickly and to let me try 0603 as a size.

# Future Work

I'd like to experiment with changing the vibration motor and the battery.

The vibration motor runs at quite a high RPM, which makes it feel quite a bit weaker when the unit is worn on top of clothes. It also has a pretty nasty 650 mA starting current. The 308-103 motor might give a stronger vibration when worn over clothes, but would require a case redesign. _[Since I ordered my 308-103 the minimum order quantity has gone up to 1k. Precision Microdrives tells me they have a new similar motor, the 308-106, coming out at the end of July 2016. It should have a minimum order quantity of 1...]_

The battery is probably unnecessarily large. Running the motor at 50%, the battery lasted for about 6h30m (the motor is the most significant power draw). In real use, the vibration motor will be running at much lower than this and won't even be on all the time. As it stands the battery is a nice fit to the PCB size, but in future iterations it's a prime target for miniaturisation.
