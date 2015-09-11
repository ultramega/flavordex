package com.ultramegasoft.flavordex2.util;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;

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
    public static final int REQUEST_STORAGE = 100;

    /**
     * Check whether we have permission to read and write external storage.
     *
     * @param context The Context
     * @return Whether we have permission to read and write external storage
     */
    public static boolean hasExternalStoragePerm(Context context) {
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
    public static boolean checkExternalStoragePerm(final FragmentActivity activity, int rationale) {
        return checkExternalStoragePerm(activity, activity.getText(rationale));
    }

    /**
     * Make a request for external storage permissions from the user if they are not already
     * granted.
     *
     * @param activity  The Activity making the request
     * @param rationale Rationale for requesting external storage permissions
     * @return Whether we already have external storage permissions
     */
    public static boolean checkExternalStoragePerm(final FragmentActivity activity, CharSequence rationale) {
        if(hasExternalStoragePerm(activity)) {
            return true;
        }

        if(shouldAskExternalStoragePerm(activity)) {
            PermissionDialog.showDialog(activity, rationale);
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
    public static void requestExternalStoragePerm(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
    }

    /**
     * Should we ask for external storage permissions? Returns true if the user has previously
     * denied permission but has not checked 'Never ask again.'
     *
     * @param activity The Activity making the request
     * @return Whether we should ask for external storage permissions
     */
    public static boolean shouldAskExternalStoragePerm(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * Callback for permission requests. If external storage permissions are granted, this will
     * restart the application. If the user checks 'Never ask again' this will restart the
     * Activity.
     *
     * @param activity     The Activity making the request
     * @param requestCode  The request code
     * @param permissions  Array of permissions requested
     * @param grantResults Array of results of the permission requests
     */
    public static void onRequestPermissionsResult(Activity activity, int requestCode,
                                                  @NonNull String[] permissions,
                                                  @NonNull int[] grantResults) {
        if(requestCode == REQUEST_STORAGE) {
            for(int i = 0; i < grantResults.length; i++) {
                if(!Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])) {
                    continue;
                }
                if(grantResults[i] == PermissionChecker.PERMISSION_GRANTED) {
                    Runtime.getRuntime().exit(0);
                } else if(!shouldAskExternalStoragePerm(activity)) {
                    activity.recreate();
                }
            }
        }
    }

    /**
     * Dialog for showing a permission request rationale to the user.
     */
    public static class PermissionDialog extends DialogFragment {
        /**
         * Tag to identify the Fragment
         */
        private static final String TAG = "PermissionDialog";

        /**
         * Arguments for the Fragment
         */
        private static final String ARG_MESSAGE = "message";

        /**
         * Show the dialog.
         *
         * @param activity The Activity to attach to
         * @param message  The rationale message
         */
        public static void showDialog(FragmentActivity activity, CharSequence message) {
            final DialogFragment fragment = new PermissionDialog();

            final Bundle args = new Bundle();
            args.putCharSequence(ARG_MESSAGE, message);
            fragment.setArguments(args);

            fragment.show(activity.getSupportFragmentManager(), TAG);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setIcon(R.drawable.ic_info)
                    .setTitle(R.string.title_permission)
                    .setMessage(getArguments().getCharSequence(ARG_MESSAGE))
                    .setPositiveButton(R.string.button_ok, null)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            requestExternalStoragePerm(getActivity());
        }
    }
}
