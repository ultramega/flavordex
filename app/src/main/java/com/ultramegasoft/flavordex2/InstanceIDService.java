package com.ultramegasoft.flavordex2;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Service to handle InstanceID callbacks.
 *
 * @author Steve Guidetti
 */
public class InstanceIDService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        if(accountName != null) {
            BackendService.registerClient(this, accountName);
        }
    }
}
