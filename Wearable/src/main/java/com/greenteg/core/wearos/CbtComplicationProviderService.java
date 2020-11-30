package com.greenteg.core.wearos;

import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import com.greenteg.core.wearos.models.TemperatureReading;


public class CbtComplicationProviderService extends android.support.wearable.complications.ComplicationProviderService {
    private final static String TAG = CbtComplicationProviderService.class.getSimpleName();

    @Override
    public void onComplicationUpdate(
            int complicationId, int dataType, ComplicationManager complicationManager) {

        Log.d(TAG, "onComplicationUpdate() id: " + complicationId);

        float lastCbtValue = AppPreferences.getLastCbtValue(CbtComplicationProviderService.this);
        float lastComplicationCbtValue = AppPreferences.getLastComplicationCbtValue(CbtComplicationProviderService.this);
        Log.d(TAG, "last: " + lastCbtValue + " oldlast: " + lastComplicationCbtValue);

        ComplicationData complicationData = null;

        if(lastCbtValue!=lastComplicationCbtValue){
            // we need to update
            AppPreferences.setLastComplicationValue(CbtComplicationProviderService.this, lastCbtValue);
            Log.d(TAG, "updating lastCbtValue to: " + lastCbtValue);

            String value = new String();
            switch (dataType) {
                case ComplicationData.TYPE_SHORT_TEXT:
                    if (lastCbtValue==0){
                        value = "--";
                    }
                    else {
                        switch (AppPreferences.getTemperatureUnit(this)) {
                            case CELSIUS:
                                value = String.format("%.1f %s", lastCbtValue, "°C");
                                break;
                            case FAHRENHEIT:
                                value = String.format("%.1f %s", TemperatureReading.celsiusToFahrenheit(lastCbtValue), "°F");
                                break;
                            default:
                                throw new RuntimeException("Unknown temperature unit");
                        }
                    }
                    complicationData =
                            new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                    .setShortText(ComplicationText.plainText(value))
                                    .build();

                    break;
                case ComplicationData.TYPE_LONG_TEXT:
                    if (lastCbtValue==0){
                        value="no data";
                    }
                    else {
                        switch (AppPreferences.getTemperatureUnit(this)) {
                            case CELSIUS:
                                value = String.format("%.2f %s", lastCbtValue, "°C");
                                break;
                            case FAHRENHEIT:
                                value = String.format("%.2f %s", TemperatureReading.celsiusToFahrenheit(lastCbtValue), "°F");
                                break;
                            default:
                                throw new RuntimeException("Unknown temperature unit");
                        }
                    }
                    complicationData =
                            new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                    .setLongText(ComplicationText.plainText(value))
                                    .setIcon(Icon.createWithResource(this, R.drawable.icn_cbt_complications))
                                    .build();
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Unexpected complication type " + dataType);
                    }
            }
        }

        if (complicationData != null) {
            complicationManager.updateComplicationData(complicationId, complicationData);

        } else {
            // If no data is sent, we still need to inform the ComplicationManager, so
            // the update job can finish and the wake lock isn't held any longer.
            complicationManager.noUpdateRequired(complicationId);
        }
    }
}
