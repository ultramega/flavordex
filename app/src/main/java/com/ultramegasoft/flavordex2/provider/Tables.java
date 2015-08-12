package com.ultramegasoft.flavordex2.provider;

import android.provider.BaseColumns;

/**
 * Constants for accessing database records.
 *
 * @author Steve Guidetti
 */
public class Tables {
    /**
     * Data contract for the 'entries' table and view.
     *
     * @author Steve Guidetti
     */
    public static class Entries implements BaseColumns {
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

    /**
     * Data contract for the 'entries_extras' table.
     *
     * @author Steve Guidetti
     */
    public static class EntriesExtras implements BaseColumns {
        public static final String TABLE_NAME = "entries_extras";

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
        public static final String TABLE_NAME = "entries_flavors";

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
        public static final String TABLE_NAME = "extras";

        public static final String TYPE = "type";
        public static final String NAME = "name";
        public static final String PRESET = "preset";
        public static final String DELETED = "deleted";

        private Extras() {
        }
    }

    /**
     * Data contract for the 'flavors' table.
     *
     * @author Steve Guidetti
     */
    public static class Flavors implements BaseColumns {
        public static final String TABLE_NAME = "flavors";

        public static final String TYPE = "type";
        public static final String NAME = "name";
        public static final String DELETED = "deleted";

        private Flavors() {
        }
    }

    /**
     * Data contract for the 'makers' table.
     *
     * @author Steve Guidetti
     */
    public static class Makers implements BaseColumns {
        public static final String TABLE_NAME = "makers";

        public static final String TYPE = "type";
        public static final String NAME = "name";
        public static final String LOCATION = "location";

        private Makers() {
        }
    }

    /**
     * Data contract for the 'photos' table.
     *
     * @author Steve Guidetti
     */
    public static class Photos implements BaseColumns {
        public static final String TABLE_NAME = "photos";

        public static final String ENTRY = "entry";
        public static final String PATH = "path";
        public static final String FROM_GALLERY = "from_gallery";

        private Photos() {
        }
    }

    /**
     * Data contract for the 'locations' table.
     *
     * @author Steve Guidetti
     */
    public static class Locations implements BaseColumns {
        public static final String TABLE_NAME = "locations";

        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lon";
        public static final String NAME = "name";

        private Locations() {
        }
    }

    /**
     * Data contract for the 'types' table.
     *
     * @author Steve Guidetti
     */
    public static class Types implements BaseColumns {
        public static final String TABLE_NAME = "types";

        public static final String TYPE = "type";
        public static final String NAME = "name";
        public static final String PRESET = "preset";

        private Types() {
        }
    }
}
