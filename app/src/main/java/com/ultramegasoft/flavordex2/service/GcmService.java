package com.ultramegasoft.flavordex2.service;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.ultramegasoft.flavordex2.util.BackendUtils;

/**
 * Service to handle GCM messages.
 *
 * @author Steve Guidetti
 */
public class GcmService extends GcmListenerService {
    private static final String TAG = "GcmService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i(TAG, "Received Message: " + data.getString("message"));
        BackendUtils.requestSync(this);
    }
}
