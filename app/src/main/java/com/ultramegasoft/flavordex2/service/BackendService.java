package com.ultramegasoft.flavordex2.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.registration.Registration;
import com.ultramegasoft.flavordex2.util.BackendUtils;

import java.io.IOException;

/**
 * Service for accessing the backend.
 *
 * @author Steve Guidetti
 */
public class BackendService extends IntentService {
    /**
     * Action value for broadcast Intents
     */
    public static final String ACTION_COMPLETED = "com.ultramegasoft.flavordex2.service.COMPLETE";

    /**
     * Keys for the Intent extras
     */
    private static final String EXTRA_COMMAND = "command";
    private static final String EXTRA_ACCOUNT_NAME = "account_name";

    /**
     * Commands this service will accept
     */
    private static final int COMMAND_REGISTER = 0;
    private static final int COMMAND_UNREGISTER = 1;
    private static final int COMMAND_SYNC = 2;

    /**
     * The API project number
     */
    private static final String PROJECT_NUMBER = "1001621163874";

    public BackendService() {
        super("BackendService");
    }

    /**
     * Register the client with the backend.
     *
     * @param context     The Context
     * @param accountName The name of the account to use
     */
    public static void registerClient(Context context, String accountName) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_REGISTER);
        intent.putExtra(EXTRA_ACCOUNT_NAME, accountName);
        context.startService(intent);
    }

    /**
     * Unregister the client from the backend.
     *
     * @param context The Context
     */
    public static void unregisterClient(Context context) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_UNREGISTER);
        context.startService(intent);
    }

    /**
     * Sync the journal data with the backend.
     *
     * @param context The Context
     */
    public static void syncData(Context context) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_SYNC);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch(intent.getIntExtra(EXTRA_COMMAND, -1)) {
            case COMMAND_REGISTER:
                doRegisterClient(intent.getStringExtra(EXTRA_ACCOUNT_NAME));
                break;
            case COMMAND_UNREGISTER:
                doUnregisterClient();
                break;
            case COMMAND_SYNC:
                doSyncData();
                break;
        }

        final Intent broadcastIntent = new Intent(ACTION_COMPLETED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    /**
     * Handle client registration.
     *
     * @param accountName The name of the account to use
     */
    private void doRegisterClient(String accountName) {
        if(accountName == null) {
            return;
        }

        final GoogleAccountCredential credential = BackendUtils.getCredential(this);
        credential.setSelectedAccountName(accountName);

        final InstanceID instanceID = InstanceID.getInstance(this);

        try {
            final String token =
                    instanceID.getToken(PROJECT_NUMBER, GoogleCloudMessaging.INSTANCE_ID_SCOPE);

            final Registration registration = BackendUtils.getRegistration(credential);
            final long clientId = registration.register(token).execute().getClientId();
            if(clientId > 0) {
                BackendUtils.setClientId(this, clientId);
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString(FlavordexApp.PREF_ACCOUNT_NAME, accountName)
                        .putBoolean(FlavordexApp.PREF_SYNC_DATA, true).apply();
            }
        } catch(IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Handle client unregistration.
     */
    private void doUnregisterClient() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        if(accountName == null) {
            return;
        }

        final GoogleAccountCredential credential = BackendUtils.getCredential(this);
        credential.setSelectedAccountName(accountName);

        final Registration registration = BackendUtils.getRegistration(credential);
        try {
            InstanceID.getInstance(this)
                    .deleteToken(PROJECT_NUMBER, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            registration.unregister(BackendUtils.getClientId(this)).execute();
        } catch(IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }

        BackendUtils.setClientId(this, 0);
        prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_DATA, false).apply();
    }

    /**
     * Handle journal data synchronization.
     */
    private void doSyncData() {
        // TODO: 10/18/2015 Implement syncing
    }
}
