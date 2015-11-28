package com.ultramegasoft.flavordex2;

import android.location.Location;

/**
 * Lite implementation of the Application.
 *
 * @author Steve Guidetti
 * @see AbsFlavordexApp
 */
public class FlavordexApp extends AbsFlavordexApp {
    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public String getLocationName() {
        return null;
    }
}
