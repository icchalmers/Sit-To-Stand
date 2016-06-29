/**
 * Based on code from Nordic Semiconductor:
 *
 * https://github.com/NordicSemiconductor/puck-central-android/tree/master/PuckCentral/app/src/main/java/no/nordicsemi/puckcentral/bluetooth/gatt
 *
 */

package cc.chalmers.sittostand;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class GattCharacteristicWriteOperation {

    protected final static String TAG = "GattWriteOperation";

    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 10000;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGatt mGatt;

    public GattCharacteristicWriteOperation(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
        mGatt = gatt;
    }

    public void execute() {
        Log.d(TAG, "writing " + mCharacteristic.getUuid());
        mGatt.writeCharacteristic(mCharacteristic);
    }

    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    public int getTimoutInMillis() {
        return DEFAULT_TIMEOUT_IN_MILLIS;
    }

    public BluetoothGatt getGatt(){
        return mGatt;
    }
}
