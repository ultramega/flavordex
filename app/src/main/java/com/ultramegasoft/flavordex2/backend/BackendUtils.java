/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.backend;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private static final String PREF_EMAIL = "pref_email";

    /**
     * The job dispatcher
     */
    @Nullable
    private static FirebaseJobDispatcher sJobDispatcher;

    /**
     * Request a data sync.
     *
     * @param context The Context
     */
    public static void requestDataSync(@NonNull Context context) {
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
    public static void requestPhotoSync(@NonNull Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!prefs.getBoolean(FlavordexApp.PREF_ACCOUNT, false) ||
                !prefs.getBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false) ||
                !prefs.getBoolean(FlavordexApp.PREF_SYNC_DATA, false)) {
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
    static long getClientId(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getLong(PREF_CLIENT_ID, 0);
    }

    /**
     * Set the client identifier.
     *
     * @param context  The Context
     * @param clientId The client identifier
     */
    public static void setClientId(@NonNull Context context, long clientId) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putLong(PREF_CLIENT_ID, clientId).apply();
    }

    /**
     * Set the user ID, resetting the synchronization state if changed.
     *
     * @param context The Context
     * @param uid     The user ID
     */
    public static void setUid(@NonNull Context context, @NonNull String uid) {
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
    public static boolean registerClient(@NonNull Context context) {
        final Registration registration = new Registration(context);
        final RegistrationRecord record;
        try {
            record = registration.register();
            if(record != null) {
                setClientId(context, record.clientId);
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putBoolean(FlavordexApp.PREF_SYNC_DATA, true).apply();

                return true;
            }
        } catch(FlavordexApiException e) {
            Log.w(TAG, "Client registration failed", e);
        }

        return false;
    }

    /**
     * Unregister the client from the backend.
     *
     * @param context The Context
     */
    public static void unregisterClient(@NonNull Context context) {
        try {
            new Registration(context).unregister();
            setClientId(context, 0);
        } catch(FlavordexApiException e) {
            Log.w(TAG, "Client unregistration failed", e);
        }
    }

    /**
     * Get the saved email address.
     *
     * @param context The Context
     * @return The saved email address, or null if it does not exist
     */
    @Nullable
    public static String getEmail(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .getString(PREF_EMAIL, null);
    }

    /**
     * Set the saved email address.
     *
     * @param context The Context
     * @param email   The email address
     */
    public static void setEmail(@NonNull Context context, @Nullable String email) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
                .putString(PREF_EMAIL, email).apply();
    }
}
