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
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(getClass().getSimpleName(), "Received Message: " + data.getString("message"));
        BackendUtils.requestSync(this);
    }
}
