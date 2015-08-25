package com.ultramegasoft.flavordex2;

import android.app.Application;

/**
 * Stores global runtime information needed by the application.
 *
 * @author Steve Guidetti
 */
public class FlavordexApp extends Application {
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
}
