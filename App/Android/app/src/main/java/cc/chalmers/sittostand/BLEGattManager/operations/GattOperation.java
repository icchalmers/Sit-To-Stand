package cc.chalmers.sittostand.BLEGattManager.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import cc.chalmers.sittostand.BLEGattManager.GattOperationBundle;

public abstract class GattOperation {

    protected final static String TAG = "GattOperation";

    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 10000;
    private BluetoothDevice mDevice;
    private GattOperationBundle mBundle;

    public GattOperation(BluetoothDevice device) {
        mDevice = device;
    }

    public abstract void execute(BluetoothGatt bluetoothGatt);

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getTimoutInMillis() {
        return DEFAULT_TIMEOUT_IN_MILLIS;
    }

    public abstract boolean hasAvailableCompletionCallback();

    public GattOperationBundle getBundle() {
        return mBundle;
    }

    public void setBundle(GattOperationBundle bundle) {
        mBundle = bundle;
    }
}
