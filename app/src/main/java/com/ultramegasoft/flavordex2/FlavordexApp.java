package com.ultramegasoft.flavordex2;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.ultramegasoft.flavordex2.provider.Tables;

import java.util.HashMap;

/**
 * Stores global runtime information needed by the application.
 *
 * @author Steve Guidetti
 */
public class FlavordexApp extends Application implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * Preference names
     */
    public static final String PREF_DETECT_LOCATION = "pref_detect_location";
    public static final String PREF_LIST_SORT_FIELD = "pref_list_sort_field";
    public static final String PREF_LIST_SORT_REVERSED = "pref_list_sort_reversed";

    /**
     * Entry type preset names
     */
    public static final String TYPE_BEER = "_beer";
    public static final String TYPE_WINE = "_wine";
    public static final String TYPE_WHISKEY = "_whiskey";
    public static final String TYPE_COFFEE = "_coffee";

    /**
     * Map of preset type names to string resource ids
     */
    private static final HashMap<String, Integer> sTypeNameMap = new HashMap<String, Integer>() {
        {
            put(TYPE_BEER, R.string.type_beer);
            put(TYPE_WINE, R.string.type_wine);
            put(TYPE_WHISKEY, R.string.type_whiskey);
            put(TYPE_COFFEE, R.string.type_coffee);
        }
    };

    /**
     * Listener for location updates.
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
     * Task for finding the nearest location from the database
     */
    private final LocatorTask mLocatorTask = new LocatorTask();

    /**
     * The current location
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
        if(PREF_DETECT_LOCATION.equals(key)) {
            setLocationEnabled(sharedPreferences.getBoolean(key, false));
        }
    }

    /**
     * Get the real display name of an entry type from a raw database name value, translating
     * internal names as needed.
     *
     * @param context The context
     * @param name    The name from the database
     * @return The real display name
     */
    public static String getRealTypeName(Context context, String name) {
        if(sTypeNameMap.containsKey(name)) {
            return context.getString(sTypeNameMap.get(name));
        }
        return name;
    }

    /**
     * Get the current location.
     *
     * @return The current location
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
     * Set the current location.
     *
     * @param location The location
     */
    private void setLocation(Location location) {
        mLocation = location;
        if(location != null) {
            mLocatorTask.cancel(true);
            mLocatorTask.execute();
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
        } catch(SecurityException ignored) {
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
