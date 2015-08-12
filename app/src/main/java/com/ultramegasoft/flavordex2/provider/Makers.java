package com.ultramegasoft.flavordex2.provider;

import android.provider.BaseColumns;

/**
 * Data contract for the 'makers' table.
 *
 * @author Steve Guidetti
 */
public class Makers implements BaseColumns {
    public static final String TABLE_NAME = "makers";

    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String LOCATION = "location";

    private Makers() {
    }
}
