# BLE Vibrator: 3D Printed Case

## Printing a Case

The two STL files, "CaseBottom.stl" and "CaseTop.stl" can be used to print a case for the BLE vibration motor.

I printed the parts on a Replicator 2 on "high" quality, 3 shells, no rafts, no supports:

* The file [FullCase.thing](./FullCase.thing) 
is designed to be used directly with MakerBot Desktop with the settings I used for a Replicator 2. It contains one case top and one case bottom. The print time is approximately 3h11m and uses about 25g of PLA.
* The file [TwoCases.thing](./TwoCases.thing) contains two tops and two bottoms i.e. two complete cases. The print time is approximately 6h23m and uses about 50g of PLA.

"Standard" quality at 0.2mm layer height works fine if you are in a hurry.

## Design Files

The case was designed in Autodesk Inventor. If you need to make any changes, here's a brief description of what each file is for:

* [CaseBase.ipt](./CaseBase.ipt): This is the master part for the case, from which CaseTop and CaseBottom are derived. If you need to make fundamental changes to the case design, make them here and then update the derived parts.
* [CaseTop.ipt](./CaseTop.ipt): This is the top piece of the case, derived from CaseBase. It has been sliced and had overlap rims added.
* [CaseBottom.ipt](./CaseBottom.ipt): This is the bottom piece of the case, derived from CaseBase. It has been sliced and had overlap rims added, as well as the support pieces for the hook-and-loop straps.
* [FullUnitAssembly.iam](./FullUnitAssembly.iam): This is the assembly of one unit, comprised of the top of the case, the bottom of the case, the motor and the PCB with major components.
* The remained of the files are simply component parts used in FullUnitAssembly to verify that everything fits.