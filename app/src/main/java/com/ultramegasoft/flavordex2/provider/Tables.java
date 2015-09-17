package com.ultramegasoft.flavordex2.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Constants for accessing database records.
 *
 * @author Steve Guidetti
 */
@SuppressWarnings("unused")
public class Tables {
    /**
     * The Authority string for the application
     */
    public static final String AUTHORITY = "com.ultramegasoft.flavordex2";

    /**
     * The base for all content Uris
     */
    private static final String URI_BASE = "content://" + AUTHORITY + "/";

    /**
     * Data contract for the 'entries' table and view.
     *
     * @author Steve Guidetti
     */
    public static class Entries implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "entries";
        public static final String VIEW_NAME = "view_entry";

        /**
         * Column names
         */
        public static final String TITLE = "title";
        public static final String CAT_ID = "cat_id";
        public static final String CAT = "cat";
        public static final String MAKER_ID = "maker_id";
        public static final String MAKER = "maker";
        public static final String ORIGIN = "origin";
        public static final String PRICE = "price";
        public static final String LOCATION = "location";
        public static final String DATE = "date";
        public static final String RATING = "rating";
        public static final String NOTES = "notes";

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".entry";
        public static final String DATA_TYPE_ITEM =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".entry";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_BASE + TABLE_NAME + "/");
        public static final Uri CONTENT_FILTER_URI_BASE =
                Uri.parse(URI_BASE + TABLE_NAME + "/filter/");

        private Entries() {
        }
    }

    /**
     * Data contract for the 'entries_extras' table.
     *
     * @author Steve Guidetti
     */
    public static class EntriesExtras implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "entries_extras";
        public static final String VIEW_NAME = "view_entry_extra";

        /**
         * Column names
         */
        public static final String ENTRY = "entry";
        public static final String EXTRA = "extra";
        public static final String VALUE = "value";

        private EntriesExtras() {
        }
    }

    /**
     * Data contract for the 'entries_flavors' table.
     *
     * @author Steve Guidetti
     */
    public static class EntriesFlavors implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "entries_flavors";

        /**
         * Column names
         */
        public static final String ENTRY = "entry";
        public static final String FLAVOR = "flavor";
        public static final String VALUE = "value";

        private EntriesFlavors() {
        }
    }

    /**
     * Data contract for the 'extras' table.
     *
     * @author Steve Guidetti
     */
    public static class Extras implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "extras";

        /**
         * Column names
         */
        public static final String CAT = "cat";
        public static final String NAME = "name";
        public static final String PRESET = "preset";
        public static final String DELETED = "deleted";

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".extra";
        public static final String DATA_TYPE_ITEM =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".extra";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_BASE + TABLE_NAME + "/");

        private Extras() {
        }

        /**
         * Beer preset extra names
         */
        public static class Beer {
            public static final String STYLE = "_style";
            public static final String SERVING = "_serving";
            public static final String STATS_IBU = "_stats_ibu";
            public static final String STATS_ABV = "_stats_abv";
            public static final String STATS_OG = "_stats_og";
            public static final String STATS_FG = "_stats_fg";
        }

        /**
         * Wine preset extra names
         */
        public static class Wine {
            public static final String VARIETAL = "_varietal";
            public static final String STATS_VINTAGE = "_stats_vintage";
            public static final String STATS_ABV = "_stats_abv";
        }

        /**
         * Whiskey preset extra names
         */
        public static class Whiskey {
            public static final String TYPE = "_type";
            public static final String STATS_AGE = "_stats_age";
            public static final String STATS_ABV = "_stats_abv";
        }

        /**
         * Coffee preset extra names
         */
        public static class Coffee {
            public static final String ROASTER = "_roaster";
            public static final String ROAST_DATE = "_roast_date";
            public static final String GRIND = "_grind";
            public static final String BREW_METHOD = "_brew_method";
            public static final String STATS_DOSE = "_stats_dose";
            public static final String STATS_MASS = "_stats_mass";
            public static final String STATS_TEMP = "_stats_temp";
            public static final String STATS_EXTIME = "_stats_extime";
            public static final String STATS_TDS = "_stats_tds";
            public static final String STATS_YIELD = "_stats_yield";
        }
    }

    /**
     * Data contract for the 'flavors' table.
     *
     * @author Steve Guidetti
     */
    public static class Flavors implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "flavors";

        /**
         * Column names
         */
        public static final String CAT = "cat";
        public static final String NAME = "name";

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".flavor";
        public static final String DATA_TYPE_ITEM =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".flavor";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_BASE + TABLE_NAME + "/");

        private Flavors() {
        }
    }

    /**
     * Data contract for the 'makers' table.
     *
     * @author Steve Guidetti
     */
    public static class Makers implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "makers";

        /**
         * Column names
         */
        public static final String NAME = "name";
        public static final String LOCATION = "location";

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".maker";
        public static final String DATA_TYPE_ITEM =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".maker";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_BASE + TABLE_NAME + "/");
        public static final Uri CONTENT_FILTER_URI_BASE =
                Uri.parse(URI_BASE + TABLE_NAME + "/filter/");

        private Makers() {
        }
    }

    /**
     * Data contract for the 'photos' table.
     *
     * @author Steve Guidetti
     */
    public static class Photos implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "photos";

        /**
         * Column names
         */
        public static final String ENTRY = "entry";
        public static final String PATH = "path";

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".photo";
        public static final String DATA_TYPE_ITEM =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".photo";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_BASE + TABLE_NAME + "/");

        private Photos() {
        }
    }

    /**
     * Data contract for the 'locations' table.
     *
     * @author Steve Guidetti
     */
    public static class Locations implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "locations";

        /**
         * Column names
         */
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lon";
        public static final String NAME = "name";

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".location";
        public static final String DATA_TYPE_ITEM =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".location";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_BASE + TABLE_NAME + "/");

        private Locations() {
        }
    }

    /**
     * Data contract for the 'cats' table.
     *
     * @author Steve Guidetti
     */
    public static class Cats implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "cats";
        public static final String VIEW_NAME = "view_cat";

        /**
         * Column names
         */
        public static final String NAME = "name";
        public static final String PRESET = "preset";
        public static final String NUM_ENTRIES = "num_entries";

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".cat";
        public static final String DATA_TYPE_ITEM =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".cat";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(URI_BASE + TABLE_NAME + "/");

        private Cats() {
        }
    }
}
