# BLE Vibrator: PCB

The PCB was designed in KiCAD, with the intention of using cheap 2-layer fab houses. Specifically, the board was designed with [OSH Park's design rules](http://docs.oshpark.com/design-tools/kicad/kicad-design-rules/).

# BOM 

The parts in the schematic all include a manufacturer’s part number for the exact component I have used. Many of the parts will work just fine with an equivalent e.g. a different brand of resister, a specification equivalent MOSFET etc.

I used [KiCost](https://github.com/xesscorp/KiCost) to generate a rough costing for the components. See [StrokeRehabilitation_KiCost.xlsx](./StrokeRehabilitation_KiCost.xlsx). Note that this is based on US price, and does not include the vibration motor or the LiPo battery. Note that a working unit also needs a case and some way of attaching it to the user - these costs are also not included.

Most of the passives are 0805, with the exception of the LiPo voltage measuring circuitry. The two resistors and one capacitor are 0603. This is purely based on the components I could source quickly and to let me try 0603 as a size.

# Improvements

There are two minor issues with the current PCB design. 

* The footprint for soldering the motor wires is fiddly. A single hole with a long strip to solder the wire to would probably be just as secure and save a lot of assembly hassle.
* The holes for soldering the battery wires are a __nightmare__. The wires are not very flexible so if they length is even slightly off it makes getting the whole assembly together a frustrating experience. I got rid of the JST connector because it was the tallest component and took up a lot of room. In any future design I would leave a lot more space (probably in the case) for wire management or find a lower profile connector to use instead.