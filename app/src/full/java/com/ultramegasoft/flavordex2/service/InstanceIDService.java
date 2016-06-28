package com.ultramegasoft.flavordex2.service;

import com.google.firebase.iid.FirebaseInstanceIdService;
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
                BackendUtils.registerClient(getApplicationContext());
            }
        }).run();
    }
}
