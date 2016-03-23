package cc.chalmers.sittostand.BLEGattManager.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

public class GattConnectOperation extends GattOperation {

    public GattConnectOperation(BluetoothDevice device) {
        super(device);
    }

    @Override
    public void execute(BluetoothGatt gatt) {
        gatt.connect();
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }
}
