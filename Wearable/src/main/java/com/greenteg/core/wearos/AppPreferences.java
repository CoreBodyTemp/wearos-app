/*
 * Copyright (C) 2021, greenTEG AG
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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Locale;

public class AppPreferences {
    private static final String TAG = AppPreferences.class.getSimpleName();

    public static final String PREFERENCES = "PREFERENCES";
    private static final String DEVICE_ADDRESS_PREFERENCE = "DEVICE_ADDRESS_PREFERENCE";
    public static final String LAST_CBT_VALUE = "LAST_CBT_VALUE";
    public static final String LAST_COMPLICATION_CBT_VALUE = "LAST_COMPLICATION_CBT_VALUE";
    private static final String TEMPERATURE_UNIT_PREFERENCE = "TEMPERATURE_UNIT_PREFERENCE";

    enum TemperatureUnit {
        CELSIUS("C"),
        FAHRENHEIT("F");

        private final String mCode;

        TemperatureUnit(String code) {
            mCode = code;
        }

        public static TemperatureUnit getByCode(String code) {
            switch(code) {
                case "C":
                    return CELSIUS;
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

    public static void setLastCbtValue(Context context, float lastCbtValue){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(LAST_CBT_VALUE, lastCbtValue);
        editor.apply();
        Log.d(TAG,"set lastcbtvalue to "+lastCbtValue);
    }

    public static void setLastComplicationValue(Context context, float lastCbtValue){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(LAST_COMPLICATION_CBT_VALUE, lastCbtValue);
        editor.apply();
        Log.d(TAG,"set lastComplicationcbtvalue to "+lastCbtValue);
    }

    public static float getLastCbtValue(Context context){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getFloat(LAST_CBT_VALUE, 0.0f);
    }

    public static float getLastComplicationCbtValue(Context context){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getFloat(LAST_COMPLICATION_CBT_VALUE, 0.0f);
    }
}
