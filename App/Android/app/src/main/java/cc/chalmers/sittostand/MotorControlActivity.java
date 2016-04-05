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
 * Based on com.example.android.bluetoothlegatt.DeviceScanActivity.java Android sample code
 */
package cc.chalmers.sittostand;


import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * For a set of BLEVibrators this activity allows you to connect and test the motors and assign to
 * left/right leg.  The Activity communicates with {@code BLEMotorService}, which in turn interacts
 * with the Bluetooth LE API.
 */
public class MotorControlActivity extends Activity {
    private final static String TAG = MotorControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE1_NAME = "DEVICE1_NAME";
    public static final String EXTRAS_DEVICE1_ADDRESS = "DEVICE1_ADDRESS";
    public static final String EXTRAS_DEVICE2_NAME = "DEVICE2_NAME";
    public static final String EXTRAS_DEVICE2_ADDRESS = "DEVICE2_ADDRESS";

    private String mDeviceLeftName;
    private String mDeviceLeftAddress;
    private String mDeviceRightName;
    private String mDeviceRightAddress;

    private BLEMotorService mBLEMotorService;

    private View mView = null;
    private Button mButtonSwitchMotors = null;

    private boolean mConnected = false;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEMotorService = ((BLEMotorService.LocalBinder) service).getService();
            if (!mBLEMotorService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBLEMotorService.connect(mDeviceLeftAddress, mDeviceRightAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEMotorService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_MOTORS_CONNECTED: connected to both motors
    // ACTION_MOTORS_DISCONNECTED: disconnected from a motor
    // ACTION_MOTORS_READY: discovered GATT services.
    //
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEMotorService.ACTION_MOTORS_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BLEMotorService.ACTION_MOTORS_DISCONNECTED.equals(action)) {
                mConnected = false;
                setViewAndChildrenEnabled(mView, false);
                invalidateOptionsMenu();
            } else if (BLEMotorService.ACTION_MOTORS_READY.equals(action)) {
                setViewAndChildrenEnabled(mView, true);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_control);
        mView = findViewById(R.id.motor_control_layout);
        mButtonSwitchMotors = (Button)findViewById(R.id.buttonSwitchMotors);
        setViewAndChildrenEnabled(mView, false);

        final Intent intent = getIntent();
        mDeviceLeftName = intent.getStringExtra(EXTRAS_DEVICE1_NAME);
        mDeviceLeftAddress = intent.getStringExtra(EXTRAS_DEVICE1_ADDRESS);
        mDeviceRightName = intent.getStringExtra(EXTRAS_DEVICE2_NAME);
        mDeviceRightAddress = intent.getStringExtra(EXTRAS_DEVICE2_ADDRESS);

        ActionBar mActionBar = getActionBar();
        if(mActionBar != null) {
            mActionBar.setTitle("Motor Setup");
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        Intent gattServiceIntent = new Intent(this, BLEMotorService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBLEMotorService != null) {
            final boolean result = mBLEMotorService.connect(mDeviceLeftAddress, mDeviceRightAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBLEMotorService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBLEMotorService.connect(mDeviceLeftAddress, mDeviceRightAddress);
                return true;
            case R.id.menu_disconnect:
                mBLEMotorService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEMotorService.ACTION_MOTORS_CONNECTED);
        intentFilter.addAction(BLEMotorService.ACTION_MOTORS_DISCONNECTED);
        intentFilter.addAction(BLEMotorService.ACTION_MOTORS_READY);
        return intentFilter;
    }

    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    private static int queuedTasks = 0;

    public void identifyRight(View view) {
        mBLEMotorService.writeMotor("right", 50);
        mButtonSwitchMotors.setClickable(false);
        // Turn the motor back off after 1 second
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mBLEMotorService.writeMotor("right", 0);
                queuedTasks -= 1;
                checkDisabledSwitchMotorButton();
            }
        };
        queuedTasks += 1;
        worker.schedule(task, 1, TimeUnit.SECONDS);
    }

    public void identifyLeft(View view) {
        mBLEMotorService.writeMotor("left", 50);
        mButtonSwitchMotors.setClickable(false);
        // Turn the motor back off after 1 second
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mBLEMotorService.writeMotor("left", 0);
                queuedTasks -= 1;
                checkDisabledSwitchMotorButton();
            }
        };
        queuedTasks += 1;
        worker.schedule(task, 1, TimeUnit.SECONDS);
    }

    private void checkDisabledSwitchMotorButton() {
        if (queuedTasks <= 0) {
            mButtonSwitchMotors.setClickable(true);
        }
    }

    public void switchMotors(View view) {
        mBLEMotorService.switchMotors();
    }

    public void startCalibration(View v) {
        final Intent intent = new Intent(this, Screen.class);
        startActivity(intent);
    }

    private static void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }
}
