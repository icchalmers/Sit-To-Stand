/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.chalmers.sittostand;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BLEMotorService extends Service {
    private final static String TAG = BLEMotorService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDeviceLeft;
    private BluetoothDevice mBluetoothDeviceRight;
    private BluetoothGatt mBluetoothGattLeft;
    private BluetoothGatt mBluetoothGattRight;
    private BluetoothGattCharacteristic mCharacteristicLeft;
    private BluetoothGattCharacteristic mCharacteristicRight;


    private ConcurrentLinkedQueue<GattCharacteristicWriteOperation> mQueue;
    private ConcurrentHashMap<String, BluetoothGatt> mGatts;
    private AsyncTask<Void, Void, Void> mCurrentOperationTimeout;
    private static boolean BLEBusy;
    private GattCharacteristicWriteOperation mCurrentOperation;
    private GattCharacteristicWriteOperation mCurrentLeftOperation;
    private GattCharacteristicWriteOperation mCurrentRightOperation;

    public enum State {
        UNKNOWN,
        IDLE,
        CONNECTING_LEFT,
        CONNECTED_LEFT,
        CONNECTING_RIGHT,
        CONNECTED_RIGHT,
        DISCOVERING_LEFT,
        DISCOVERED_LEFT,
        DISCOVERING_RIGHT,
        DISCOVERED_RIGHT,
        INITIAL_WRITE_LEFT,
        INITIAL_WRITE_RIGHT,
        READY,
        DISCONNECTING,
        RESET
    }
    private State mConnectionState = State.UNKNOWN;


    public final static String ACTION_MOTORS_CONNECTED =
            "cc.chalmers.BLEMotorService.ACTION_MOTORS_CONNECTED";
    public final static String ACTION_MOTORS_DISCONNECTED =
            "cc.chalmers.BLEMotorService.ACTION_MOTORS_DISCONNECTED";
    public final static String ACTION_MOTORS_READY =
            "cc.chalmers.BLEMotorService.ACTION_MOTORS_READY";

    public class LocalBinder extends Binder {
        BLEMotorService getService() {
            return BLEMotorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        mQueue = new ConcurrentLinkedQueue<>();
        mGatts = new ConcurrentHashMap<>();
        BLEBusy = false;
        mConnectionState = State.IDLE;
        return true;
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mConnectionState == State.CONNECTING_LEFT) {
                    Log.i(TAG, "Connected left motor " + gatt.getDevice().getAddress());
                    mBluetoothGattLeft = gatt;
                    updateState(State.CONNECTED_LEFT);
                } else if(mConnectionState == State.CONNECTING_RIGHT) {
                    Log.i(TAG, "Connected right motor " + gatt.getDevice().getAddress());
                    mBluetoothGattRight = gatt;
                    updateState(State.CONNECTED_RIGHT);
                } else {
                    Log.e(TAG, "PANIC! Unknown connection made");
                    updateState(State.UNKNOWN);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                BluetoothDevice device = gatt.getDevice();
                Log.e(TAG, "PANIC! Disconnected from device " + device.getAddress());
                gatt.close();
                updateState(State.UNKNOWN);
            } else {
                Log.e(TAG, "Weird connection state change! " + gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (mConnectionState == State.DISCOVERING_LEFT) {
                Log.i(TAG, "Left services discovered, status: " + status);
                mCharacteristicLeft =
                        mBluetoothGattLeft.getService(
                                    UUID.fromString(RFDuinoGATTServices.RFDUINO_SERVICE)).
                                getCharacteristic(
                                    UUID.fromString(RFDuinoGATTServices.RFDUINO_SEND_DATA));
                updateState(State.DISCOVERED_LEFT);
            } else if(mConnectionState == State.DISCOVERING_RIGHT) {
                Log.i(TAG, "Right services discovered, status: " + status);
                mCharacteristicRight =
                        mBluetoothGattRight.getService(
                                UUID.fromString(RFDuinoGATTServices.RFDUINO_SERVICE)).
                                getCharacteristic(
                                        UUID.fromString(RFDuinoGATTServices.RFDUINO_SEND_DATA));
                updateState(State.DISCOVERED_RIGHT);
            } else {
                Log.e(TAG, "PANIC! Unknown services discovered");
                updateState(State.UNKNOWN);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(mConnectionState == State.INITIAL_WRITE_LEFT){
                Log.d(TAG,"Successfully initialised the left motor");
                updateState(State.INITIAL_WRITE_RIGHT);
                return;
            }
            if (mConnectionState == State.INITIAL_WRITE_RIGHT) {
                Log.d(TAG, "Successfully initialised the right motor");
                updateState(State.READY);
                return;
            }
            if (gatt == mBluetoothGattLeft){
                Log.d(TAG, "Finished writing to left motor. Device: " + gatt.getDevice().getAddress() + " Status: " + status);
            }
            if (gatt == mBluetoothGattRight) {
                Log.d(TAG, "Finished writing to right motor. Device: " + gatt.getDevice().getAddress() + " Status: " + status);
            }
            mCurrentOperation = null;
            BLEBusy = false;
            nextWrite();
        }
    };

    private synchronized void queueWrite(GattCharacteristicWriteOperation operation) {
        if(operation.getGatt() == mBluetoothGattLeft){
            Log.d(TAG,"Write to left motor queued");
            mCurrentLeftOperation = operation;
        }
        if(operation.getGatt() == mBluetoothGattRight) {
            Log.d(TAG,"Write to right motor queued");
            mCurrentRightOperation = operation;
        }
        nextWrite();
    }

    private synchronized void nextWrite() {
        // This is sloppy but simple. Left motor always gets write priority
        if(BLEBusy){
            Log.w(TAG,"BLE busy! Try again later...");
            return;
        }
        if(mCurrentLeftOperation != null) {
            Log.d(TAG,"Starting write to left motor");
            mCurrentOperation = mCurrentLeftOperation;
            mCurrentLeftOperation = null;
            doWrite(mCurrentOperation);
            return;
        }
        if(mCurrentRightOperation != null) {
            Log.d(TAG,"Starting write to right motor");
            mCurrentOperation = mCurrentRightOperation;
            mCurrentRightOperation = null;
            doWrite(mCurrentOperation);
        }
    }

    private synchronized void doWrite(GattCharacteristicWriteOperation operation) {
        BLEBusy = true;
        Log.d(TAG,"Write started...");
        operation.execute();
    }


    /**
     * Connects to the GATT servers hosted on each vibration device.
     *
     * @param addressLeft The device address of the left motor.
     *
     * @param addressRight The device address of the right motor.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String addressLeft, final String addressRight) {
        if (mBluetoothAdapter == null || addressLeft == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        mBluetoothDeviceLeft = mBluetoothAdapter.getRemoteDevice(addressLeft);
        if (mBluetoothDeviceLeft == null) {
            Log.w(TAG, "Device not found.  Unable to connect to left device.");
            return false;
        }

        mBluetoothDeviceRight = mBluetoothAdapter.getRemoteDevice(addressRight);
        if (mBluetoothDeviceRight == null) {
            Log.w(TAG, "Device not found.  Unable to connect to right device.");
            return false;
        }
        Log.d(TAG, "Requesting connection to both motors");

        updateState(State.CONNECTING_LEFT);

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.w(TAG, "Should be disconnecting both devices...");
        if (mBluetoothAdapter == null || mBluetoothDeviceLeft == null || mBluetoothDeviceRight == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        mBluetoothDeviceRight = null;
        mBluetoothDeviceLeft = null;
    }


    public boolean writeMotor(String motor, int value) {
        if (mBluetoothAdapter == null || mBluetoothDeviceLeft == null || mBluetoothDeviceRight == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        if (mConnectionState == State.INITIAL_WRITE_LEFT) {
            mCharacteristicLeft.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattLeft.writeCharacteristic(mCharacteristicLeft);
            return true;
        }
        if (mConnectionState == State.INITIAL_WRITE_RIGHT) {
            mCharacteristicRight.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattRight.writeCharacteristic(mCharacteristicRight);
            return true;
        }
        if (mConnectionState != State.READY) {
            Log.e(TAG, "Tried to write before connections ready");
            return false;
        }
        GattCharacteristicWriteOperation operation;
        if (motor.contentEquals("left")) {
            mCharacteristicLeft.setValue(value,BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            operation = new GattCharacteristicWriteOperation(mBluetoothGattLeft, mCharacteristicLeft);
            queueWrite(operation);
            return true;
        }
        if (motor.contentEquals("right")) {
            mCharacteristicRight.setValue(value,BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            operation = new GattCharacteristicWriteOperation(mBluetoothGattRight, mCharacteristicRight);
            queueWrite(operation);
            return true;
        }

        return false;
    }


    private void updateState(State newState) {
        mConnectionState = newState;
        Log.d("CurrentState", mConnectionState.name());
        switch(newState) {
            case CONNECTING_LEFT:
                doConnectingLeft();
                break;
            case CONNECTED_LEFT:
                doConnectingRight();
                break;
            case CONNECTED_RIGHT:
                broadcastUpdate(ACTION_MOTORS_CONNECTED);
                updateState(State.DISCOVERING_LEFT);
                break;
            case DISCOVERING_LEFT:
                doDiscoveringLeft();
                break;
            case DISCOVERED_LEFT:
                updateState(State.DISCOVERING_RIGHT);
                break;
            case DISCOVERING_RIGHT:
                doDiscoveringRight();
                break;
            case DISCOVERED_RIGHT:
                updateState(State.INITIAL_WRITE_LEFT);
                break;
            case INITIAL_WRITE_LEFT:
                doInitialWriteLeft();
                break;
            case INITIAL_WRITE_RIGHT:
                doInitialWriteRight();
                break;
            case READY:
                doReady();
                break;
            case RESET:
                doReset();
                break;
            case UNKNOWN:
                doUnknown();
                break;
        }
    }

    private void doConnectingLeft(){
        mBluetoothDeviceLeft.connectGatt(this, false, mGattCallback);
    }

    private void doConnectingRight(){
        mBluetoothDeviceRight.connectGatt(this, false, mGattCallback);
        updateState(State.CONNECTING_RIGHT);
    }

    private void doDiscoveringLeft(){
        mBluetoothGattLeft.discoverServices();
    }

    private void doDiscoveringRight() {
        mBluetoothGattRight.discoverServices();
    }

    private void doInitialWriteLeft() {
        writeMotor("left", 0);
    }

    private void doInitialWriteRight() {
        writeMotor("right", 0);
    }

    private void doReady() {
        broadcastUpdate(ACTION_MOTORS_READY);
    }
    private void doReset() {
        //TODO
    }

    private void doUnknown() {
        //TODO
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
}

