package com.ultramegasoft.flavordex2.backend;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.model.RegistrationRecord;
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

    /**
     * Keys for the backend shared preferences
     */
    private static final String PREFS_KEY = "backend";
    private static final String PREF_CLIENT_ID = "pref_client_id";
    private static final String PREF_DATA_SYNC_REQUESTED = "pref_data_sync_requested";
    private static final String PREF_PHOTO_SYNC_REQUESTED = "pref_photo_sync_requested";
    private static final String PREF_REMOTE_IDS_REQUESTED = "pref_remote_ids_requested";

    /**
     * The job dispatcher
     */
    private static FirebaseJobDispatcher sJobDispatcher;

    /**
     * Schedule the sync service to run.
     *
     * @param context The Context
     */
    public static void setupSync(Context context) {
        if(sJobDispatcher == null) {
            sJobDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        }
        final Job job = sJobDispatcher.newJobBuilder()
                .setService(SyncService.class)
                .setTag(JOB_SYNC_DATA)
                .setReplaceCurrent(true)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(30, 60))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
        sJobDispatcher.schedule(job);

        requestDataSync(context);
        requestPhotoSync(context);
    }

    /**
     * Cancel all sync jobs.
     */
    public static void stopSync() {
        if(sJobDispatcher != null) {
            sJobDispatcher.cancel(JOB_SYNC_DATA);
        }
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

    /**
     * Set whether to request remote IDs from the backend.
     *
     * @param context   The Context
     * @param requested Whether to request remote IDs
     */
    public static void setRequestRemoteIds(Context context, boolean requested) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_REMOTE_IDS_REQUESTED, requested).apply();
    }

    /**
     * Are remote IDs requested?
     *
     * @param context The Context
     * @return Whether remote IDs have been requested
     */
    public static boolean areRemoteIdsRequested(Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getBoolean(PREF_REMOTE_IDS_REQUESTED, false);
    }

    /**
     * Helper class to implement exponential backoff.
     */
    public static class ExponentialBackoffHelper {
        /**
         * The base interval of delay times in milliseconds
         */
        private final long mRetryInterval;

        /**
         * The amount of tolerance in the random offset in delay times in milliseconds
         */
        private final long mRetryTolerance;

        /**
         * The maximum delay time in milliseconds
         */
        private final long mMaxRetryDelay;

        /**
         * The current failure count
         */
        private long mFailureCount = 0;

        /**
         * The minimum timestamp of the next allowed execution
         */
        private long mRetryTime = 0;

        /**
         * @param interval  The base interval of delay times in seconds
         * @param tolerance The amount of tolerance in the random offset in delay times in seconds
         * @param maxDelay  The maximum delay time in seconds
         */
        public ExponentialBackoffHelper(long interval, long tolerance, long maxDelay) {
            mRetryInterval = interval * 1000;
            mRetryTolerance = tolerance * 1000;
            mMaxRetryDelay = maxDelay * 1000;
        }

        /**
         * Should the task be executed?
         *
         * @return Whether the retry delay has been met
         */
        public boolean shouldExecute() {
            return System.currentTimeMillis() > mRetryTime;
        }

        /**
         * Call when the task has executed successfully to reset the failure count.
         */
        public void onSuccess() {
            mFailureCount = mRetryTime = 0;
        }

        /**
         * Call when the task has failed to execute.
         */
        public void onFail() {
            mFailureCount++;
            long delay = (long)(Math.pow(2, mFailureCount) * mRetryInterval);
            delay = delay > mMaxRetryDelay ? mMaxRetryDelay : delay;
            delay += (Math.random() - 0.5) * mRetryTolerance;
            mRetryTime = System.currentTimeMillis() + delay;
            Log.d(TAG, "Task failed! Retrying in " + delay + "ms");
        }
    }
}
