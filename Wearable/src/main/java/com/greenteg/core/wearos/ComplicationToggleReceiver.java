/*
 * Copyright (C) 2021, greenTEG AG
 * info@CoreBodyTemp.com
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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

/**
 * Simple {@link BroadcastReceiver} subclass for asynchronously incrementing an integer for any
 * complication id triggered via TapAction on complication. Also, provides static method to create a
 * {@link PendingIntent} that triggers this receiver.
 */
public class ComplicationToggleReceiver extends BroadcastReceiver {
    private static final String TAG = ComplicationToggleReceiver.class.getSimpleName();

    private static final String EXTRA_PROVIDER_COMPONENT =
            "com.greenteg.core.wearos.action.PROVIDER_COMPONENT";
    private static final String EXTRA_COMPLICATION_ID =
            "com.greenteg.core.wearos.action.COMPLICATION_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() --> sending ProviderUpdaterequest now");
        Bundle extras = intent.getExtras();
        ComponentName provider = extras.getParcelable(EXTRA_PROVIDER_COMPONENT);
        int complicationId = extras.getInt(EXTRA_COMPLICATION_ID);

        // Request an update for the complication that has just been toggled.
        ProviderUpdateRequester requester = new ProviderUpdateRequester(context, provider);
        requester.requestUpdate(complicationId);
    }

    /**
     * Returns a pending intent, suitable for use as a tap intent, that causes a complication to be
     * toggled and updated.
     */
    static PendingIntent getToggleIntent(
            Context context, ComponentName provider, int complicationId) {
        Intent intent = new Intent(context, ComplicationToggleReceiver.class);
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, provider);
        intent.putExtra(EXTRA_COMPLICATION_ID, complicationId);

        // Pass complicationId as the requestCode to ensure that different complications get
        // different intents.
        Log.d(TAG, "getToggleIntent()");
        return PendingIntent.getBroadcast(
                context, complicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
