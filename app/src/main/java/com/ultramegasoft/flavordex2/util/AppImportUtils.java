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
package com.ultramegasoft.flavordex2.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.BaseColumns;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;

import java.util.ArrayList;

/**
 * Helpers for importing journal entries from the original Flavordex apps.
 *
 * @author Steve Guidetti
 */
public class AppImportUtils {
    /**
     * Application IDs
     */
    private static final int APP_FD2_LITE = 0;
    private static final int APP_BEER = 1;
    private static final int APP_WINE = 2;
    private static final int APP_WHISKEY = 3;
    private static final int APP_COFFEE = 4;

    /**
     * The package names for the apps
     */
    private static final String[] sPackageNames = new String[] {
            "com.ultramegasoft.flavordex2.lite",
            "com.flavordex.beer",
            "com.flavordex.wine",
            "com.flavordex.whiskey",
            "com.flavordex.coffee"
    };

    /**
     * Lists of extra column names for each app
     */
    private static final String[][] sExtraColumns = new String[][] {
            null,
            new String[] {
                    BeerColumns.STYLE,
                    BeerColumns.SERVING,
                    BeerColumns.STATS_IBU,
                    BeerColumns.STATS_ABV,
                    BeerColumns.STATS_OG,
                    BeerColumns.STATS_FG
            },
            new String[] {
                    WineColumns.VARIETAL,
                    WineColumns.STATS_VINTAGE,
                    WineColumns.STATS_ABV
            },
            new String[] {
                    WhiskeyColumns.TYPE,
                    WhiskeyColumns.STATS_AGE,
                    WhiskeyColumns.STATS_ABV
            },
            new String[] {
                    CoffeeColumns.ROASTER,
                    CoffeeColumns.ROAST_DATE,
                    CoffeeColumns.GRIND,
                    CoffeeColumns.BREW_METHOD,
                    CoffeeColumns.STATS_DOSE,
                    CoffeeColumns.STATS_MASS,
                    CoffeeColumns.STATS_TEMP,
                    CoffeeColumns.STATS_EXTIME,
                    CoffeeColumns.STATS_TDS,
                    CoffeeColumns.STATS_YIELD
            }
    };

    /**
     * Get information about all of the currently installed original Flavordex apps.
     *
     * @param pm The PackageManager
     * @return List of AppHolder objects for each app
     */
    public static ArrayList<AppHolder> getInstalledApps(PackageManager pm) {
        final ArrayList<AppHolder> apps = new ArrayList<>();

        ApplicationInfo appInfo;
        AppHolder appHolder;
        for(int i = 0; i < sPackageNames.length; i++) {
            try {
                appInfo = pm.getApplicationInfo(sPackageNames[i], 0);

                appHolder = new AppHolder(i);
                appHolder.icon = pm.getApplicationIcon(appInfo);
                appHolder.title = pm.getApplicationLabel(appInfo);
                appHolder.supported = getProviderInfo(pm, i).exported;

                apps.add(appHolder);
            } catch(PackageManager.NameNotFoundException ignored) {
            }
        }

        return apps;
    }

    /**
     * Check if any original Flavordex app is installed on the device.
     *
     * @param context The Context
     * @return Whether any app is installed
     */
    public static boolean isAnyAppInstalled(Context context) {
        for(int i = 0; i < sPackageNames.length; i++) {
            if(isAppInstalled(context, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an app is installed on the device.
     *
     * @param context The Context
     * @param app     The app
     * @return Whether the app is installed
     */
    private static boolean isAppInstalled(Context context, int app) {
        return isAppInstalled(context, app, false);
    }

    /**
     * Check if an app is installed on the device.
     *
     * @param context      The Context
     * @param app          The app
     * @param checkSupport Check whether the app supports importing
     * @return Whether the app is installed
     */
    public static boolean isAppInstalled(Context context, int app, boolean checkSupport) {
        final ProviderInfo pi = getProviderInfo(context.getPackageManager(), app);
        return pi != null && (!checkSupport || pi.exported);
    }

    /**
     * Get the information about an app's ContentProvider.
     *
     * @param pm  The PackageManager
     * @param app The app
     * @return The ProviderInfo for the app's ContentProvider
     */
    private static ProviderInfo getProviderInfo(PackageManager pm, int app) {
        return pm.resolveContentProvider(sPackageNames[app] + ".provider", 0);
    }

    /**
     * Get the content Uri for the app entries.
     *
     * @param app The app
     * @return The content Uri
     */
    public static Uri getEntriesUri(int app) {
        return Uri.parse("content://" + sPackageNames[app] + ".provider/entries");
    }

    /**
     * Get the list of flavor names for the app.
     *
     * @param context The Context
     * @param app     The app
     * @return The list of flavor names
     */
    private static String[] getFlavorNames(Context context, int app) {
        final Resources res = context.getResources();
        switch(app) {
            case APP_BEER:
                return res.getStringArray(R.array.beer_flavor_names);
            case APP_WINE:
                return res.getStringArray(R.array.wine_flavor_names);
            case APP_WHISKEY:
                return res.getStringArray(R.array.whiskey_flavor_names);
            case APP_COFFEE:
                return res.getStringArray(R.array.coffee_flavor_names);
        }
        return new String[0];
    }

    /**
     * Import an entry from a Flavordex app.
     *
     * @param context  The Context
     * @param app      The source app
     * @param sourceId The ID of the source entry
     * @return The imported entry
     */
    public static EntryHolder importEntry(Context context, int app, long sourceId) {
        if(app == APP_FD2_LITE) {
            return importFd2LiteEntry(context, sourceId);
        } else {
            return importLegacyEntry(context, app, sourceId);
        }
    }

    /**
     * Import an entry from an original Flavordex app.
     *
     * @param context  The Context
     * @param app      The source app
     * @param sourceId The ID of the source entry
     * @return The imported entry
     */
    private static EntryHolder importLegacyEntry(Context context, int app, long sourceId) {
        final EntryHolder entry = new EntryHolder();
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(getEntriesUri(app), sourceId);
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    entry.title = cursor.getString(cursor.getColumnIndex(EntriesColumns.TITLE));
                    entry.maker = cursor.getString(cursor.getColumnIndex(EntriesColumns.MAKER));
                    entry.origin = cursor.getString(cursor.getColumnIndex(EntriesColumns.ORIGIN));
                    entry.location =
                            cursor.getString(cursor.getColumnIndex(EntriesColumns.LOCATION));
                    entry.date = cursor.getLong(cursor.getColumnIndex(EntriesColumns.DATE));
                    entry.price = cursor.getString(cursor.getColumnIndex(EntriesColumns.PRICE));
                    entry.rating = cursor.getFloat(cursor.getColumnIndex(EntriesColumns.RATING));
                    entry.notes = cursor.getString(cursor.getColumnIndex(EntriesColumns.NOTES));

                    getExtras(sExtraColumns[app], cursor, entry);
                }
            } finally {
                cursor.close();
            }
        }

        getFlavors(context, app, uri, entry);
        getPhotos(context, uri, entry);

        switch(app) {
            case APP_BEER:
                entry.catName = FlavordexApp.CAT_BEER;
                break;
            case APP_WINE:
                entry.catName = FlavordexApp.CAT_WINE;
                break;
            case APP_WHISKEY:
                entry.catName = FlavordexApp.CAT_WHISKEY;
                break;
            case APP_COFFEE:
                entry.catName = FlavordexApp.CAT_COFFEE;
                break;
        }

        return entry;
    }

    /**
     * Insert the extra fields from the source entry into the new local entry.
     *
     * @param extraColumns The list of extra columns from the source entry
     * @param cursor       The Cursor for the source entry row
     * @param entry        The new local entry
     */
    private static void getExtras(String[] extraColumns, Cursor cursor, EntryHolder entry) {
        String name;
        String value;
        for(String column : extraColumns) {
            name = "_" + column;
            value = cursor.getString(cursor.getColumnIndex(column));
            entry.addExtra(0, name, true, value);
        }
    }

    /**
     * Insert the flavors from the source entry into the new local entry.
     *
     * @param context   The Context
     * @param app       The app
     * @param sourceUri The entry Uri from the source app
     * @param entry     The new local entry
     */
    private static void getFlavors(Context context, int app, Uri sourceUri, EntryHolder entry) {
        final ContentResolver cr = context.getContentResolver();
        final String[] names = getFlavorNames(context, app);
        final Cursor cursor = cr.query(Uri.withAppendedPath(sourceUri, "flavor"), null, null, null,
                FlavorsColumns.FLAVOR + " ASC");
        if(cursor != null) {
            try {
                if(cursor.getCount() != names.length) {
                    return;
                }

                String name;
                int value;
                while(cursor.moveToNext()) {
                    name = names[cursor.getInt(cursor.getColumnIndex(FlavorsColumns.FLAVOR))];
                    value = cursor.getInt(cursor.getColumnIndex(FlavorsColumns.VALUE));
                    entry.addFlavor(name, value);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Insert the photos from the source entry into the new local entry.
     *
     * @param context   The Context
     * @param sourceUri The entry Uri from the source app
     * @param entry     The new local entry
     */
    private static void getPhotos(Context context, Uri sourceUri, EntryHolder entry) {
        final ContentResolver cr = context.getContentResolver();
        final Cursor cursor = cr.query(Uri.withAppendedPath(sourceUri, "photos"), null, null, null,
                PhotosColumns._ID + " ASC");
        if(cursor != null) {
            try {
                String path;
                Uri uri;
                while(cursor.moveToNext()) {
                    path = cursor.getString(cursor.getColumnIndex(PhotosColumns.PATH));
                    uri = PhotoUtils.parsePath(path);
                    entry.addPhoto(0, PhotoUtils.getMD5Hash(cr, uri), uri);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Import an entry from Flavordex 2 Lite.
     *
     * @param context  The Context
     * @param sourceId The ID of the source entry
     * @return The imported entry
     */
    private static EntryHolder importFd2LiteEntry(Context context, long sourceId) {
        final EntryHolder entry = new EntryHolder();
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(getEntriesUri(APP_FD2_LITE), sourceId);
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    if(cursor.getColumnIndex(Tables.Entries.UUID) > -1) {
                        entry.uuid = cursor.getString(cursor.getColumnIndex(Tables.Entries.UUID));
                    }
                    entry.title = cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                    entry.catName = cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT));
                    entry.maker = cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER));
                    entry.origin = cursor.getString(cursor.getColumnIndex(Tables.Entries.ORIGIN));
                    entry.price = cursor.getString(cursor.getColumnIndex(Tables.Entries.PRICE));
                    entry.location =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.LOCATION));
                    entry.date = cursor.getLong(cursor.getColumnIndex(Tables.Entries.DATE));
                    entry.rating = cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
                    entry.notes = cursor.getString(cursor.getColumnIndex(Tables.Entries.NOTES));

                    getFd2LiteExtras(context, uri, entry);
                    getFd2LiteFlavors(context, uri, entry);
                    getFd2LitePhotos(context, uri, entry);
                }
            } finally {
                cursor.close();
            }
        }

        return entry;
    }

    /**
     * Insert the extra fields from the Flavordex 2 Lite source entry into the new local entry.
     *
     * @param context   The Context
     * @param sourceUri The entry Uri from the source app
     * @param entry     The new local entry
     */
    private static void getFd2LiteExtras(Context context, Uri sourceUri, EntryHolder entry) {
        final ContentResolver cr = context.getContentResolver();
        final Cursor cursor =
                cr.query(Uri.withAppendedPath(sourceUri, "extras"), null, null, null, null);
        if(cursor != null) {
            try {
                String name;
                String value;
                while(cursor.moveToNext()) {
                    name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
                    value = cursor.getString(cursor.getColumnIndex(Tables.EntriesExtras.VALUE));
                    entry.addExtra(0, name, true, value);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Insert the flavors from the Flavordex 2 Lite source entry into the new local entry.
     *
     * @param context   The Context
     * @param sourceUri The entry Uri from the source app
     * @param entry     The new local entry
     */
    private static void getFd2LiteFlavors(Context context, Uri sourceUri, EntryHolder entry) {
        final ContentResolver cr = context.getContentResolver();
        final Cursor cursor =
                cr.query(Uri.withAppendedPath(sourceUri, "flavor"), null, null, null, null);
        if(cursor != null) {
            try {
                String name;
                int value;
                while(cursor.moveToNext()) {
                    name = cursor.getString(cursor.getColumnIndex(Tables.EntriesFlavors.FLAVOR));
                    value = cursor.getInt(cursor.getColumnIndex(Tables.EntriesFlavors.VALUE));
                    entry.addFlavor(name, value);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Insert the photos from the Flavordex 2 Lite source entry into the new local entry.
     *
     * @param context   The Context
     * @param sourceUri The entry Uri from the source app
     * @param entry     The new local entry
     */
    private static void getFd2LitePhotos(Context context, Uri sourceUri, EntryHolder entry) {
        final ContentResolver cr = context.getContentResolver();
        final Cursor cursor =
                cr.query(Uri.withAppendedPath(sourceUri, "photos"), null, null, null, null);
        if(cursor != null) {
            try {
                String hash;
                String path;
                int pos;
                final ArrayList<PhotoHolder> photos = entry.getPhotos();
                while(cursor.moveToNext()) {
                    hash = cursor.getString(cursor.getColumnIndex(Tables.Photos.HASH));
                    path = cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH));
                    pos = cursor.getInt(cursor.getColumnIndex(Tables.Photos.POS));
                    photos.add(new PhotoHolder(0, hash, PhotoUtils.parsePath(path), pos));
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Holds data about an app.
     */
    public static class AppHolder {
        /**
         * The app identifier
         */
        public final int app;

        /**
         * The app icon
         */
        public Drawable icon;

        /**
         * The name of the app
         */
        public CharSequence title;

        /**
         * Whether the app supports importing
         */
        public boolean supported;

        /**
         * @param app The app identifier
         */
        private AppHolder(int app) {
            this.app = app;
        }
    }

    /**
     * Common column names for the entries table.
     */
    public static class EntriesColumns implements BaseColumns {
        public static final String TITLE = "title";
        public static final String MAKER = "maker";
        public static final String ORIGIN = "origin";
        public static final String LOCATION = "location";
        public static final String DATE = "date";
        public static final String PRICE = "price";
        public static final String RATING = "rating";
        public static final String NOTES = "notes";
    }

    /**
     * Extra column names for the beer entries table.
     */
    private static final class BeerColumns extends EntriesColumns {
        static final String STYLE = "style";
        static final String SERVING = "serving";
        static final String STATS_IBU = "stats_ibu";
        static final String STATS_ABV = "stats_abv";
        static final String STATS_OG = "stats_og";
        static final String STATS_FG = "stats_fg";
    }

    /**
     * Extra column names for the wine entries table.
     */
    private static final class WineColumns extends EntriesColumns {
        static final String VARIETAL = "varietal";
        static final String STATS_VINTAGE = "stats_vintage";
        static final String STATS_ABV = "stats_abv";
    }

    /**
     * Extra column names for the whiskey entries table.
     */
    private static final class WhiskeyColumns extends EntriesColumns {
        static final String TYPE = "style";
        static final String STATS_AGE = "stats_age";
        static final String STATS_ABV = "stats_abv";
    }

    /**
     * Extra column names for the coffee entries table.
     */
    private static final class CoffeeColumns extends EntriesColumns {
        static final String ROASTER = "roaster";
        static final String ROAST_DATE = "roast_date";
        static final String GRIND = "grind";
        static final String BREW_METHOD = "brew_method";
        static final String STATS_DOSE = "stats_dose";
        static final String STATS_MASS = "stats_mass";
        static final String STATS_TEMP = "stats_temp";
        static final String STATS_EXTIME = "stats_extime";
        static final String STATS_TDS = "stats_tds";
        static final String STATS_YIELD = "stats_yield";
    }

    /**
     * Column names for the flavors table.
     */
    private static class FlavorsColumns implements BaseColumns {
        static final String FLAVOR = "flavor";
        static final String VALUE = "value";
    }

    /**
     * Column names for the photos table.
     */
    private static class PhotosColumns implements BaseColumns {
        static final String PATH = "path";
    }
}
