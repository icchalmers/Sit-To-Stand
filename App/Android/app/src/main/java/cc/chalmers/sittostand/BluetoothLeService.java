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
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import cc.chalmers.sittostand.BLEGattManager.GattManager;
import cc.chalmers.sittostand.BLEGattManager.GattOperationBundle;
import cc.chalmers.sittostand.BLEGattManager.operations.GattCharacteristicWriteOperation;
import cc.chalmers.sittostand.BLEGattManager.operations.GattConnectOperation;
import cc.chalmers.sittostand.BLEGattManager.operations.GattDisconnectOperation;
import cc.chalmers.sittostand.BLEGattManager.operations.GattOperation;
import cc.chalmers.sittostand.BLEGattManager.operations.GattServiceDiscoveryOperation;

import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDeviceLeft;
    private BluetoothDevice mBluetoothDeviceRight;
    private GattManager mGattManager = new GattManager();

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
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

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param addressLeft The device address of the destination device.
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

        GattOperation connectLeft = new GattConnectOperation(mBluetoothDeviceLeft);
        mGattManager.queue(connectLeft);
        GattOperation connectRight = new GattConnectOperation(mBluetoothDeviceRight);
        mGattManager.queue(connectRight);
        GattOperation discoverServicesLeft = new GattServiceDiscoveryOperation(mBluetoothDeviceLeft);
        mGattManager.queue(discoverServicesLeft);
        GattOperation discoverServicesRight = new GattServiceDiscoveryOperation(mBluetoothDeviceRight);
        mGattManager.queue(discoverServicesRight);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.w(TAG,"Should be disconnecting both devices...");
        if (mBluetoothAdapter == null || mBluetoothDeviceLeft == null || mBluetoothDeviceRight == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        GattOperation disconnectLeft = new GattDisconnectOperation(mBluetoothDeviceLeft);
        mGattManager.queue(disconnectLeft);
        GattOperation disconnectRight = new GattDisconnectOperation(mBluetoothDeviceRight);
        mGattManager.queue(disconnectRight);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        mBluetoothDeviceRight = null;
        mBluetoothDeviceLeft = null;
    }


    public void writeCustomCharacteristic(int value) {
        if (mBluetoothAdapter == null || mBluetoothDeviceLeft == null || mBluetoothDeviceRight == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //GattOperationBundle bundle = new GattOperationBundle();
        byte[] temp = new byte[]{(byte)value};
        GattOperation writeLeft = new GattCharacteristicWriteOperation(
                mBluetoothDeviceLeft,
                UUID.fromString("00002220-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("00002222-0000-1000-8000-00805f9b34fb"),
                temp);
        mGattManager.queue(writeLeft);

        GattOperation writeRight = new GattCharacteristicWriteOperation(
                mBluetoothDeviceRight,
                UUID.fromString("00002220-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("00002222-0000-1000-8000-00805f9b34fb"),
                temp);
        mGattManager.queue(writeRight);
    }
}
