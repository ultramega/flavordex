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

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ultramegasoft.flavordex2.FlavordexApp;

import java.util.UUID;

/**
 * The ContentProvider backing the application.
 *
 * @author Steve Guidetti
 */
public class FlavordexProvider extends ContentProvider {
    private static final String TAG = "FlavordexProvider";

    /**
     * The Authority string for this ContentProvider
     */
    private static final String AUTHORITY = FlavordexApp.AUTHORITY;

    /**
     * URI identifier codes
     */
    private static final int ENTRIES = 1;
    private static final int ENTRIES_ID = 2;
    private static final int ENTRIES_FILTER = 3;
    private static final int ENTRIES_CAT = 4;
    private static final int ENTRIES_CAT_FILTER = 5;
    private static final int ENTRIES_EXTRAS = 6;
    private static final int ENTRIES_FLAVOR = 7;
    private static final int ENTRIES_PHOTOS = 8;
    private static final int CATS = 9;
    private static final int CATS_ID = 10;
    private static final int CATS_EXTRAS = 11;
    private static final int CATS_FLAVOR = 12;
    private static final int EXTRAS = 13;
    private static final int EXTRAS_ID = 14;
    private static final int FLAVORS = 15;
    private static final int FLAVORS_ID = 16;
    private static final int PHOTOS = 17;
    private static final int PHOTOS_ID = 18;
    private static final int MAKERS = 19;
    private static final int MAKERS_ID = 20;
    private static final int MAKERS_FILTER = 21;
    private static final int LOCATIONS = 22;
    private static final int LOCATIONS_ID = 23;
    private static final int DELETED = 24;

    /**
     * The UriMatcher to use
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, "entries", ENTRIES);
        sUriMatcher.addURI(AUTHORITY, "entries/#", ENTRIES_ID);
        sUriMatcher.addURI(AUTHORITY, "entries/filter/*", ENTRIES_FILTER);
        sUriMatcher.addURI(AUTHORITY, "entries/cat/#", ENTRIES_CAT);
        sUriMatcher.addURI(AUTHORITY, "entries/cat/#/filter/*", ENTRIES_CAT_FILTER);
        sUriMatcher.addURI(AUTHORITY, "entries/#/extras", ENTRIES_EXTRAS);
        sUriMatcher.addURI(AUTHORITY, "entries/#/flavor", ENTRIES_FLAVOR);
        sUriMatcher.addURI(AUTHORITY, "entries/#/photos", ENTRIES_PHOTOS);
        sUriMatcher.addURI(AUTHORITY, "cats", CATS);
        sUriMatcher.addURI(AUTHORITY, "cats/#", CATS_ID);
        sUriMatcher.addURI(AUTHORITY, "cats/#/extras", CATS_EXTRAS);
        sUriMatcher.addURI(AUTHORITY, "cats/#/flavor", CATS_FLAVOR);
        sUriMatcher.addURI(AUTHORITY, "extras", EXTRAS);
        sUriMatcher.addURI(AUTHORITY, "extras/#", EXTRAS_ID);
        sUriMatcher.addURI(AUTHORITY, "flavors", FLAVORS);
        sUriMatcher.addURI(AUTHORITY, "flavors/#", FLAVORS_ID);
        sUriMatcher.addURI(AUTHORITY, "photos", PHOTOS);
        sUriMatcher.addURI(AUTHORITY, "photos/#", PHOTOS_ID);
        sUriMatcher.addURI(AUTHORITY, "makers", MAKERS);
        sUriMatcher.addURI(AUTHORITY, "makers/#", MAKERS_ID);
        sUriMatcher.addURI(AUTHORITY, "makers/filter/*", MAKERS_FILTER);
        sUriMatcher.addURI(AUTHORITY, "locations", LOCATIONS);
        sUriMatcher.addURI(AUTHORITY, "locations/#", LOCATIONS_ID);
        sUriMatcher.addURI(AUTHORITY, "deleted", DELETED);
    }

    /**
     * The helper to access the database
     */
    private DatabaseHelper mDbHelper;

    /**
     * The BackupManager to notify of data changes
     */
    private BackupManager mBackupManager;

    /**
     * The ContentResolver
     */
    private ContentResolver mResolver;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        if(context == null) {
            return false;
        }
        mDbHelper = new DatabaseHelper(getContext());
        mBackupManager = new BackupManager(getContext());
        //noinspection ConstantConditions
        mResolver = getContext().getContentResolver();
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch(sUriMatcher.match(uri)) {
            case ENTRIES:
            case ENTRIES_FILTER:
            case ENTRIES_CAT:
            case ENTRIES_CAT_FILTER:
                return Tables.Entries.DATA_TYPE;
            case ENTRIES_ID:
                return Tables.Entries.DATA_TYPE_ITEM;
            case CATS:
                return Tables.Cats.DATA_TYPE;
            case CATS_ID:
                return Tables.Cats.DATA_TYPE_ITEM;
            case EXTRAS:
            case ENTRIES_EXTRAS:
            case CATS_EXTRAS:
                return Tables.Extras.DATA_TYPE;
            case EXTRAS_ID:
                return Tables.Extras.DATA_TYPE_ITEM;
            case FLAVORS:
            case ENTRIES_FLAVOR:
            case CATS_FLAVOR:
                return Tables.Flavors.DATA_TYPE;
            case FLAVORS_ID:
                return Tables.Flavors.DATA_TYPE_ITEM;
            case PHOTOS:
            case ENTRIES_PHOTOS:
                return Tables.Photos.DATA_TYPE;
            case PHOTOS_ID:
                return Tables.Photos.DATA_TYPE_ITEM;
            case MAKERS:
            case MAKERS_FILTER:
                return Tables.Makers.DATA_TYPE;
            case MAKERS_ID:
                return Tables.Makers.DATA_TYPE_ITEM;
            case LOCATIONS:
                return Tables.Locations.DATA_TYPE;
            case LOCATIONS_ID:
                return Tables.Locations.DATA_TYPE_ITEM;
            case DELETED:
                return Tables.Deleted.DATA_TYPE;
        }

        return null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Uri notifyUri = uri;
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch(sUriMatcher.match(uri)) {
            case ENTRIES:
                queryBuilder.setTables(Tables.Entries.VIEW_NAME);
                break;
            case ENTRIES_ID:
                queryBuilder.setTables(Tables.Entries.VIEW_NAME);
                queryBuilder.appendWhere(Tables.Entries._ID + " = " + uri.getLastPathSegment());
                break;
            case ENTRIES_FILTER:
                queryBuilder.setTables(Tables.Entries.VIEW_NAME);
                queryBuilder.appendWhere(Tables.Entries.TITLE + " LIKE ");
                queryBuilder.appendWhereEscapeString("%" + uri.getLastPathSegment() + "%");
                notifyUri = Tables.Entries.CONTENT_URI;
                break;
            case ENTRIES_CAT:
                queryBuilder.setTables(Tables.Entries.VIEW_NAME);
                queryBuilder.appendWhere(Tables.Entries.CAT_ID + " = " + uri.getLastPathSegment());
                notifyUri = Tables.Entries.CONTENT_URI;
                break;
            case ENTRIES_CAT_FILTER:
                queryBuilder.setTables(Tables.Entries.VIEW_NAME);
                queryBuilder.appendWhere(Tables.Entries.CAT_ID + " = "
                        + uri.getPathSegments().get(2));
                queryBuilder.appendWhere(" AND " + Tables.Entries.TITLE + " LIKE ");
                queryBuilder.appendWhereEscapeString("%" + uri.getLastPathSegment() + "%");
                notifyUri = Tables.Entries.CONTENT_URI;
                break;
            case CATS:
                queryBuilder.setTables(Tables.Cats.VIEW_NAME);
                break;
            case CATS_ID:
                queryBuilder.setTables(Tables.Cats.VIEW_NAME);
                queryBuilder.appendWhere(Tables.Cats._ID + " = " + uri.getLastPathSegment());
                break;
            case EXTRAS:
                queryBuilder.setTables(Tables.Extras.TABLE_NAME);
                break;
            case EXTRAS_ID:
                queryBuilder.setTables(Tables.Extras.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Extras._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS_EXTRAS:
                queryBuilder.setTables(Tables.Extras.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Extras.CAT + " = " + uri.getPathSegments().get(1));
                break;
            case ENTRIES_EXTRAS:
                queryBuilder.setTables(Tables.EntriesExtras.VIEW_NAME);
                queryBuilder.appendWhere(Tables.EntriesExtras.ENTRY + " = "
                        + uri.getPathSegments().get(1));
                break;
            case FLAVORS:
                queryBuilder.setTables(Tables.Flavors.TABLE_NAME);
                break;
            case FLAVORS_ID:
                queryBuilder.setTables(Tables.Flavors.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Flavors._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS_FLAVOR:
                queryBuilder.setTables(Tables.Flavors.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Flavors.CAT + " = "
                        + uri.getPathSegments().get(1));
                break;
            case ENTRIES_FLAVOR:
                queryBuilder.setTables(Tables.EntriesFlavors.TABLE_NAME);
                queryBuilder.appendWhere(Tables.EntriesFlavors.ENTRY + " = "
                        + uri.getPathSegments().get(1));
                break;
            case PHOTOS:
                queryBuilder.setTables(Tables.Photos.TABLE_NAME);
                break;
            case PHOTOS_ID:
                queryBuilder.setTables(Tables.Photos.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Photos._ID + " = " + uri.getLastPathSegment());
                break;
            case ENTRIES_PHOTOS:
                queryBuilder.setTables(Tables.Photos.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Photos.ENTRY + " = "
                        + uri.getPathSegments().get(1));
                break;
            case MAKERS:
                queryBuilder.setTables(Tables.Makers.TABLE_NAME);
                break;
            case MAKERS_ID:
                queryBuilder.setTables(Tables.Makers.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Makers._ID + " = " + uri.getLastPathSegment());
                break;
            case MAKERS_FILTER:
                queryBuilder.setTables(Tables.Makers.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Makers.NAME + " LIKE ");
                queryBuilder.appendWhereEscapeString("%" + uri.getLastPathSegment() + "%");
                break;
            case LOCATIONS:
                queryBuilder.setTables(Tables.Locations.TABLE_NAME);
                break;
            case LOCATIONS_ID:
                queryBuilder.setTables(Tables.Locations.TABLE_NAME);
                queryBuilder.appendWhere(Tables.Locations._ID + " = " + uri.getLastPathSegment());
                break;
            case DELETED:
                queryBuilder.setTables(Tables.Deleted.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri.toString());
        }

        final Cursor cursor = queryBuilder.query(mDbHelper.getReadableDatabase(), projection,
                selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(mResolver, notifyUri);

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        String table;
        values = new ContentValues(values);

        switch(sUriMatcher.match(uri)) {
            case ENTRIES:
                table = Tables.Entries.TABLE_NAME;
                if(!values.containsKey(Tables.Entries.UUID)) {
                    values.put(Tables.Entries.UUID, UUID.randomUUID().toString());
                }
                if(values.containsKey(Tables.Entries.MAKER)
                        || values.containsKey(Tables.Entries.ORIGIN)) {
                    processMaker(values);
                }
                break;
            case CATS:
                table = Tables.Cats.TABLE_NAME;
                if(!values.containsKey(Tables.Cats.UUID)) {
                    values.put(Tables.Cats.UUID, UUID.randomUUID().toString());
                }
                values.remove(Tables.Cats.PRESET);
                break;
            case EXTRAS:
                table = Tables.Extras.TABLE_NAME;
                if(!values.containsKey(Tables.Extras.UUID)) {
                    values.put(Tables.Extras.UUID, UUID.randomUUID().toString());
                }
                values.remove(Tables.Extras.PRESET);
                break;
            case CATS_EXTRAS:
                table = Tables.Extras.TABLE_NAME;
                if(!values.containsKey(Tables.Extras.UUID)) {
                    values.put(Tables.Extras.UUID, UUID.randomUUID().toString());
                }
                values.remove(Tables.Extras.PRESET);
                values.put(Tables.Extras.CAT, uri.getPathSegments().get(1));
                break;
            case FLAVORS:
                table = Tables.Flavors.TABLE_NAME;
                break;
            case CATS_FLAVOR:
                table = Tables.Flavors.TABLE_NAME;
                values.put(Tables.Flavors.CAT, uri.getPathSegments().get(1));
                break;
            case ENTRIES_EXTRAS:
                table = Tables.EntriesExtras.TABLE_NAME;
                values.put(Tables.EntriesExtras.ENTRY, uri.getPathSegments().get(1));
                break;
            case ENTRIES_FLAVOR:
                table = Tables.EntriesFlavors.TABLE_NAME;
                values.put(Tables.EntriesFlavors.ENTRY, uri.getPathSegments().get(1));
                break;
            case ENTRIES_PHOTOS:
                table = Tables.Photos.TABLE_NAME;
                values.put(Tables.Photos.ENTRY, uri.getPathSegments().get(1));
                break;
            case PHOTOS:
                table = Tables.Photos.TABLE_NAME;
                break;
            case LOCATIONS:
                table = Tables.Locations.TABLE_NAME;
                break;
            case DELETED:
                table = Tables.Deleted.TABLE_NAME;
                break;
            case ENTRIES_ID:
            case CATS_ID:
            case EXTRAS_ID:
            case FLAVORS_ID:
            case PHOTOS_ID:
            case LOCATIONS_ID:
                throw new IllegalArgumentException("Insert not permitted on: " + uri.toString());
            case ENTRIES_FILTER:
            case ENTRIES_CAT:
            case ENTRIES_CAT_FILTER:
            case MAKERS:
            case MAKERS_ID:
            case MAKERS_FILTER:
                throw new IllegalArgumentException("URI is read-only: " + uri.toString());
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri.toString());
        }

        synchronized(FlavordexProvider.class) {
            try {
                final long id = mDbHelper.getWritableDatabase().insertOrThrow(table, null, values);

                if(id > 0) {
                    mBackupManager.dataChanged();

                    if(Tables.Entries.TABLE_NAME.equals(table)) {
                        mResolver.notifyChange(Tables.Cats.CONTENT_URI, null);
                    }

                    final Uri rowUri = ContentUris.withAppendedId(uri, id);
                    mResolver.notifyChange(rowUri, null);
                    return rowUri;
                }
            } catch(SQLException e) {
                Log.e(TAG, "Failed to insert row into: " + uri.toString(), e);
            }
        }

        return null;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table;
        switch(sUriMatcher.match(uri)) {
            case ENTRIES_FLAVOR:
                table = Tables.EntriesFlavors.TABLE_NAME;
                final long entryId = Long.parseLong(uri.getPathSegments().get(1));
                db.delete(table, Tables.EntriesFlavors.ENTRY + " = " + entryId, null);
                for(ContentValues value : values) {
                    value.put(Tables.EntriesFlavors.ENTRY, entryId);
                    db.insert(table, null, value);
                }
                mResolver.notifyChange(uri, null);
                return values.length;
        }
        return super.bulkInsert(uri, values);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        String table;
        values = new ContentValues(values);

        switch(sUriMatcher.match(uri)) {
            case ENTRIES:
                table = Tables.Entries.TABLE_NAME;
                if(values.containsKey(Tables.Entries.MAKER)
                        || values.containsKey(Tables.Entries.ORIGIN)) {
                    processMaker(values);
                }
                break;
            case ENTRIES_ID:
                table = Tables.Entries.TABLE_NAME;
                if(values.containsKey(Tables.Entries.MAKER)
                        || values.containsKey(Tables.Entries.ORIGIN)) {
                    processMaker(values);
                }
                selection = appendWhere(selection,
                        Tables.Entries._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS:
                table = Tables.Cats.TABLE_NAME;
                values.remove(Tables.Cats.PRESET);
                break;
            case CATS_ID:
                table = Tables.Cats.TABLE_NAME;
                values.remove(Tables.Cats.PRESET);
                selection = appendWhere(selection,
                        Tables.Cats._ID + " = " + uri.getLastPathSegment());
                break;
            case EXTRAS:
                table = Tables.Extras.TABLE_NAME;
                values.remove(Tables.Extras.PRESET);
                break;
            case EXTRAS_ID:
                table = Tables.Extras.TABLE_NAME;
                values.remove(Tables.Extras.PRESET);
                selection = appendWhere(selection,
                        Tables.Extras._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS_EXTRAS:
                table = Tables.Extras.TABLE_NAME;
                values.remove(Tables.Extras.PRESET);
                selection = appendWhere(selection,
                        Tables.Extras.CAT + " = " + uri.getPathSegments().get(1));
                break;
            case ENTRIES_EXTRAS:
                table = Tables.EntriesExtras.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.EntriesExtras.ENTRY + " = " + uri.getPathSegments().get(1));
                break;
            case FLAVORS:
                table = Tables.Flavors.TABLE_NAME;
                break;
            case FLAVORS_ID:
                table = Tables.Flavors.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Flavors._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS_FLAVOR:
                table = Tables.Flavors.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Flavors.CAT + " = " + uri.getPathSegments().get(1));
                break;
            case ENTRIES_FLAVOR:
                table = Tables.EntriesFlavors.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.EntriesFlavors.ENTRY + " = " + uri.getPathSegments().get(1));
                break;
            case PHOTOS:
                table = Tables.Photos.TABLE_NAME;
                break;
            case PHOTOS_ID:
                table = Tables.Photos.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Photos._ID + " = " + uri.getLastPathSegment());
                break;
            case ENTRIES_PHOTOS:
                table = Tables.Photos.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Photos.ENTRY + " = " + uri.getPathSegments().get(1));
                break;
            case LOCATIONS:
            case LOCATIONS_ID:
            case DELETED:
                throw new IllegalArgumentException("Update not permitted on: " + uri.toString());
            case ENTRIES_FILTER:
            case ENTRIES_CAT:
            case ENTRIES_CAT_FILTER:
            case MAKERS:
            case MAKERS_ID:
            case MAKERS_FILTER:
                throw new IllegalArgumentException("URI is read-only: " + uri.toString());
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri.toString());
        }

        synchronized(FlavordexProvider.class) {
            final int count = mDbHelper.getWritableDatabase().update(table, values, selection,
                    selectionArgs);

            if(count > 0) {
                mBackupManager.dataChanged();
                mResolver.notifyChange(uri, null);
            }

            return count;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        String table;

        switch(sUriMatcher.match(uri)) {
            case ENTRIES:
                table = Tables.Entries.TABLE_NAME;
                break;
            case ENTRIES_ID:
                table = Tables.Entries.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Entries._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS:
                table = Tables.Cats.TABLE_NAME;
                selection = appendWhere(selection, Tables.Cats.PRESET + " = 0");
                break;
            case CATS_ID:
                table = Tables.Cats.TABLE_NAME;
                selection = appendWhere(selection, Tables.Cats.PRESET + " = 0");
                selection = appendWhere(selection,
                        Tables.Cats._ID + " = " + uri.getLastPathSegment());
                break;
            case EXTRAS:
                table = Tables.Extras.TABLE_NAME;
                selection = appendWhere(selection, Tables.Extras.PRESET + " = 0");
                break;
            case EXTRAS_ID:
                table = Tables.Extras.TABLE_NAME;
                selection = appendWhere(selection, Tables.Extras.PRESET + " = 0");
                selection = appendWhere(selection,
                        Tables.Extras._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS_EXTRAS:
                table = Tables.Extras.TABLE_NAME;
                selection = appendWhere(selection, Tables.Extras.PRESET + " = 0");
                selection = appendWhere(selection,
                        Tables.Extras.CAT + " = " + uri.getPathSegments().get(1));
                break;
            case ENTRIES_EXTRAS:
                table = Tables.EntriesExtras.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.EntriesExtras.ENTRY + " = " + uri.getPathSegments().get(1));
                break;
            case FLAVORS:
                table = Tables.Flavors.TABLE_NAME;
                break;
            case FLAVORS_ID:
                table = Tables.Flavors.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Flavors._ID + " = " + uri.getLastPathSegment());
                break;
            case CATS_FLAVOR:
                table = Tables.Flavors.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Flavors.CAT + " = " + uri.getPathSegments().get(1));
                break;
            case ENTRIES_FLAVOR:
                table = Tables.EntriesFlavors.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.EntriesFlavors.ENTRY + " = " + uri.getPathSegments().get(1));
                break;
            case PHOTOS:
                table = Tables.Photos.TABLE_NAME;
                break;
            case PHOTOS_ID:
                table = Tables.Photos.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Photos._ID + " = " + uri.getLastPathSegment());
                break;
            case ENTRIES_PHOTOS:
                table = Tables.Photos.TABLE_NAME;
                selection = appendWhere(selection,
                        Tables.Photos.ENTRY + " = " + uri.getPathSegments().get(1));
                break;
            case DELETED:
                table = Tables.Deleted.TABLE_NAME;
                break;
            case LOCATIONS:
            case LOCATIONS_ID:
                throw new IllegalArgumentException("Delete not permitted on: " + uri.toString());
            case ENTRIES_FILTER:
            case ENTRIES_CAT:
            case ENTRIES_CAT_FILTER:
            case MAKERS:
            case MAKERS_ID:
            case MAKERS_FILTER:
                throw new IllegalArgumentException("URI is read-only: " + uri.toString());
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri.toString());
        }

        synchronized(FlavordexProvider.class) {
            final int count =
                    mDbHelper.getWritableDatabase().delete(table, selection, selectionArgs);

            if(count > 0) {
                mBackupManager.dataChanged();

                if(Tables.Entries.TABLE_NAME.equals(table)) {
                    mResolver.notifyChange(Tables.Cats.CONTENT_URI, null);
                }

                mResolver.notifyChange(uri, null);
            }

            return count;
        }
    }

    /**
     * Find or create a maker based on data in values, replacing relevant values with the maker's
     * ID. Used while inserting or updating an entry.
     *
     * @param values ContentValues containing the name and/or origin of the maker
     */
    private void processMaker(@NonNull ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String maker = values.getAsString(Tables.Entries.MAKER);
        if(maker == null) {
            maker = "";
        }

        String origin = values.getAsString(Tables.Entries.ORIGIN);
        if(origin == null) {
            origin = "";
        }

        final Cursor cursor = db.query(Tables.Makers.TABLE_NAME, new String[] {Tables.Makers._ID},
                Tables.Makers.NAME + " = ? AND " + Tables.Makers.LOCATION + " = ?",
                new String[] {maker, origin}, null, null, null, "1");
        //noinspection TryFinallyCanBeTryWithResources
        try {
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                values.put(Tables.Entries.MAKER, cursor.getLong(0));
            } else {
                final ContentValues makerValues = new ContentValues();
                makerValues.put(Tables.Makers.NAME, maker);
                makerValues.put(Tables.Makers.LOCATION, origin);

                synchronized(FlavordexProvider.class) {
                    final long makerId = db.insert(Tables.Makers.TABLE_NAME, null, makerValues);
                    values.put(Tables.Entries.MAKER, makerId);
                }
            }
        } finally {
            cursor.close();
        }

        values.remove(Tables.Entries.ORIGIN);
        values.remove(Tables.Entries.MAKER_ID);
    }

    /**
     * Appends a fragment to a where clause.
     *
     * @param selection The original where clause
     * @param fragment  The fragment to add
     * @return The original where clause ANDed to the new fragment
     */
    @NonNull
    private static String appendWhere(@Nullable String selection, @NonNull String fragment) {
        if(TextUtils.isEmpty(selection)) {
            return fragment;
        } else {
            return fragment + " AND " + selection;
        }
    }
}
