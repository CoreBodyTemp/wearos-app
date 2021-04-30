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
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.greenteg.core.wearos.models.TemperatureReading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.greenteg.core.wearos.R.layout.activity_core_body_temperature;

public class CoreBodyTemperatureActivity extends Activity {
    private final static String TAG = CoreBodyTemperatureActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    private double mTemperature;
    private TextView mConnectionState;
    private TextView mDataField;

    private ImageView mBattery0to9;
    private ImageView mBattery10to24;
    private ImageView mBattery25to49;
    private ImageView mBattery50to74;
    private ImageView mBattery75to100;

    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mTemperatureCharacteristic;
    private BluetoothGattCharacteristic mBatteryCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private ProgressBar mProgressBar;

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
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int battery_level;
            Log.d(TAG, "BroadcastReceiver: action "+action);

            switch (action) {
                case BluetoothLeService.ACTION_GATT_CONNECTED: {
                    updateConnectionState(R.string.connected);
                    setConnecting(false);
                    Toast.makeText(context, R.string.connected, Toast.LENGTH_SHORT).show();
                    displayTemperature();
                }
                break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED: {
                    updateConnectionState(R.string.disconnected);
                    clearUI();
                    //Toast.makeText(context, R.string.disconnected, Toast.LENGTH_SHORT).show();
                    setConnecting(true);
                    displayTemperature();
                }
                break;
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED: {
                    listGattServices(mBluetoothLeService.getSupportedGattServices());
                    if (mBatteryCharacteristic != null) {
                        readBattery();
                    } else {
                        // try to immediately set up notification for temperature
                        setTemperatureNotification(true);
                    }
                }
                break;
                case BluetoothLeService.ACTION_TEMPERATURE_AVAILABLE: {
                    //mTemperature = intent.getDoubleExtra(BluetoothLeService.EXTRA_TEMPERATURE_VALUE, 0);
                    displayTemperature();
                }
                break;
                case BluetoothLeService.ACTION_BATTERY_LEVEL_AVAILABLE: {
                    battery_level = intent.getIntExtra(BluetoothLeService.EXTRA_BATTERY_VALUE, -1);
                    Log.d(TAG, "received battery level: "+battery_level);
                    mConnectionState.setVisibility(View.INVISIBLE);
                    displayBattery(battery_level);
                    // setup notification on Temperature characteristic AFTER getting battery level:
                    setTemperatureNotification(true);
                }
                break;
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                            == BluetoothAdapter.STATE_OFF) {
                        updateConnectionState(R.string.disconnected);
                        clearUI();
                        displayTemperature();
                    }
                }
            }
        }
    };

    private void readBattery() {
        mBluetoothLeService.readCharacteristic(mBatteryCharacteristic);
    }

    private void setTemperatureNotification(boolean enable) {
        mBluetoothLeService.setCharacteristicNotification(mTemperatureCharacteristic, enable);
    }

    private void setConnecting(boolean enabled) {
        Log.d(TAG, "setConnecting to: "+ enabled);
        if (enabled) {
            mProgressBar.setVisibility(View.VISIBLE);
            //keep screen during "connecting..." (it's annoying if the user cannot check whether the connection attempt was successful
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_core_body_temperature); // we display only the CBT in this version!
        mProgressBar = findViewById(R.id.progress_bar_core_body_temperature_activity);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.current_CBT);

        mBattery0to9 = (ImageView) findViewById(R.id.battery0to9);
        mBattery10to24 = (ImageView) findViewById(R.id.battery10to24);
        mBattery25to49 = (ImageView) findViewById(R.id.battery25to49);
        mBattery50to74 = (ImageView) findViewById(R.id.battery50to74);
        mBattery75to100 = (ImageView) findViewById(R.id.battery75to100);

        mDataField.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                toggleTemperatureUnits();
            }
        });
        Log.d(TAG, "start BluetoothLeService Intent next.");
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mTemperature = 0;
        setConnecting(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayTemperature();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        // to be sure, make all battery icons invisible:
        mBattery0to9.setVisibility(View.INVISIBLE);
        mBattery10to24.setVisibility(View.INVISIBLE);
        mBattery25to49.setVisibility(View.INVISIBLE);
        mBattery50to74.setVisibility(View.INVISIBLE);
        mBattery75to100.setVisibility(View.INVISIBLE);

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
            if (!result) {
                finish();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
            // try to read battery value:
            if (mBatteryCharacteristic != null) {
                readBattery();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        new ProviderUpdateRequester(
                this, new ComponentName(this, CbtComplicationProviderService.class))
                .requestUpdateAll();
        Log.d(TAG, "onPause() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        // we will no longer get updates for CBT and it will soon be outdated, thus set it to 0
        AppPreferences.setLastCbtValue(CoreBodyTemperatureActivity.this,0);
        mBluetoothLeService = null;
        Log.d(TAG, "onDestroy() called.");
    }

    private void updateConnectionState(final int resourceId) {
        mConnectionState.setText(resourceId);
    }

    private void displayTemperature() {
        mTemperature = AppPreferences.getLastCbtValue(this);
        if (mTemperature != 0) {
            String value;

            switch (AppPreferences.getTemperatureUnit(this)) {
                case CELSIUS:
                    value = String.format("%.2f %s", mTemperature, "°C");
                    break;
                case FAHRENHEIT:
                    value = String.format("%.2f %s", TemperatureReading.celsiusToFahrenheit(mTemperature), "°F");
                    break;
                default:
                    throw new RuntimeException("Unknown temperature unit");
            }

            mDataField.setText(value);
            Log.d(TAG, "setting temperature on display to: " + value);
        } else {
            mDataField.setText(R.string.no_data);
            Log.d(TAG, "setting temperature on display to: " + "No data...");
        }
    }

    private void displayBattery(int battery_level) {
        if (battery_level>=0 & battery_level<=10) {
            mBattery0to9.setVisibility(View.VISIBLE);
        } else if (battery_level>=10 & battery_level<=24) {
            mBattery10to24.setVisibility(View.VISIBLE);
        } else if (battery_level>=25 & battery_level<=49) {
            mBattery25to49.setVisibility(View.VISIBLE);
        } else if (battery_level>=50 & battery_level<=74) {
            mBattery50to74.setVisibility(View.VISIBLE);
        } else if (battery_level>=75 & battery_level<=100) {
            mBattery75to100.setVisibility(View.VISIBLE);
        } else {
            Log.i(TAG, "battery level not in range 0 to 100");
        }
    }

    private void listGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, CoreGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                String mGattAttribute = CoreGattAttributes.lookup(uuid, unknownCharaString);
                Log.d(TAG, "gattAttribute: " + mGattAttribute);
                // extract interesting characteristics (Battery level and Body Temperature)
                if (mGattAttribute.equals("Temperature Measurement")) {
                    final int charaProp = gattCharacteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mTemperatureCharacteristic = gattCharacteristic;
                    }
                }
                if (mGattAttribute.equals("Battery Level")) {
                    mBatteryCharacteristic = gattCharacteristic;
                }

                currentCharaData.put(
                        LIST_NAME, mGattAttribute);
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    public void disconnectClicked(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.confirm_forget_clicked)
                .setCancelable(false)
                .setPositiveButton(R.string.confirm_forget_clicked_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CoreBodyTemperatureActivity.this.disconnectConfirmed();
                    }
                })
                .setNegativeButton(R.string.confirm_forget_clicked_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void disconnectConfirmed() {
        AppPreferences.removeDevice(this);
        if (mBluetoothLeService!=null) {
            mBluetoothLeService.disconnect();
        }
        finish();
    }

    private void toggleTemperatureUnits() {
        switch (AppPreferences.getTemperatureUnit(this)) {
            case CELSIUS:
                AppPreferences.saveTemperatureUnit(this, AppPreferences.TemperatureUnit.FAHRENHEIT);
                break;
            case FAHRENHEIT:
                AppPreferences.saveTemperatureUnit(this, AppPreferences.TemperatureUnit.CELSIUS);
                break;
            default:
                throw new RuntimeException("Unknown temperature unit");
        }

        displayTemperature();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMPERATURE_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BATTERY_LEVEL_AVAILABLE);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        return intentFilter;
    }
}
