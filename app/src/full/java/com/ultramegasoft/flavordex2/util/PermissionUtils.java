package com.ultramegasoft.flavordex2.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.PermissionChecker;

import com.ultramegasoft.flavordex2.FlavordexApp;

/**
 * Full implementation of the permissions utilities. Adds helpers for location and accounts
 * permissions.
 *
 * @author Steve Guidetti
 */
public class PermissionUtils extends BasePermissionUtils {
    /**
     * Request codes
     */
    public static final int REQUEST_LOCATION = 20;
    public static final int REQUEST_ACCOUNTS = 30;

    /**
     * Keys for the backend shared preferences
     */
    private static final String PREF_ASKED_LOCATION = "pref_asked_location";
    private static final String PREF_ASKED_ACCOUNTS = "pref_asked_accounts";

    /**
     * Check whether we have permission to access the device's location.
     *
     * @param context The Context
     * @return Whether we have permission to access the device's location
     */
    public static boolean hasLocationPerm(Context context) {
        return PermissionChecker.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    /**
     * Make a request for location permissions from the user if they are not already granted.
     *
     * @param activity The Activity making the request
     * @return Whether we already have location permissions
     */
    public static boolean checkLocationPerm(FragmentActivity activity) {
        if(hasLocationPerm(activity)) {
            return true;
        }

        requestLocationPerm(activity);

        return false;
    }

    /**
     * Make a request for location permissions from the user if they are not already granted.
     *
     * @param fragment The Fragment making the request
     * @return Whether we already have location permissions
     */
    public static boolean checkLocationPerm(Fragment fragment) {
        if(hasLocationPerm(fragment.getContext())) {
            return true;
        }

        requestLocationPerm(fragment);

        return false;
    }

    /**
     * Make the actual request from the user for location permissions.
     *
     * @param activity The Activity making the request
     */
    public static void requestLocationPerm(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        getPreferences(activity).edit().putBoolean(PREF_ASKED_LOCATION, true).apply();
    }

    /**
     * Make the actual request from the user for location permissions.
     *
     * @param fragment The Fragment making the request
     */
    public static void requestLocationPerm(Fragment fragment) {
        fragment.requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION);
        getPreferences(fragment.getContext()).edit().putBoolean(PREF_ASKED_LOCATION, true).apply();
    }

    /**
     * Should we ask for location permissions? Returns true if the user has not checked 'Never ask
     * again.'
     *
     * @param activity The Activity making the request
     * @return Whether we should ask for location permissions
     */
    public static boolean shouldAskLocationPerm(Activity activity) {
        return !getPreferences(activity).getBoolean(PREF_ASKED_LOCATION, false)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Check whether we have permission to access the user's accounts.
     *
     * @param context The Context
     * @return Whether we have permission to access the user's accounts
     */
    public static boolean hasAccountsPerm(Context context) {
        return PermissionChecker.checkSelfPermission(context,
                Manifest.permission.GET_ACCOUNTS) == PermissionChecker.PERMISSION_GRANTED;
    }

    /**
     * Make a request for accounts permission from the user if they are not already granted.
     *
     * @param activity The Activity making the request
     * @return Whether we already have accounts permission
     */
    public static boolean checkAccountsPerm(FragmentActivity activity) {
        if(hasAccountsPerm(activity)) {
            return true;
        }

        requestAccountsPerm(activity);

        return false;
    }

    /**
     * Make a request for accounts permission from the user if they are not already granted.
     *
     * @param fragment The Fragment making the request
     * @return Whether we already have accounts permission
     */
    public static boolean checkAccountsPerm(Fragment fragment) {
        if(hasAccountsPerm(fragment.getContext())) {
            return true;
        }

        requestAccountsPerm(fragment);

        return false;
    }

    /**
     * Make the actual request from the user for accounts permission.
     *
     * @param activity The Activity making the request
     */
    public static void requestAccountsPerm(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] {Manifest.permission.GET_ACCOUNTS}, REQUEST_ACCOUNTS);
        getPreferences(activity).edit().putBoolean(PREF_ASKED_ACCOUNTS, true).apply();
    }

    /**
     * Make the actual request from the user for accounts permission.
     *
     * @param fragment The Fragment making the request
     */
    public static void requestAccountsPerm(Fragment fragment) {
        fragment.requestPermissions(new String[] {Manifest.permission.GET_ACCOUNTS},
                REQUEST_ACCOUNTS);
        getPreferences(fragment.getContext()).edit().putBoolean(PREF_ASKED_ACCOUNTS, true).apply();
    }

    /**
     * Should we ask for accounts permission? Returns true if the user has not checked 'Never ask
     * again.'
     *
     * @param activity The Activity making the request
     * @return Whether we should ask for accounts permission
     */
    public static boolean shouldAskAccountsPerm(Activity activity) {
        return !getPreferences(activity).getBoolean(PREF_ASKED_ACCOUNTS, false)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.GET_ACCOUNTS);
    }

    /**
     * @see BasePermissionUtils#onRequestPermissionsResult(Context, int, String[], int[])
     */
    public static void onRequestPermissionsResult(Context context, int requestCode,
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
                                .putBoolean(FlavordexApp.PREF_DETECT_LOCATION, true).commit();
                    }
                }
                return;
        }
        BasePermissionUtils.onRequestPermissionsResult(context, requestCode, permissions,
                grantResults);
    }
}
