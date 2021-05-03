/*
 * Copyright (C) 2013 The Android Open Source Project
 * Modification copyright (C) 2021, greenTEG AG
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

package com.greenteg.core.wearos;

import android.app.Activity;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED;

public class DeviceScanActivity extends Activity {
    private static final String TAG = "DeviceScanActivity";

    private BluetoothAdapter mBluetoothAdapter;
    // used to start/stop scan:
    private BluetoothLeScanner mBluetoothLeScanner =
            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    private List<ScanFilter> mFilters = new ArrayList<>();  // not used currently
    private ScanSettings mSettings;
    private ProgressBar mProgressBar;

    private final Handler mScanningTimeout = new scanningTimeoutHandler(this);
    /**
     * Custom 'what' for Message sent to Handler.
     */
    private static final int MSG_SCANNING_TIMEOUT = 0;
    private static final int scanningTimeout_Ms = 10000;



    private boolean mScanning;

    private LeDeviceListAdapter mDeviceListAdapter;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();

    private static final int REQUEST_ENABLE_BT = 1;

    private void setScanning(boolean enabled) {
        mScanning = enabled;
        if (enabled && mDeviceListAdapter.getItemCount() == 0) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void displayScanningTimeout() {
        Log.d(TAG, "displayScanningTimeout()");
        scanLeDevice(false);
        Toast.makeText(DeviceScanActivity.this, R.string.scanning_timed_out, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(DeviceScanActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        Log.d(TAG, "onCreate()");
        mProgressBar = findViewById(R.id.progress_bar);

        mDeviceListAdapter = new LeDeviceListAdapter(mDeviceList);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(mDeviceListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager((this)));

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

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Log.d(TAG, "line after bluetooth enable intent.");

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (mBluetoothAdapter.isEnabled()) {
            mDeviceList.clear();
            mDeviceListAdapter.notifyDataSetChanged();
            scanLeDevice(true);
        }
        else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            Log.w(TAG, "user enabled BT.");
            Intent intent = new Intent(DeviceScanActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mDeviceList.clear();
        mDeviceListAdapter.notifyDataSetChanged();
    }

    /**
     * Enables or disables scanning for Bluetooth LE devices.
     * @param enable If true, enable scanning. False otherwise.
     */
    private void scanLeDevice(final boolean enable) {
        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner =
                    BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        }
        if (enable) {
            if(!mBluetoothAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            mSettings = new ScanSettings.Builder().setScanMode(SCAN_MODE_BALANCED).build();  //default settings

            setScanning(true);
            Log.d(TAG, "scanLeDevice: startLeScan.");
            mBluetoothLeScanner.startScan(mFilters, mSettings, mScanCallback);

            mScanningTimeout.sendEmptyMessageDelayed(MSG_SCANNING_TIMEOUT, scanningTimeout_Ms);

        } else {
            setScanning(false);
            if(mBluetoothLeScanner != null && mBluetoothAdapter.isEnabled()) {
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        final Intent intent = new Intent(this, CoreBodyTemperatureActivity.class);
        intent.putExtra(CoreBodyTemperatureActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(CoreBodyTemperatureActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        AppPreferences.saveDevice(this, device);

        if (mScanning) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            setScanning(false);
        }
        String myString = getString(R.string.connecting_to) + " " + device.getAddress();
        Toast.makeText(this, myString, Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }


    // Adapter for holding devices found through scanning.
    class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView deviceName;
            TextView deviceAddress;
            BluetoothDevice bleDevice;

            public ViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                deviceName = itemView.findViewById(R.id.device_name);
                deviceAddress = itemView.findViewById(R.id.device_address);
            }

            @Override
            public void onClick(View view) {
                connectToDevice(bleDevice);
            }
        }

        private final ArrayList<BluetoothDevice> mLeDevices;
        private final LayoutInflater mInflater;

        public LeDeviceListAdapter(ArrayList<BluetoothDevice> leDevices) {
            super();
            mLeDevices = leDevices;
            mInflater = DeviceScanActivity.this.getLayoutInflater();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.listitem_device, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(LeDeviceListAdapter.ViewHolder holder, int position) {
            // Get the data model based on position
            BluetoothDevice device = mLeDevices.get(position);

            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                holder.deviceName.setText(deviceName);
            } else {
                holder.deviceName.setText(R.string.unknown_device);
            }

            holder.bleDevice = device;
            holder.deviceAddress.setText(device.getAddress());
        }

        @Override
        public int getItemCount() {
            return mLeDevices.size();
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            Log.d(TAG, "address: " + device.getAddress() + "name: " + name);

            String addressToConnect = AppPreferences.getDeviceAddress(DeviceScanActivity.this);

            if (addressToConnect != null) {
                if (device.getAddress().equals(addressToConnect)) {
                    connectToDevice(device);
                    return;
                }
            }

            if (name != null && name.contains("CORE")) {
                if (!mDeviceList.contains(device)) {
                    mDeviceList.add(device);
                    mDeviceListAdapter.notifyItemInserted(mDeviceList.size() - 1);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    // LOG message MOT
                    Log.d(TAG, "adding a device");
                } else {
                    Log.d(TAG, "device" + device.getAddress() + " already in list.");
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed() errorCode: "+ errorCode);
        };
    };

    /**
     * Handler separated into static class to avoid memory leaks.
     */
    private static class scanningTimeoutHandler extends Handler {
        private final WeakReference<DeviceScanActivity> mDeviceScanActivityWeakReference;

        scanningTimeoutHandler(DeviceScanActivity reference) {
            mDeviceScanActivityWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message message) {
            DeviceScanActivity deviceScanActivity = mDeviceScanActivityWeakReference.get();

            if (deviceScanActivity != null) {
                if (message.what == MSG_SCANNING_TIMEOUT) {
                    if (deviceScanActivity.mDeviceListAdapter.getItemCount() == 0 && deviceScanActivity.mScanning == true) {
                        deviceScanActivity.displayScanningTimeout();
                    }
                }
            }
        }
    }

}
