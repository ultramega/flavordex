package com.ultramegasoft.flavordex2;

import android.location.Location;

/**
 * Lite implementation of the Application.
 *
 * @author Steve Guidetti
 * @see AbsFlavordexApp
 */
public class FlavordexApp extends AbsFlavordexApp {
    /**
     * The Authority string for the application
     */
    public static final String AUTHORITY = "com.ultramegasoft.flavordex2.lite.provider";

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public String getLocationName() {
        return null;
    }
}
