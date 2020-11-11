package com.greenteg.core_wearos.models;

import android.bluetooth.BluetoothGattCharacteristic;
import java.util.Calendar;

public class TemperatureReading {
    public enum Type {
        CELSIUS,
        FAHRENHEIT
    }

    public static double fromCharacteristic(BluetoothGattCharacteristic characteristic) {
        int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        Type type = (flags & 0x01) > 0 ? Type.FAHRENHEIT : Type.CELSIUS;
        int tempData = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1); //ByteBuffer.wrap(data, 1, 4).getInt();
        int exponent = tempData >> 24;
        int mantissa = tempData & 0x00FFFFFF;
        double actualTemp = (double) mantissa * Math.pow(10, exponent);
        long time;
        if ((flags & 0x02) > 0) {
            int year = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
            int month = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 7);
            int day = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 8);
            int hour = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 9);
            int min = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 10);
            int sec = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 11);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, sec);
            cal.set(Calendar.MILLISECOND, 0);
            time = cal.getTimeInMillis();
        } else {
            time = System.currentTimeMillis();
        }

        switch (type) {
            case CELSIUS:
                return actualTemp;
            case FAHRENHEIT:
                return fahrenheitToCelsius(actualTemp);
        }

        return 0;
    }

    public static double fahrenheitToCelsius(double fTemp) {
        return (fTemp - 32.0f) * 5.0f / 9.0f;
    }

    public static double celsiusToFahrenheit(double cTemp) {
        return cTemp * 9.0f / 5.0f + 32.0f;
    }
}
