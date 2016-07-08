package com.ultramegasoft.flavordex2.service;

import android.preference.PreferenceManager;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.BackendUtils;

/**
 * Service to handle InstanceID callbacks.
 *
 * @author Steve Guidetti
 */
public class InstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getBoolean(FlavordexApp.PREF_SYNC_DATA, false)) {
                    BackendUtils.registerClient(getApplicationContext());
                }
            }
        }).run();
    }
}
