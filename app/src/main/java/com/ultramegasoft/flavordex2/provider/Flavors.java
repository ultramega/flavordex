package com.ultramegasoft.flavordex2.provider;

import android.provider.BaseColumns;

/**
 * Data contract for the 'flavors' table.
 *
 * @author Steve Guidetti
 */
public class Flavors implements BaseColumns {
    public static final String TABLE_NAME = "flavors";

    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String DELETED = "deleted";

    private Flavors() {
    }
}
