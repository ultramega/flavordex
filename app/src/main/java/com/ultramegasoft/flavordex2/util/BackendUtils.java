package com.ultramegasoft.flavordex2.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.iid.InstanceID;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.registration.Registration;
import com.ultramegasoft.flavordex2.backend.sync.Sync;
import com.ultramegasoft.flavordex2.service.TaskService;

import java.io.IOException;

/**
 * Helpers for accessing the backend.
 *
 * @author Steve Guidetti
 */
public class BackendUtils {
    private static final String TAG = "BackendUtils";

    /**
     * Task tags for the GCM Network Manager
     */
    public static final String TASK_SYNC_DATA = "sync_data";

    /**
     * Keys for the backend shared preferences
     */
    private static final String PREFS_KEY = "backend";
    private static final String PREF_CLIENT_ID = "pref_client_id";
    private static final String PREF_DATA_SYNC_REQUESTED = "pref_data_sync_requested";
    private static final String PREF_PHOTO_SYNC_REQUESTED = "pref_photo_sync_requested";

    /**
     * The API project number
     */
    private static final String PROJECT_NUMBER = "1001621163874";

    /**
     * The API project ID
     */
    private static final String PROJECT_ID = "flavordex";

    /**
     * The project Web client ID
     */
    private static final String WEB_CLIENT_ID = "1001621163874-su48pt09eaj7rd4g0mni19ag4vv2g7p7.apps.googleusercontent.com";

    /**
     * Schedule the sync service to run.
     *
     * @param context The Context
     */
    public static void setupSync(Context context) {
        final PeriodicTask task = new PeriodicTask.Builder()
                .setTag(TASK_SYNC_DATA)
                .setUpdateCurrent(true)
                .setPeriod(30)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setService(TaskService.class)
                .build();
        GcmNetworkManager.getInstance(context).schedule(task);

        requestDataSync(context);
        requestPhotoSync(context);
    }

    /**
     * Request a data sync.
     *
     * @param context The Context
     */
    public static void requestDataSync(Context context) {
        requestDataSync(context, true);
    }

    /**
     * Set whether a data sync is requested.
     *
     * @param context       The Context
     * @param syncRequested Whether a data sync is requested
     */
    public static void requestDataSync(Context context, boolean syncRequested) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_DATA_SYNC_REQUESTED, syncRequested).apply();
    }

    /**
     * Is a data sync requested?
     *
     * @param context The Context
     * @return Whether a data sync was requested
     */
    public static boolean isDataSyncRequested(Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getBoolean(PREF_DATA_SYNC_REQUESTED, true);
    }

    /**
     * Request a photo sync.
     *
     * @param context The Context
     */
    public static void requestPhotoSync(Context context) {
        requestPhotoSync(context, true);
    }

    /**
     * Set whether a photo sync is requested.
     *
     * @param context       The Context
     * @param syncRequested Whether a photo sync is requested
     */
    public static void requestPhotoSync(Context context, boolean syncRequested) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_PHOTO_SYNC_REQUESTED, syncRequested).apply();
    }

    /**
     * Is a photo sync requested?
     *
     * @param context The Context
     * @return Whether a photo sync was requested
     */
    public static boolean isPhotoSyncRequested(Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getBoolean(PREF_PHOTO_SYNC_REQUESTED, true);
    }

    /**
     * Get a GoogleAccountCredential to authenticate with the backend.
     *
     * @param context The Context
     * @return A GoogleAccountCredential
     */
    public static GoogleAccountCredential getCredential(Context context) {
        return GoogleAccountCredential.usingAudience(context, "server:client_id:" + WEB_CLIENT_ID);
    }

    /**
     * Get the client identifier.
     *
     * @param context The Context
     * @return The client identifier
     */
    public static long getClientId(Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getLong(PREF_CLIENT_ID, 0);
    }

    /**
     * Set the client identifier.
     *
     * @param context  The Context
     * @param clientId The client identifier
     */
    public static void setClientId(Context context, long clientId) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putLong(PREF_CLIENT_ID, clientId).apply();
    }

    /**
     * Register the client with the backend.
     *
     * @param context     The Context
     * @param accountName The name of the account to use
     * @return Whether the registration was successful
     */
    public static boolean registerClient(Context context, String accountName) {
        if(accountName == null) {
            return false;
        }

        final GoogleAccountCredential credential = getCredential(context);
        credential.setSelectedAccountName(accountName);

        final InstanceID instanceID = InstanceID.getInstance(context);

        try {
            final String token =
                    instanceID.getToken(PROJECT_NUMBER, GoogleCloudMessaging.INSTANCE_ID_SCOPE);

            final Registration registration = BackendUtils.getRegistration(credential);
            final long clientId = registration.register(token).execute().getClientId();
            if(clientId > 0) {
                BackendUtils.setClientId(context, clientId);
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putString(FlavordexApp.PREF_ACCOUNT_NAME, accountName)
                        .putBoolean(FlavordexApp.PREF_SYNC_DATA, true).apply();

                return true;
            }
        } catch(IOException e) {
            Log.w(TAG, "Client registration failed", e);
        }

        return false;
    }

    /**
     * Unregister the client from the backend.
     *
     * @param context The Context
     * @return Whether the unregistration was successful
     */
    public static boolean unregisterClient(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        if(accountName == null) {
            return false;
        }

        final GoogleAccountCredential credential = getCredential(context);
        credential.setSelectedAccountName(accountName);

        final Registration registration = getRegistration(credential);
        try {
            InstanceID.getInstance(context)
                    .deleteToken(PROJECT_NUMBER, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            registration.unregister(getClientId(context)).execute();
            setClientId(context, 0);

            return true;
        } catch(IOException e) {
            Log.w(TAG, "Client unregistration failed", e);
        }

        return false;
    }

    /**
     * Get a Registration endpoint client.
     *
     * @param credential The credential to use for authentication
     * @return The Registration endpoint client
     */
    public static Registration getRegistration(GoogleAccountCredential credential) {
        final Registration.Builder builder = new Registration.Builder(new ApacheHttpTransport(),
                new AndroidJsonFactory(), credential);
        return (Registration)build(builder);
    }

    /**
     * Get a Sync endpoint client.
     *
     * @param credential The credential to use for authentication
     * @return The Sync endpoint client
     */
    public static Sync getSync(GoogleAccountCredential credential) {
        final Sync.Builder builder = new Sync.Builder(new ApacheHttpTransport(),
                new AndroidJsonFactory(), credential);
        return (Sync)build(builder);
    }

    /**
     * Build an endpoint client from a Builder.
     *
     * @param builder The Builder
     * @return The built endpoint client
     */
    private static AbstractGoogleJsonClient build(AbstractGoogleJsonClient.Builder builder) {
        builder.setApplicationName(PROJECT_ID);
        if(FlavordexApp.DEVELOPER_MODE) {
            builder.setRootUrl("http://10.0.2.2:8080/_ah/api");
        }
        return builder.build();
    }
}
