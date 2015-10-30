package com.ultramegasoft.flavordex2;

import android.app.Application;
import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;

import java.util.HashMap;

/**
 * Stores global runtime information needed by the application.
 *
 * @author Steve Guidetti
 */
public class FlavordexApp extends Application implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * Enable developer features
     */
    public static final boolean DEVELOPER_MODE = true;

    /**
     * Preference names
     */
    public static final String PREF_FIRST_RUN = "pref_first_run";
    public static final String PREF_ACCOUNT_NAME = "pref_account_name";
    public static final String PREF_SYNC_DATA = "pref_sync_data";
    public static final String PREF_SYNC_PHOTOS = "pref_sync_photos";
    public static final String PREF_SYNC_PHOTOS_UNMETERED = "pref_sync_photos_unmetered";
    public static final String PREF_DETECT_LOCATION = "pref_detect_location";
    public static final String PREF_LIST_SORT_FIELD = "pref_list_sort_field";
    public static final String PREF_LIST_SORT_REVERSED = "pref_list_sort_reversed";

    /**
     * Entry category preset names
     */
    public static final String CAT_BEER = "_beer";
    public static final String CAT_WINE = "_wine";
    public static final String CAT_WHISKEY = "_whiskey";
    public static final String CAT_COFFEE = "_coffee";

    /**
     * Map of preset category names to string resource IDs
     */
    private static final HashMap<String, Integer> sCatNameMap = new HashMap<String, Integer>() {
        {
            put(CAT_BEER, R.string.cat_beer);
            put(CAT_WINE, R.string.cat_wine);
            put(CAT_WHISKEY, R.string.cat_whiskey);
            put(CAT_COFFEE, R.string.cat_coffee);
        }
    };

    /**
     * Listener for location updates
     */
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            setLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * The current Location
     */
    private Location mLocation;

    /**
     * The name of the nearest previous location
     */
    private String mLocationName;

    /**
     * The BackupManager to notify of preference changes
     */
    private BackupManager mBackupManager;

    @Override
    public void onCreate() {
        super.onCreate();
        if(DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        mBackupManager = new BackupManager(this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        if(prefs.getBoolean(PREF_DETECT_LOCATION, false)) {
            setLocationEnabled(true);
        }

        BackendUtils.requestDataSync(this);
        BackendUtils.requestPhotoSync(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mBackupManager.dataChanged();
        if(PREF_DETECT_LOCATION.equals(key)) {
            setLocationEnabled(sharedPreferences.getBoolean(key, false));
        }
    }

    /**
     * Get the real display name of an entry category from a raw database name value, translating
     * internal names as needed.
     *
     * @param context The Context
     * @param name    The name from the database
     * @return The real display name
     */
    public static String getRealCatName(Context context, String name) {
        if(sCatNameMap.containsKey(name)) {
            return context.getString(sCatNameMap.get(name));
        }
        return name;
    }

    /**
     * Get the current Location.
     *
     * @return The current Location
     */
    public Location getLocation() {
        return mLocation;
    }

    /**
     * Get the name of the nearest previous location.
     *
     * @return The nearest location name
     */
    public String getLocationName() {
        return mLocationName;
    }

    /**
     * Set the current Location.
     *
     * @param location The Location
     */
    private void setLocation(Location location) {
        mLocation = location;
        if(location != null) {
            new LocatorTask().execute();
        } else {
            mLocationName = null;
        }
    }

    /**
     * Enable or disable location detection.
     *
     * @param enabled Whether to enable location detection
     */
    private void setLocationEnabled(boolean enabled) {
        final LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
        if(!lm.getProviders(true).contains(LocationManager.NETWORK_PROVIDER)) {
            return;
        }

        try {
            if(enabled) {
                setLocation(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * 60 * 1000, 0,
                        mLocationListener);
            } else {
                setLocation(null);
                lm.removeUpdates(mLocationListener);
            }
        } catch(SecurityException e) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean(PREF_DETECT_LOCATION, false).commit();
        }
    }

    /**
     * Task for finding the nearest location from the database in the background.
     */
    private class LocatorTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            final ContentResolver cr = getContentResolver();
            final Cursor cursor = cr.query(Tables.Locations.CONTENT_URI, null, null, null, null);
            if(cursor == null) {
                return null;
            }

            final double lat = mLocation.getLatitude();
            final double lon = mLocation.getLongitude();

            try {
                String closestName = null;
                float closestDistance = Float.MAX_VALUE;
                final float[] distance = new float[1];
                while(cursor.moveToNext()) {
                    if(isCancelled()) {
                        return null;
                    }

                    Location.distanceBetween(
                            lat, lon,
                            cursor.getDouble(cursor.getColumnIndex(Tables.Locations.LATITUDE)),
                            cursor.getDouble(cursor.getColumnIndex(Tables.Locations.LONGITUDE)),
                            distance);

                    if(distance[0] < closestDistance) {
                        closestDistance = distance[0];
                        closestName =
                                cursor.getString(cursor.getColumnIndex(Tables.Locations.NAME));
                    }
                }

                mLocationName = closestName;
            } finally {
                cursor.close();
            }

            return null;
        }
    }
}
