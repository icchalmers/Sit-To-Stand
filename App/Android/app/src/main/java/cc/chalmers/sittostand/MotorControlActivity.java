package cc.chalmers.sittostand;


import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * For a set of BLEVibrators this activity allows you to connect and test the motors and assign to
 * left/right leg.  The Activity communicates with {@code MotorService}, which in turn interacts
 * with the Bluetooth LE API.
 */
public class MotorControlActivity extends Activity {
    private final static String TAG = MotorControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE1_NAME = "DEVICE1_NAME";
    public static final String EXTRAS_DEVICE1_ADDRESS = "DEVICE1_ADDRESS";
    public static final String EXTRAS_DEVICE2_NAME = "DEVICE2_NAME";
    public static final String EXTRAS_DEVICE2_ADDRESS = "DEVICE2_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceLeftName;
    private String mDeviceLeftAddress;
    private String mDeviceRightName;
    private String mDeviceRightAddress;

    private BluetoothLeService mBluetoothLeService;

    private SeekBar motorControl = null;
    private View mView = null;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<>();
    private boolean mConnected = false;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceLeftAddress, mDeviceRightAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
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
            if (BluetoothLeService.ACTION_MOTORS_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_MOTORS_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_MOTORS_READY.equals(action)) {
                setViewAndChildrenEnabled(mView, true);
            }
        }
    };

    private void clearUI() {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_control);
        mView = findViewById(R.id.motor_control_layout);
        setViewAndChildrenEnabled(mView, false);

        final Intent intent = getIntent();
        mDeviceLeftName = intent.getStringExtra(EXTRAS_DEVICE1_NAME);
        mDeviceLeftAddress = intent.getStringExtra(EXTRAS_DEVICE1_ADDRESS);
        mDeviceRightName = intent.getStringExtra(EXTRAS_DEVICE2_NAME);
        mDeviceRightAddress = intent.getStringExtra(EXTRAS_DEVICE2_ADDRESS);

        motorControl = (SeekBar) findViewById(R.id.motorSlider);
        motorControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBluetoothLeService.writeMotor("left", progress);
                mBluetoothLeService.writeMotor("right", progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mBluetoothLeService.writeMotor("left", seekBar.getProgress());
                mBluetoothLeService.writeMotor("right", seekBar.getProgress());
            }
        });
        ActionBar mActionBar = getActionBar();
        if(mActionBar != null) {
            mActionBar.setTitle(mDeviceLeftName);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceLeftAddress, mDeviceRightAddress);
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
        mBluetoothLeService = null;
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
                mBluetoothLeService.connect(mDeviceLeftAddress, mDeviceRightAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_MOTORS_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_MOTORS_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_MOTORS_READY);
        return intentFilter;
    }

    public void disableMotors(View v){
        if(mBluetoothLeService != null) {
            mBluetoothLeService.writeMotor("left", 0);
            mBluetoothLeService.writeMotor("right", 0);
        }
    }

    public void startCalibration(View v) {
        //TODO start the full screen web browser activity
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
