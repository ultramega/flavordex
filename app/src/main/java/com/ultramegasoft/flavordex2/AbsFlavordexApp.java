package com.ultramegasoft.flavordex2;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.StrictMode;

import java.util.HashMap;

/**
 * Base class for the Application. Stores global runtime information needed by the application.
 *
 * @author Steve Guidetti
 */
public abstract class AbsFlavordexApp extends Application {
    /**
     * Enable developer features
     */
    public static final boolean DEVELOPER_MODE = true;

    /**
     * Preference names
     */
    public static final String PREF_FIRST_RUN = "pref_first_run";
    public static final String PREF_LIST_SORT_FIELD = "pref_list_sort_field";
    public static final String PREF_LIST_SORT_REVERSED = "pref_list_sort_reversed";
    public static final String PREF_LIST_CAT_ID = "pref_list_cat_id";

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
    public abstract Location getLocation();

    /**
     * Get the name of the nearest previous location.
     *
     * @return The nearest location name
     */
    public abstract String getLocationName();
}
