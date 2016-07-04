package com.ultramegasoft.flavordex2;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.provider.Tables;

/**
 * Full implementation of the Application. Adds support for location detection and data
 * synchronization.
 *
 * @author Steve Guidetti
 * @see AbsFlavordexApp
 */
public class FlavordexApp extends AbsFlavordexApp implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * The Authority string for the application
     */
    public static final String AUTHORITY = "com.ultramegasoft.flavordex2.provider";

    /**
     * Preference names
     */
    public static final String PREF_ACCOUNT = "pref_account";
    public static final String PREF_SYNC_DATA = "pref_sync_data";
    public static final String PREF_SYNC_PHOTOS = "pref_sync_photos";
    public static final String PREF_SYNC_PHOTOS_UNMETERED = "pref_sync_photos_unmetered";
    public static final String PREF_DETECT_LOCATION = "pref_detect_location";

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

    @Override
    public void onCreate() {
        super.onCreate();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        if(prefs.getBoolean(PREF_DETECT_LOCATION, false)) {
            setLocationEnabled(true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case PREF_DETECT_LOCATION:
                setLocationEnabled(sharedPreferences.getBoolean(key, false));
                break;
            case PREF_SYNC_DATA:
                if(sharedPreferences.getBoolean(key, false)) {
                    BackendUtils.requestDataSync(this);
                } else {
                    BackendUtils.cancelDataSync();
                }
                break;
            case PREF_SYNC_PHOTOS:
                if(sharedPreferences.getBoolean(key, false)) {
                    BackendUtils.requestPhotoSync(this);
                } else {
                    BackendUtils.cancelPhotoSync();
                }
                break;
            case PREF_SYNC_PHOTOS_UNMETERED:
                BackendUtils.requestPhotoSync(this);
                break;
        }
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
