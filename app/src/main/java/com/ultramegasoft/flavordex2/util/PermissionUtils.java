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
package com.ultramegasoft.flavordex2.util;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;

/**
 * Helpers for dealing with permissions.
 *
 * @author Steve Guidetti
 */
public class PermissionUtils {
    /**
     * Request codes
     */
    private static final int REQUEST_STORAGE = 10;
    private static final int REQUEST_LOCATION = 20;

    /**
     * Keys for the backend shared preferences
     */
    private static final String PREFS_KEY = "perms";
    private static final String PREF_ASKED_STORAGE = "pref_asked_storage";
    private static final String PREF_ASKED_LOCATION = "pref_asked_location";

    /**
     * Check whether we have permission to read and write external storage.
     *
     * @param context The Context
     * @return Whether we have permission to read and write external storage
     */
    public static boolean hasExternalStoragePerm(@NonNull Context context) {
        return PermissionChecker.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED;
    }

    /**
     * Make a request for external storage permissions from the user if they are not already
     * granted.
     *
     * @param activity  The Activity making the request
     * @param rationale Rationale for requesting external storage permissions
     * @return Whether we already have external storage permissions
     */
    public static boolean checkExternalStoragePerm(@NonNull FragmentActivity activity,
                                                   int rationale) {
        if(hasExternalStoragePerm(activity)) {
            return true;
        }

        if(ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionDialog.showDialog(activity.getSupportFragmentManager(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_STORAGE,
                    activity.getString(rationale), null);
            getPreferences(activity).edit().putBoolean(PREF_ASKED_STORAGE, true).apply();
        } else {
            requestExternalStoragePerm(activity);
        }

        return false;
    }

    /**
     * Make the actual request from the user for external storage permissions.
     *
     * @param activity The Activity making the request
     */
    public static void requestExternalStoragePerm(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        getPreferences(activity).edit().putBoolean(PREF_ASKED_STORAGE, true).apply();
    }

    /**
     * Should we ask for external storage permissions? Returns true if the user has not checked
     * 'Never ask again.'
     *
     * @param activity The Activity making the request
     * @return Whether we should ask for external storage permissions
     */
    public static boolean shouldAskExternalStoragePerm(@NonNull Activity activity) {
        return !getPreferences(activity).getBoolean(PREF_ASKED_STORAGE, false)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * Check whether we have permission to access the device's location.
     *
     * @param context The Context
     * @return Whether we have permission to access the device's location
     */
    public static boolean hasLocationPerm(@NonNull Context context) {
        return PermissionChecker.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    /**
     * Make a request for location permissions from the user if they are not already granted.
     *
     * @param fragment The Fragment making the request
     * @return Whether we already have location permissions
     */
    public static boolean checkLocationPerm(@NonNull Fragment fragment) {
        final Context context = fragment.getContext();
        if(context != null) {
            if(hasLocationPerm(context)) {
                return true;
            }

            requestLocationPerm(fragment);
        }

        return false;
    }

    /**
     * Make the actual request from the user for location permissions.
     *
     * @param fragment The Fragment making the request
     */
    private static void requestLocationPerm(@NonNull Fragment fragment) {
        fragment.requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION);

        final Context context = fragment.getContext();
        if(context != null) {
            getPreferences(context).edit().putBoolean(PREF_ASKED_LOCATION, true).apply();
        }
    }

    /**
     * Should we ask for location permissions? Returns true if the user has not checked 'Never ask
     * again.'
     *
     * @param activity The Activity making the request
     * @return Whether we should ask for location permissions
     */
    public static boolean shouldAskLocationPerm(@NonNull Activity activity) {
        return !getPreferences(activity).getBoolean(PREF_ASKED_LOCATION, false)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Callback for permission requests. If external storage permissions are granted, this will
     * restart the application.
     *
     * @param context      The Context
     * @param requestCode  The request code
     * @param permissions  Array of permissions requested
     * @param grantResults Array of results of the permission requests
     */
    public static void onRequestPermissionsResult(@NonNull Context context, int requestCode,
                                                  @NonNull String[] permissions,
                                                  @NonNull int[] grantResults) {
        switch(requestCode) {
            case REQUEST_LOCATION:
                for(int i = 0; i < grantResults.length; i++) {
                    if(!Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                        continue;
                    }
                    if(grantResults[i] == PermissionChecker.PERMISSION_GRANTED) {
                        PreferenceManager.getDefaultSharedPreferences(context).edit()
                                .putBoolean(FlavordexApp.PREF_DETECT_LOCATION, true).apply();
                    }
                }
                break;
        }
    }

    /**
     * Get the shared preferences for the permissions.
     *
     * @param context The Context
     * @return The SharedPreferences
     */
    @NonNull
    private static SharedPreferences getPreferences(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Dialog for showing a permission request rationale to the user.
     */
    public static class PermissionDialog extends DialogFragment {
        private static final String TAG = "PermissionDialog";

        /**
         * Arguments for the Fragment
         */
        private static final String ARG_PERMISSION = "permission";
        private static final String ARG_MESSAGE = "message";

        /**
         * Show the dialog.
         *
         * @param fm          The FragmentManager to use
         * @param permission  The permission being requested
         * @param requestCode The permission request code
         * @param target      The target Fragment
         * @param message     The rationale message
         */
        @SuppressWarnings("SameParameterValue")
        static void showDialog(@NonNull FragmentManager fm, @NonNull String permission,
                               int requestCode, @NonNull CharSequence message,
                               @Nullable Fragment target) {
            final DialogFragment fragment = new PermissionDialog();
            fragment.setTargetFragment(target, requestCode);

            final Bundle args = new Bundle();
            args.putString(ARG_PERMISSION, permission);
            args.putCharSequence(ARG_MESSAGE, message);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getContext();
            if(context == null) {
                return super.onCreateDialog(savedInstanceState);
            }

            final Bundle args = getArguments();
            return new AlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_info)
                    .setTitle(R.string.title_permission)
                    .setMessage(args != null ? args.getCharSequence(ARG_MESSAGE) : null)
                    .setPositiveButton(R.string.button_ok, null)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            final Bundle args = getArguments();
            if(args == null) {
                return;
            }

            final String permission = args.getString(ARG_PERMISSION);
            final Fragment target = getTargetFragment();
            if(target != null) {
                target.requestPermissions(new String[] {permission}, getTargetRequestCode());
            } else {
                final Activity activity = getActivity();
                if(activity != null) {
                    ActivityCompat.requestPermissions(activity, new String[] {permission},
                            getTargetRequestCode());
                }
            }
        }
    }
}
