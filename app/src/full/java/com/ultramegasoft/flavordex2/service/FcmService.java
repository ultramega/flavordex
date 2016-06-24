package com.ultramegasoft.flavordex2.service;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ultramegasoft.flavordex2.util.BackendUtils;

/**
 * Service to handle FCM messages.
 *
 * @author Steve Guidetti
 */
public class FcmService extends FirebaseMessagingService {
    private static final String TAG = "FcmService";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.i(TAG, "Received Message: " + message.getData().get("message"));
        BackendUtils.requestDataSync(this);
    }
}
