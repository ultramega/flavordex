package com.ultramegasoft.flavordex2.provider.tables;

import android.provider.BaseColumns;

/**
 * Data contract for the 'types' table.
 *
 * @author Steve Guidetti
 */
public class Types implements BaseColumns {
    public static final String TABLE_NAME = "types";

    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String PRESET = "preset";

    private Types() {
    }
}
