/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.ultramegasoft.flavordex2.FlavordexApp;

/**
 * Constants for accessing database records.
 *
 * @author Steve Guidetti
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Tables {
    /**
     * The Authority string for the application
     */
    private static final String AUTHORITY = FlavordexApp.AUTHORITY;

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
        public static final String UUID = "uuid";
        public static final String TITLE = "title";
        public static final String CAT_ID = "cat_id";
        public static final String CAT_UUID = "cat_uuid";
        public static final String CAT = "cat";
        public static final String MAKER_ID = "maker_id";
        public static final String MAKER = "maker";
        public static final String ORIGIN = "origin";
        public static final String PRICE = "price";
        public static final String LOCATION = "location";
        public static final String DATE = "date";
        public static final String RATING = "rating";
        public static final String NOTES = "notes";
        public static final String UPDATED = "updated";
        public static final String PUBLISHED = "published";
        public static final String SYNCED = "synced";

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
        public static final Uri CONTENT_CAT_URI_BASE =
                Uri.parse(URI_BASE + TABLE_NAME + "/cat/");

        /**
         * Get the Uri for an entry's extra fields.
         *
         * @param entryId The entry ID
         * @return The Uri for the entry's extra fields
         */
        public static Uri getExtrasUri(long entryId) {
            final Uri baseUri = ContentUris.withAppendedId(CONTENT_ID_URI_BASE, entryId);
            return Uri.withAppendedPath(baseUri, "extras");
        }

        /**
         * Get the Uri for an entry's flavors.
         *
         * @param entryId The entry ID
         * @return The Uri for the entry's flavors
         */
        public static Uri getFlavorUri(long entryId) {
            final Uri baseUri = ContentUris.withAppendedId(CONTENT_ID_URI_BASE, entryId);
            return Uri.withAppendedPath(baseUri, "flavor");
        }

        /**
         * Get the Uri for an entry's photos.
         *
         * @param entryId The entry ID
         * @return The Uri for the entry's photos
         */
        public static Uri getPhotoUri(long entryId) {
            final Uri baseUri = ContentUris.withAppendedId(CONTENT_ID_URI_BASE, entryId);
            return Uri.withAppendedPath(baseUri, "photos");
        }

        /**
         * Create a category entry filter Uri.
         *
         * @param catId      The category ID
         * @param filterText The filter text
         * @return The category filter Uri
         */
        public static Uri getCatFilterUri(long catId, String filterText) {
            final Uri baseUri = ContentUris.withAppendedId(CONTENT_CAT_URI_BASE, catId);
            return Uri.withAppendedPath(baseUri, "filter/" + Uri.encode(filterText));
        }

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
        public static final String POS = "pos";

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
        public static final String UUID = "uuid";
        public static final String CAT = "cat";
        public static final String NAME = "name";
        public static final String POS = "pos";
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
            public static final String STYLE = "_style";
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
        public static final String POS = "pos";

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
        public static final String HASH = "hash";
        public static final String PATH = "path";
        public static final String DRIVE_ID = "drive_id";
        public static final String POS = "pos";

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
        public static final String UUID = "uuid";
        public static final String NAME = "name";
        public static final String PRESET = "preset";
        public static final String UPDATED = "updated";
        public static final String PUBLISHED = "published";
        public static final String SYNCED = "synced";
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

        /**
         * Get the Uri for an category's extra fields.
         *
         * @param catId The category ID
         * @return The Uri for the category's extra fields
         */
        public static Uri getExtrasUri(long catId) {
            final Uri baseUri = ContentUris.withAppendedId(CONTENT_ID_URI_BASE, catId);
            return Uri.withAppendedPath(baseUri, "extras");
        }

        /**
         * Get the Uri for an category's flavors.
         *
         * @param catId The category ID
         * @return The Uri for the category's flavors
         */
        public static Uri getFlavorUri(long catId) {
            final Uri baseUri = ContentUris.withAppendedId(CONTENT_ID_URI_BASE, catId);
            return Uri.withAppendedPath(baseUri, "flavor");
        }

        private Cats() {
        }
    }

    /**
     * Data contract for the 'deleted' table.
     *
     * @author Steve Guidetti
     */
    public static class Deleted implements BaseColumns {
        /**
         * Table names
         */
        public static final String TABLE_NAME = "deleted";

        /**
         * Column names
         */
        public static final String TYPE = "type";
        public static final String CAT = "cat";
        public static final String UUID = "uuid";
        public static final String TIME = "time";

        /**
         * Values for the 'type' column
         */
        public static final int TYPE_CAT = 0;
        public static final int TYPE_ENTRY = 1;
        public static final int TYPE_PHOTO = 2;

        /**
         * Content data types
         */
        public static final String DATA_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".deleted";

        /**
         * Content Uris
         */
        public static final Uri CONTENT_URI = Uri.parse(URI_BASE + TABLE_NAME);

        private Deleted() {
        }
    }
}
