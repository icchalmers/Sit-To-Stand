package cc.chalmers.sittostand.BLEGattManager.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.UUID;

public class GattServiceDiscoveryOperation extends GattOperation {

    public GattServiceDiscoveryOperation(BluetoothDevice device) {
        super(device);
    }

    @Override
    public void execute(BluetoothGatt gatt) {
        Log.d(TAG, "Starting service discovery for " + gatt.getDevice().getAddress());
        gatt.discoverServices();
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }
}
