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

package com.greenteg.core_wearos;

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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class DeviceScanActivity extends ListActivity {
    private static final String TAG = "DeviceScanActivity";

    public static final String CONNECT_TO_DEVICE_ADDRESS_ACTION = "CONNECT_TO_DEVICE_ADDRESS_ACTION";

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    // used to start/stop scan:
    private BluetoothLeScanner bluetoothLeScanner =
            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    // for filtering:
    public final static UUID UUID_TEMPERATURE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.TEMPERATURE_MEASUREMENT);
    private List<ScanFilter> mFilters = new ArrayList<>();  // not used currently
    private ScanSettings settings;


    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 30000;  // MOT changed

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("deviceScanActivity", "onCreate()");
        super.onCreate(savedInstanceState);
        //getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
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
        mLeDeviceListAdapter.clear();
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;

        connectToDevice(device);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            ScanFilter uuidFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID_TEMPERATURE_MEASUREMENT)).build();
            // don't add uuid filter because it doesn't work
            //mFilters.add(uuidFilter);
            settings = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).build();  //default settings
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    // replaced because deprecated:
                    bluetoothLeScanner.stopScan(mScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            Log.d("deviceScanActivity", "scanLeDevice: startLeScan.");
            //mBluetoothAdapter.startLeScan(mLeScanCallback);
            // replaced because deprecated:
            bluetoothLeScanner.startScan(mFilters, settings, mScanCallback);
        } else {
            mScanning = false;
            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
            // replaced because deprecated:
            bluetoothLeScanner.stopScan(mScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void connectToDevice(BluetoothDevice device) {
        final Intent intent = new Intent(this, CBTDeviceControlActivity.class);
        intent.putExtra(CBTDeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(CBTDeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        AppPreferences.saveDevice(this, device);

        if (mScanning) {
            bluetoothLeScanner.stopScan(mScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    // Adapter for holding devices found through scanning.
    class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
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

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            Log.d(TAG, "address: " + device.getAddress() + "name: " + name);

            String addressToConnect = getIntent().getExtras().getString(CONNECT_TO_DEVICE_ADDRESS_ACTION);

            if (addressToConnect != null) {
                if (device.getAddress().equals(addressToConnect)) {
                    connectToDevice(device);
                    return;
                }
            }

            if (name != null && name.contains("CORE")) {
                mLeDeviceListAdapter.addDevice(device);

                // LOG message MOT
                Log.d("deviceScanActivity", "adding a device");
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
