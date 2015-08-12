package com.ultramegasoft.flavordex2.provider;

import android.provider.BaseColumns;

/**
 * Data contract for the 'entries' table and view.
 *
 * @author Steve Guidetti
 */
public class Entries implements BaseColumns {
    public static final String TABLE_NAME = "entries";
    public static final String VIEW_NAME = "viewentry";

    public static final String TITLE = "title";
    public static final String TYPE_ID = "type_id";
    public static final String TYPE = "type";
    public static final String MAKER_ID = "maker_id";
    public static final String MAKER = "maker";
    public static final String ORIGIN = "origin";
    public static final String LOCATION = "location";
    public static final String DATE = "date";
    public static final String PRICE = "price";
    public static final String RATING = "rating";
    public static final String NOTES = "notes";

    private Entries() {
    }
}
