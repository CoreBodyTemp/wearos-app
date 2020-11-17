package com.greenteg.core_wearos;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Locale;

public class AppPreferences {

    public static final String PREFERENCES = "PREFERENCES";
    private static final String DEVICE_ADDRESS_PREFERENCE = "DEVICE_ADDRESS_PREFERENCE";
    public static final String LAST_CBT_VALUE = "LAST_CBT_VALUE";
    private static final String TEMPERATURE_UNIT_PREFERENCE = "TEMPERATURE_UNIT_PREFERENCE";

    enum TemperatureUnit {
        CELCIUS("C"),
        FAHRENHEIT("F");

        private final String mCode;

        TemperatureUnit(String code) {
            mCode = code;
        }

        public static TemperatureUnit getByCode(String code) {
            switch(code) {
                case "C":
                    return CELCIUS;
                case "F":
                    return FAHRENHEIT;
                default:
                    throw new RuntimeException("Unknown enum value");
            }
        }
    }

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

    public static void saveTemperatureUnit(Context context, TemperatureUnit unit) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TEMPERATURE_UNIT_PREFERENCE, unit.mCode);
        editor.apply();
    }

    public static TemperatureUnit getTemperatureUnit(Context context) {
        Locale locale = Locale.getDefault();
        String defaultTemperature = "C";

        if (locale.equals(Locale.US)) {
            defaultTemperature = "F";
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return TemperatureUnit.getByCode(preferences.getString(TEMPERATURE_UNIT_PREFERENCE, defaultTemperature));
    }

    public static void setLastCbtValue(Context context, float mCBT){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(LAST_CBT_VALUE, mCBT);
        editor.apply();
        Log.d("apppref","set lastcbtvalue to "+mCBT);
    }

    public static float getLastCbtValue(Context context){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getFloat(LAST_CBT_VALUE, 0);
    }
}
