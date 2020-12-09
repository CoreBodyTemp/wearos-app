/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

/**
 * IMPORTANT NOTE: Most apps shouldn't use always on ambient mode, as it drains battery life. Unless
 * required, it's much better to allow the system to take over after the user stops interacting
 * with your app.
 * <p>
 * Demonstrates support for <i>Ambient Mode</i> by attaching ambient mode support to the activity,
 * and listening for ambient mode updates (onEnterAmbient, onUpdateAmbient, and onExitAmbient) via a
 * named AmbientCallback subclass.
 *
 * <p>Also demonstrates how to update the display more frequently than every 60 seconds, which is
 * the default frequency, using an AlarmManager. The Alarm code is only necessary for the custom
 * refresh frequency; it can be ignored for basic ambient mode support where you can simply rely on
 * calls to onUpdateAmbient() by the system.
 *
 * <p>There are two modes: <i>ambient</i> and <i>active</i>. To trigger future display updates, we
 * use a custom Handler for active mode and an Alarm for ambient mode.
 *
 * <p>Why not use just one or the other? Handlers are generally less battery intensive and can be
 * triggered every second. However, they can not wake up the processor (common in ambient mode).
 *
 * <p>Alarms can wake up the processor (what we need for ambient move), but they are less efficient
 * compared to Handlers when it comes to quick update frequencies.
 *
 * <p>Therefore, we use Handler for active mode (can trigger every second and are better on the
 * battery), and we use an Alarm for ambient mode (only need to update once every 10 seconds and
 * they can wake up a sleeping processor).
 *
 * <p>The activity waits 10 seconds between doing any processing (getting data, updating display
 * etc.) while in ambient mode to conserving battery life (processor allowed to sleep). If your app
 * can wait 60 seconds for display updates, you can disregard the Alarm code and simply use
 * onUpdateAmbient() to save even more battery life.
 *
 * <p>As always, you will still want to apply the performance guidelines outlined in the Watch Faces
 * documentation to your app.
 *
 * <p>Finally, in ambient mode, this activity follows the same best practices outlined in the Watch
 * Faces API documentation: keeping most pixels black, avoiding large blocks of white pixels, using
 * only black and white, disabling anti-aliasing, etc.
 */
public class MainActivity<REQUEST_ENABLE_BT> extends FragmentActivity {

    private static final String TAG = "MainActivity";

    //for permission request and turn-on-bluetooth request
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private TextView mRationaleTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRationaleTextView = findViewById(R.id.textview_permission_rationale);
    }

    public void startScanButtonClicked(View view) {
        //Ask for ACCESS_FINE_LOCATION permission if necessary:
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            startActivity(intent);
            connectToSavedDevice();
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
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
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mRationaleTextView.setText(R.string.location_permission_rationale_no_show);
        } else {
            mRationaleTextView.setText(R.string.location_permission_rationale);
        }

        connectToSavedDevice();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    private void connectToSavedDevice() {
        String deviceAddress = AppPreferences.getDeviceAddress(this);
        if (deviceAddress != null) {
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