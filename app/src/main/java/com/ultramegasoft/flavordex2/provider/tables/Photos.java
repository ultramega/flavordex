package com.ultramegasoft.flavordex2.provider.tables;

import android.provider.BaseColumns;

/**
 * Data contract for the 'photos' table.
 *
 * @author Steve Guidetti
 */
public class Photos implements BaseColumns {
    public static final String TABLE_NAME = "photos";

    public static final String ENTRY = "entry";
    public static final String PATH = "path";
    public static final String FROM_GALLERY = "from_gallery";

    private Photos() {
    }
}
