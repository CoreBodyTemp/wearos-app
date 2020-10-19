package com.example.android.wearable.wear.core_wearos.models;

import android.bluetooth.BluetoothGattCharacteristic;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*
Modified from https://github.com/SiliconLabs/EFRConnect-android under Apache 2.0
 */

public class TemperatureReading {
    public enum Type {
        CELSIUS(0f, 50f), FAHRENHEIT(32, 122);

        Type(float normalizedMin, float normalizedMax) {
            this.normalizedMin = normalizedMin;
            this.normalizedMax = normalizedMax;
        }

        public float normalizedMin;
        public float normalizedMax;

        public float getRange() {
            return normalizedMax - normalizedMin;
        }
    }

    // skip HtmType for now
    /*public enum HtmType {
        UNKNOWN(R.string.unknown),
        ARMPIT(R.string.therm_type_armpit),
        BODY(R.string.therm_type_body),
        EAR(R.string.therm_type_ear),
        FINGER(R.string.therm_type_finger),
        GI_TRACT(R.string.therm_type_gi_tract),
        MOUTH(R.string.therm_type_mouth),
        RECTUM(R.string.therm_type_rectum),
        TOE(R.string.therm_type_toe),
        TYMPANUM(R.string.therm_type_tympanum);

        private int nameResId;

        HtmType(int nameResId) {
            this.nameResId = nameResId;
        }

        public int getNameResId() {
            return nameResId;
        }
    }*/

    private Type type;
    //private HtmType htmType;
    private double temperature;
    private double normalizedTemperature;
    private long readingTime;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());


    /*public void setHtmType(HtmType htmType) {
        this.htmType = htmType;
    }*/

    public TemperatureReading(Type type, double temperature, long readingTime) {
        this.type = type;
        this.temperature = temperature;
        this.readingTime = readingTime;
        if (temperature > type.normalizedMax) {
            normalizedTemperature = type.normalizedMax;
        } else if (temperature < type.normalizedMin) {
            normalizedTemperature = type.normalizedMin;
        } else {
            normalizedTemperature = temperature;
        }
    }

    public static TemperatureReading fromCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
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
        return new TemperatureReading(type, actualTemp, time);
    }

    public String getFormattedTime() {
        return dateFormat.format(new Date(readingTime));
    }

    public Type getType() {
        return type;
    }

    /*public HtmType getHtmType() {
        return htmType;
    }*/

    public double getTemperature() {
        return temperature;
    }

    public double getTemperature(Type fetchType) {
        if (fetchType == this.type) {
            return temperature;
        } else if (fetchType == Type.CELSIUS) {
            return fahrenheitToCelsius(temperature);
        }
        return celsiusToFahrenheit(temperature);
    }

    public long getReadingTime() {
        return readingTime;
    }

    public double getNormalizedTemperature() {
        return normalizedTemperature;
    }

    //Dummy data
    public static TemperatureReading getSampleReading() {
        long time = System.currentTimeMillis() - (long) (Math.random() * 6000000);
        float temp = (float) (Math.random() * Type.FAHRENHEIT.getRange()) + Type.FAHRENHEIT.normalizedMin;
        if (time % 3 == 0) {
            temp = Type.FAHRENHEIT.normalizedMax - 15 + (float) (Math.random() * 50);
        }
        return new TemperatureReading(Type.FAHRENHEIT, temp, time);
    }

    public static double fahrenheitToCelsius(double fTemp) {
        return (fTemp - 32.0f) * 5.0f / 9.0f;
    }

    public static double celsiusToFahrenheit(double cTemp) {
        return cTemp * 9.0f / 5.0f + 32.0f;
    }
}
