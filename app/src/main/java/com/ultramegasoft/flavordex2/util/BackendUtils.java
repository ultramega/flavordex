package com.ultramegasoft.flavordex2.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.registration.Registration;
import com.ultramegasoft.flavordex2.backend.sync.Sync;
import com.ultramegasoft.flavordex2.service.TaskService;

/**
 * Helpers for accessing the backend.
 *
 * @author Steve Guidetti
 */
public class BackendUtils {
    /**
     * Task tags for the GCM Network Manager
     */
    public static final String TASK_SYNC_DATA = "sync_data";
    public static final String TASK_SYNC_PHOTOS = "sync_photos";

    /**
     * Keys for the backend shared preferences
     */
    private static final String PREFS_KEY = "backend";
    private static final String PREF_CLIENT_ID = "pref_client_id";
    private static final String PREF_LAST_SYNC = "pref_last_sync";

    /**
     * The API project ID
     */
    private static final String PROJECT_ID = "flavordex";

    /**
     * The project Web client ID
     */
    private static final String WEB_CLIENT_ID = "1001621163874-su48pt09eaj7rd4g0mni19ag4vv2g7p7.apps.googleusercontent.com";

    /**
     * Schedule the sync service to run if it is enabled.
     *
     * @param context The Context
     */
    public static void requestDataSync(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.getBoolean(FlavordexApp.PREF_SYNC_DATA, false)) {
            final OneoffTask task = new OneoffTask.Builder()
                    .setTag(TASK_SYNC_DATA)
                    .setUpdateCurrent(true)
                    .setExecutionWindow(5, 30)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setService(TaskService.class)
                    .build();
            GcmNetworkManager.getInstance(context).schedule(task);
        }
    }

    /**
     * Schedule the photo sync service to run if it is enabled.
     *
     * @param context The Context
     */
    public static void requestPhotoSync(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.getBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false)) {
            final OneoffTask.Builder builder = new OneoffTask.Builder()
                    .setTag(TASK_SYNC_PHOTOS)
                    .setUpdateCurrent(true)
                    .setExecutionWindow(5, 30)
                    .setService(TaskService.class);

            if(prefs.getBoolean(FlavordexApp.PREF_SYNC_PHOTOS_UNMETERED, true)) {
                builder.setRequiredNetwork(Task.NETWORK_STATE_UNMETERED);
            } else {
                builder.setRequiredNetwork(Task.NETWORK_STATE_CONNECTED);
            }

            GcmNetworkManager.getInstance(context).schedule(builder.build());
        }
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
     * Get the Unix timestamp of the last sync with the backend.
     *
     * @param context The Context
     * @return The Unix timestamp with milliseconds
     */
    public static long getLastSync(Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getLong(PREF_LAST_SYNC, -1);
    }

    /**
     * Set the time of the last sync with the backend to now.
     *
     * @param context The Context
     */
    public static void setLastSync(Context context) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply();
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
