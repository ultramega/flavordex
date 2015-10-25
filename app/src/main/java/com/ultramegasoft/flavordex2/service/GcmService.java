package com.ultramegasoft.flavordex2.service;

import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Service to handle GCM messages.
 *
 * @author Steve Guidetti
 */
public class GcmService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle data) {
        BackendService.syncData(this);
    }
}
