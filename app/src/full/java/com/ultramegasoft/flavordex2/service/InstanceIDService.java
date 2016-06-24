package com.ultramegasoft.flavordex2.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.util.BackendUtils;

/**
 * Service to handle InstanceID callbacks.
 *
 * @author Steve Guidetti
 */
public class InstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        if(accountName != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BackendUtils.registerClient(getApplicationContext(), accountName);
                }
            }).run();
        }
    }
}
