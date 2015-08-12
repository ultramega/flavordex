package com.ultramegasoft.flavordex2.provider;

import android.provider.BaseColumns;

/**
 * Data contract for the 'extras' table.
 *
 * @author Steve Guidetti
 */
public class Extras implements BaseColumns {
    public static final String TABLE_NAME = "extras";

    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String PRESET = "preset";
    public static final String DELETED = "deleted";

    private Extras() {
    }
}
