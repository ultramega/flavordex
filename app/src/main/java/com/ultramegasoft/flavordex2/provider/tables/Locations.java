package com.ultramegasoft.flavordex2.provider.tables;

import android.provider.BaseColumns;

/**
 * Data contract for the 'locations' table.
 *
 * @author Steve Guidetti
 */
public class Locations implements BaseColumns {
    public static final String TABLE_NAME = "locations";

    public static final String LATITUDE = "lat";
    public static final String LONGITUDE = "lon";
    public static final String NAME = "name";

    private Locations() {
    }
}
