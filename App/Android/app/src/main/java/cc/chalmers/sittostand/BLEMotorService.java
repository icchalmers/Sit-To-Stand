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
 *
 * Edited by Iain Chalmers, (C) 2016
 *
 * Based on com.example.android.bluetoothlegatt.BluetoothLeService.java Android sample code
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
 * Service for managing connection and data communication with a GATT server hosted on an RFDuino
 * based BLE Vibration Motor device.
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

    private static boolean BLEBusy;
    private GattCharacteristicWriteOperation mCurrentOperation;
    private GattCharacteristicWriteOperation mCurrentLeftOperation;
    private GattCharacteristicWriteOperation mCurrentRightOperation;

    /**
     * "State" type is used to define the current state of the service.
     *
     * In general, the program flow is to connect to both motors, then discover their services, then
     * do an initial write to each motor to make sure they are off and to then wait for write
     * commands.
     *
     * The logic for switching between states is a rocky when anything unexpected happens e.g. one
     * of the motors disconnects. In this case a general catch all of "UNKNOWN" state is use and the
     * state machine attempts to reset and start again.
     */
    public enum State {
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
        RESET,
        UNKNOWN
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
        BLEBusy = false;
        mConnectionState = State.IDLE;
        return true;
    }

    /**
     * mGattCallback is the BLE Gatt callback used to report the outcome of most BLE Gatt actions.
     */
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
                Log.e(TAG, "Disconnected from device " + device.getAddress());
                gatt.close();
                if (device == mBluetoothDeviceLeft) {
                    mBluetoothGattLeft = null;
                }
                if (device == mBluetoothDeviceRight) {
                    mBluetoothGattRight = null;
                }
                broadcastUpdate(ACTION_MOTORS_DISCONNECTED);

                // Going to UNKNOWN is cludgy. Really the state machine should handle the
                // disconnection of one device followed cleanly by disconnecting the other and then
                // going to IDLE. Going to UNKNOWN is a buggy quick-fix.
                updateState(State.UNKNOWN);
            } else {
                Log.e(TAG, "PANIC! Unknown connection state change! " + gatt.getDevice().getAddress());
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

    /**
     * Add a GattCharacteristicWriteOperation to the write queue.
     */
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

    /**
     * nextWrite should be called any time a new write operation is added to the queue or whenever
     * a previous write operation finishes.
     *
     * The write queue is only one deep for each motor. This means that of writes are being
     * performed too quickly, only the most recent write operation to that motor will be performed.
     */
    private synchronized void nextWrite() {
        // This is sloppy but simple. Left motor always gets write priority
        if(BLEBusy){
            Log.w(TAG,"BLE busy! Trying again later...");
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

    /**
     * Perform the actual write to the BLE GATT Characteristic. Response is asynchronous via the
     * "mGattCallback" callback.
     */
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
        Log.w(TAG, "Disconnecting both devices...");
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (mBluetoothGattLeft != null) {
            mBluetoothGattLeft.disconnect();
        }
        if (mBluetoothGattRight != null) {
            mBluetoothGattRight.disconnect();
        }
    }

    /**
     * Switches the left and right motors.
     *
     * @return true if switch was successful.
     */
    public boolean switchMotors() {
        if (mConnectionState == State.READY) {
            BluetoothDevice tempDevice = mBluetoothDeviceLeft;
            BluetoothGatt tempGatt = mBluetoothGattLeft;
            BluetoothGattCharacteristic tempCharacteristic =  mCharacteristicLeft;
            mBluetoothDeviceLeft = mBluetoothDeviceRight;
            mBluetoothGattLeft = mBluetoothGattRight;
            mCharacteristicLeft = mCharacteristicRight;
            mBluetoothDeviceRight = tempDevice;
            mBluetoothGattRight = tempGatt;
            mCharacteristicRight = tempCharacteristic;
            return true;
        } else {
            return false;
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGattLeft != null) {
            mBluetoothGattLeft.close();
        }

        if (mBluetoothGattRight != null) {
            mBluetoothGattRight.close();
        }
        disconnect();
        mBluetoothGattLeft = null;
        mBluetoothGattRight = null;
    }


    /**
     * Used to write a vibration value to the given motor.
     *
     * @param motor: "left" or "right".
     * @param value: An integer from 0 to 255.
     * @return returns "true" if the write was successfully queued, "false" otherwise.
     */
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


    /**
     * Used to update the current state of the state machine governing the service.
     */
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
        // This is not a true reset and the states wont quite match - requesting a disconnect on the
        // BLE connections does not happen instantly so state will enter IDLE erroneously when only
        // one of the motors has been disconnected.
        close();
        updateState(State.IDLE);
    }

    private void doUnknown() {
        Log.e(TAG,"Entered an unknown state - performing reset!");
        updateState(State.RESET);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public State getState() {
        return mConnectionState;
    }
}

