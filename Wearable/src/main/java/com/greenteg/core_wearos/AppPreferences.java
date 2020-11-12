package com.greenteg.core_wearos;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREFERENCES = "PREFERENCES";
    private static final String DEVICE_ADDRESS_PREFERENCE = "DEVICE_ADDRESS_PREFERENCE";

    private AppPreferences() {

    }

    public static void saveDevice(Context context, BluetoothDevice device) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(DEVICE_ADDRESS_PREFERENCE, device.getAddress());
        editor.apply();
    }

    public static String getDeviceAddress(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getString(DEVICE_ADDRESS_PREFERENCE, null);
    }

    public static void removeDevice(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(DEVICE_ADDRESS_PREFERENCE);
        editor.apply();
    }
}
