/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    //for permission request and turn-on-bluetooth request
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private TextView mRationaleTextView;
    private boolean assumeLocationPermission = true;


    private void checkAndRequestPermissions() {
        //Ask for ACCESS_FINE_LOCATION permission if necessary:
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            startActivity(intent);
            connectToSavedDevice();
        } else {
            // You can directly ask for the permission.
            Log.d(TAG, "permission fine location not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRationaleTextView = findViewById(R.id.textview_permission_rationale);
    }

    public void startScanButtonClicked(View view) {
        checkAndRequestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, DeviceScanActivity.class);
                    startActivity(intent);
                }
            } else {
                Log.d(TAG, "location permission not granted, ask again");
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showDialogOK(R.string.location_permission_denied,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        checkAndRequestPermissions();
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        // proceed with logic by disabling the related features or quit the app.
                                        break;
                                }
                            }
                        });
                } else {
                    Toast.makeText(this, R.string.location_permission_denied_for_good, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showDialogOK(int resID, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(resID)
                .setPositiveButton(R.string.location_dialogue_positive, okListener)
                .setNegativeButton(R.string.location_dialogue_negative, okListener)
                .create()
                .show();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mRationaleTextView.setText(R.string.location_permission_rationale_no_show);
        } else {
            mRationaleTextView.setText(R.string.location_permission_rationale);
            assumeLocationPermission = false;
        }

        connectToSavedDevice();
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    private void connectToSavedDevice() {
        String deviceAddress = AppPreferences.getDeviceAddress(this);
        if (deviceAddress != null && assumeLocationPermission) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            startActivity(intent);
        }
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
}