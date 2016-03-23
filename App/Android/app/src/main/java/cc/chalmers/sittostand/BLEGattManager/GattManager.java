package cc.chalmers.sittostand.BLEGattManager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.AsyncTask;
import android.util.Log;

import cc.chalmers.sittostand.BLEGattManager.operations.GattOperation;

import org.droidparts.Injector;
import org.droidparts.bus.EventBus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GattManager {

    protected final static String TAG = "GattManager";

    private ConcurrentLinkedQueue<GattOperation> mQueue;
    private ConcurrentHashMap<String, BluetoothGatt> mGatts;
    private GattOperation mCurrentOperation;
    private AsyncTask<Void, Void, Void> mCurrentOperationTimeout;

    public static final String TRIGGER_CONNECTION_STATE_CHANGED = "trigger_connection_state_changed";

    public GattManager() {
        mQueue = new ConcurrentLinkedQueue<>();
        mGatts = new ConcurrentHashMap<>();
        mCurrentOperation = null;
    }

    public synchronized void cancelCurrentOperationBundle() {
        Log.v(TAG, "Cancelling current operation. Queue size before: " + mQueue.size());
        if(mCurrentOperation != null && mCurrentOperation.getBundle() != null) {
            for(GattOperation op : mCurrentOperation.getBundle().getOperations()) {
                mQueue.remove(op);
            }
        }
        Log.v(TAG, "Queue size after: " + mQueue.size());
        mCurrentOperation = null;
        drive();
    }

    public synchronized void queue(GattOperation gattOperation) {
        mQueue.add(gattOperation);
        Log.v(TAG, "Queueing Gatt operation, size will now become: " + mQueue.size());
        drive();
    }

    private synchronized void drive() {
        if(mCurrentOperation != null) {
            Log.e(TAG, "tried to drive, but currentOperation was not null, " + mCurrentOperation);
            return;
        }
        if( mQueue.size() == 0) {
            Log.v(TAG, "Queue empty, drive loop stopped.");
            mCurrentOperation = null;
            return;
        }

        final GattOperation operation = mQueue.poll();
        Log.v(TAG, "Driving Gatt queue, size will now become: " + mQueue.size());
        setCurrentOperation(operation);


        if(mCurrentOperationTimeout != null) {
            mCurrentOperationTimeout.cancel(true);
        }
        mCurrentOperationTimeout = new AsyncTask<Void, Void, Void>() {
            @Override
            protected synchronized Void doInBackground(Void... voids) {
                try {
                    Log.v(TAG, "Starting to do a background timeout");
                    wait(operation.getTimoutInMillis());
                } catch (InterruptedException e) {
                    Log.v(TAG, "was interrupted out of the timeout");
                }
                if(isCancelled()) {
                    Log.v(TAG, "The timeout was cancelled, so we do nothing.");
                    return null;
                }
                Log.v(TAG, "Timeout ran to completion, time to cancel the entire operation bundle. Abort, abort!");
                cancelCurrentOperationBundle();
                return null;
            }

            @Override
            protected synchronized void onCancelled() {
                super.onCancelled();
                notify();
            }
        }.execute();

        final BluetoothDevice device = operation.getDevice();
        if(mGatts.containsKey(device.getAddress())) {
            execute(mGatts.get(device.getAddress()), operation);
        } else {
            device.connectGatt(Injector.getApplicationContext(), true, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);

                    EventBus.postEvent(TRIGGER_CONNECTION_STATE_CHANGED,
                            new ConnectionStateChangedBundle(
                                    device.getAddress(),
                                    newState));

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Gatt connected to device " + device.getAddress());
                        mGatts.put(device.getAddress(), gatt);
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from gatt server " + device.getAddress() + ", newState: " + newState);
                        mGatts.remove(device.getAddress());
                        setCurrentOperation(null);
                        gatt.close();
                        drive();
                    }
                }


                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    Log.d(TAG, "services discovered, status: " + status);
                    execute(gatt, operation);
                }


                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    Log.d(TAG, "Characteristic " + characteristic.getUuid() + " written to on device " + device.getAddress() + " Status: " + status);
                    setCurrentOperation(null);
                    drive();
                }
            });
        }
    }

    private void execute(BluetoothGatt gatt, GattOperation operation) {
        if(operation != mCurrentOperation) {
            return;
        }
        operation.execute(gatt);
        if(!operation.hasAvailableCompletionCallback()) {
            setCurrentOperation(null);
            drive();
        }
    }

    public synchronized void setCurrentOperation(GattOperation currentOperation) {
        mCurrentOperation = currentOperation;
    }

    public BluetoothGatt getGatt(BluetoothDevice device) {
        return mGatts.get(device);
    }

    public void queue(GattOperationBundle bundle) {
        for(GattOperation operation : bundle.getOperations()) {
            queue(operation);
        }
    }

    public class ConnectionStateChangedBundle {
        public final int mNewState;
        public final String mAddress;

        public ConnectionStateChangedBundle(String address, int newState) {
            mAddress = address;
            mNewState = newState;
        }
    }
}
