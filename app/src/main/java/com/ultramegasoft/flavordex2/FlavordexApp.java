package com.ultramegasoft.flavordex2;

import android.app.Application;

/**
 * Stores global runtime information needed by the application
 *
 * @author Steve Guidetti
 */
public class FlavordexApp extends Application {
    public static final String PREF_DETECT_LOCATION = "pref_detect_location";
    public static final String PREF_RETAIN_PHOTOS = "pref_retain_photos";
    public static final String PREF_LIST_SORT = "pref_list_sort";
}
