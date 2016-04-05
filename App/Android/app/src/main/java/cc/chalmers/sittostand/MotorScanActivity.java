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
 * Edited by Iain Chalmers, Copyright (C) 2016
 *
 * Based on com.example.android.bluetoothlegatt.DeviceScanActivity.java Android sample code
 */

package cc.chalmers.sittostand;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.Manifest;

import static java.util.jar.Manifest.*;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class MotorScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner scanner;
    private boolean mScanning;
    private Handler mHandler;
    private static boolean firstMotorSelected = false;
    private static BluetoothDevice firstMotorDevice;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("Select left vibrator");
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        scanner = mBluetoothAdapter.getBluetoothLeScanner();
        checkLocationPermissions();
    }

    private final int PERMISSION_LOCATION_REQUEST_CODE = 1;
    private void checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= 23){
            int hasLocationPermission = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            if( hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(
                        new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_LOCATION_REQUEST_CODE);
            } else {
                scanLeDevice(true);
            }
        } else {
            scanLeDevice(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanLeDevice(true);
        } else {
            Toast.makeText(this,"NEED LOCATION PERMISSIONS", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        firstMotorSelected = false;
        getActionBar().setTitle("Select left vibrator");
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        firstMotorSelected = false;
        getActionBar().setTitle("Select left vibrator");
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {

        // TODO Need to check bluetooth is enabled better than just checking the scanner isnt null
        if (scanner == null) {
            return;
        }
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                    ScanFilter filter = new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(RFDuinoGATTServices.RFDUINO_SERVICE))
                            .build();
                    List<ScanFilter> filterList = new ArrayList<>();
                    filterList.add(filter);
                    scanner.startScan(filterList, settings, mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            scanner.stopScan(mLeScanCallback);
        } else {
            mScanning = false;
            scanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(!firstMotorSelected) {
            firstMotorDevice = mLeDeviceListAdapter.getDevice(position);
            if (firstMotorDevice == null) return;
            mLeDeviceListAdapter.removeDevice(firstMotorDevice);
            getActionBar().setTitle("Select right vibrator");
            firstMotorSelected = true;
        }
        else {
            final BluetoothDevice secondDevice = mLeDeviceListAdapter.getDevice(position);
            if (secondDevice == null) return;
            final Intent intent = new Intent(this, MotorControlActivity.class);
            intent.putExtra(MotorControlActivity.EXTRAS_DEVICE1_NAME, firstMotorDevice.getName());
            intent.putExtra(MotorControlActivity.EXTRAS_DEVICE1_ADDRESS, firstMotorDevice.getAddress());
            intent.putExtra(MotorControlActivity.EXTRAS_DEVICE2_NAME, secondDevice.getName());
            intent.putExtra(MotorControlActivity.EXTRAS_DEVICE2_ADDRESS, secondDevice.getAddress());
            if (mScanning) {
                scanLeDevice(false);
            }
            startActivity(intent);
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MotorScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public void removeDevice(BluetoothDevice device) {
            if(mLeDevices.contains(device)) {
                mLeDevices.remove(mLeDevices.indexOf(device));
                this.notifyDataSetChanged();
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(result.getDevice());
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
