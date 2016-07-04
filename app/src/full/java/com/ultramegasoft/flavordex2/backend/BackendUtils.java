package com.ultramegasoft.flavordex2.backend;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.model.RegistrationRecord;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.service.SyncService;

/**
 * Helpers for accessing the backend.
 *
 * @author Steve Guidetti
 */
public class BackendUtils {
    private static final String TAG = "BackendUtils";

    /**
     * Job tags for the job dispatcher
     */
    public static final String JOB_SYNC_DATA = "sync_data";
    public static final String JOB_SYNC_PHOTOS = "sync_photo";

    /**
     * Keys for the backend shared preferences
     */
    private static final String PREFS_KEY = "backend";
    private static final String PREF_CLIENT_ID = "pref_client_id";
    private static final String PREF_UID = "pref_uid";

    /**
     * The job dispatcher
     */
    private static FirebaseJobDispatcher sJobDispatcher;

    /**
     * Request a data sync.
     *
     * @param context The Context
     */
    public static void requestDataSync(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!prefs.getBoolean(FlavordexApp.PREF_ACCOUNT, false) ||
                !prefs.getBoolean(FlavordexApp.PREF_SYNC_DATA, false)) {
            return;
        }
        if(sJobDispatcher == null) {
            sJobDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        }
        final Job job = sJobDispatcher.newJobBuilder()
                .setService(SyncService.class)
                .setTag(JOB_SYNC_DATA)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.NOW)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
        sJobDispatcher.schedule(job);
    }

    /**
     * Cancel the data sync.
     */
    public static void cancelDataSync() {
        if(sJobDispatcher != null) {
            sJobDispatcher.cancel(JOB_SYNC_DATA);
        }
    }

    /**
     * Request a photo sync.
     *
     * @param context The Context
     */
    public static void requestPhotoSync(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!prefs.getBoolean(FlavordexApp.PREF_ACCOUNT, false) ||
                !prefs.getBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false)) {
            return;
        }
        if(sJobDispatcher == null) {
            sJobDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        }
        int constraints = Constraint.ON_ANY_NETWORK;
        if(PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(FlavordexApp.PREF_SYNC_PHOTOS_UNMETERED, true)) {
            constraints |= Constraint.ON_UNMETERED_NETWORK;
        }
        final Job job = sJobDispatcher.newJobBuilder()
                .setService(SyncService.class)
                .setTag(JOB_SYNC_PHOTOS)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.NOW)
                .setConstraints(constraints)
                .build();
        sJobDispatcher.schedule(job);
    }

    /**
     * Cancel the photo sync.
     */
    public static void cancelPhotoSync() {
        if(sJobDispatcher != null) {
            sJobDispatcher.cancel(JOB_SYNC_PHOTOS);
        }
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
     * Set the user ID, resetting the synchronization state if changed.
     *
     * @param context The Context
     * @param uid     The user ID
     */
    public static void setUid(Context context, String uid) {
        final SharedPreferences prefs =
                context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        if(!uid.equals(prefs.getString(PREF_UID, null))) {
            final ContentResolver cr = context.getContentResolver();

            final ContentValues values = new ContentValues();
            values.put(Tables.Cats.SYNCED, false);
            cr.update(Tables.Cats.CONTENT_URI, values, null, null);

            values.clear();
            values.put(Tables.Entries.SYNCED, false);
            cr.update(Tables.Entries.CONTENT_URI, values, null, null);

            prefs.edit().putString(PREF_UID, uid).apply();
        }
    }

    /**
     * Register the client with the backend.
     *
     * @param context The Context
     * @return Whether the registration was successful
     */
    public static boolean registerClient(Context context) {
        final Registration registration = new Registration(context);
        final RegistrationRecord record;
        try {
            record = registration.register();
            if(record != null) {
                BackendUtils.setClientId(context, record.clientId);
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putBoolean(FlavordexApp.PREF_SYNC_DATA, true).apply();

                return true;
            }
        } catch(ApiException e) {
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
        try {
            new Registration(context).unregister();
            setClientId(context, 0);

            return true;
        } catch(ApiException e) {
            Log.w(TAG, "Client unregistration failed", e);
        }

        return false;
    }
}
