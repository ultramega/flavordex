package com.ultramegasoft.flavordex2.provider.tables;

import android.provider.BaseColumns;

/**
 * Data contract for the 'entries_flavors' table.
 *
 * @author Steve Guidetti
 */
public class EntriesFlavors implements BaseColumns {
    public static final String TABLE_NAME = "entries_flavors";

    public static final String ENTRY = "entry";
    public static final String FLAVOR = "flavor";
    public static final String VALUE = "value";

    private EntriesFlavors() {
    }
}
