package com.ultramegasoft.flavordex2.provider.tables;

import android.provider.BaseColumns;

/**
 * Data contract for the 'entries_extras' table.
 *
 * @author Steve Guidetti
 */
public class EntriesExtras implements BaseColumns {
    public static final String TABLE_NAME = "entries_extras";

    public static final String ENTRY = "entry";
    public static final String EXTRA = "extra";
    public static final String VALUE = "value";

    private EntriesExtras() {
    }
}
