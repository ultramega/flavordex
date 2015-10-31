package com.ultramegasoft.flavordex2.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;
import com.ultramegasoft.flavordex2.widget.RadarHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Utilities for managing journal entries.
 *
 * @author Steve Guidetti
 */
public class EntryUtils {
    private static final String TAG = "EntryUtils";

    /**
     * Insert a new journal entry.
     *
     * @param context The Context
     * @param entry   The entry
     * @return The Uri for the new entry
     * @throws SQLiteException
     */
    public static Uri insertEntry(Context context, EntryHolder entry) throws SQLiteException {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues values = new ContentValues();
        if(entry.uuid != null) {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(entry.uuid);
                values.put(Tables.Entries.UUID, entry.uuid);
            } catch(IllegalArgumentException ignored) {
            }
        }
        values.put(Tables.Entries.TITLE, entry.title);
        values.put(Tables.Entries.MAKER, entry.maker);
        values.put(Tables.Entries.ORIGIN, entry.origin);
        values.put(Tables.Entries.PRICE, entry.price);
        values.put(Tables.Entries.LOCATION, entry.location);
        values.put(Tables.Entries.DATE, entry.date);
        values.put(Tables.Entries.RATING, entry.rating);
        values.put(Tables.Entries.NOTES, entry.notes);
        values.put(Tables.Entries.UPDATED, System.currentTimeMillis());

        final Uri catUri = getCatUri(cr, entry);
        values.put(Tables.Entries.CAT, entry.catId);

        final Uri entryUri = cr.insert(Tables.Entries.CONTENT_URI, values);
        insertExtras(cr, catUri, entryUri, entry);
        insertFlavors(cr, entryUri, entry);
        insertPhotos(cr, entryUri, entry);

        BackendUtils.requestSync(context);

        return entryUri;
    }

    /**
     * Find the ID for a category, creating one if it doesn't exist.
     *
     * @param cr    The ContentResolver
     * @param entry The entry
     * @return The Uri for the category
     */
    private static Uri getCatUri(ContentResolver cr, EntryHolder entry) {
        final Uri uri = Tables.Cats.CONTENT_URI;
        final String[] projection = new String[] {Tables.Cats._ID};
        final String where = Tables.Cats.NAME + " = ?";
        final String[] whereArgs = new String[] {entry.catName};
        final Cursor cursor = cr.query(uri, projection, where, whereArgs, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    final long id = cursor.getLong(cursor.getColumnIndex(Tables.Cats._ID));
                    entry.catId = id;
                    return ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, id);
                }
            } finally {
                cursor.close();
            }
        }

        final ContentValues values = new ContentValues();
        values.put(Tables.Cats.NAME, filterName(entry.catName));
        final Uri catUri = cr.insert(uri, values);

        if(catUri == null) {
            throw new SQLiteException("Inserting new category failed");
        }

        entry.catId = Long.valueOf(catUri.getLastPathSegment());
        insertCatFlavors(cr, catUri, entry);

        return catUri;
    }

    /**
     * Insert the flavor list for the new category.
     *
     * @param cr     The ContentResolver
     * @param catUri The Uri for the category
     * @param entry  The entry
     */
    private static void insertCatFlavors(ContentResolver cr, Uri catUri, EntryHolder entry) {
        final ArrayList<RadarHolder> flavors = entry.getFlavors();
        final Uri uri = Uri.withAppendedPath(catUri, "flavor");
        final ContentValues values = new ContentValues();
        values.put(Tables.Flavors.CAT, entry.catId);
        for(int i = 0; i < flavors.size(); i++) {
            values.put(Tables.Flavors.NAME, filterName(flavors.get(i).name));
            values.put(Tables.Flavors.POS, i);
            cr.insert(uri, values);
        }
    }

    /**
     * Insert the extra fields for the new entry.
     *
     * @param cr       The ContentResolver
     * @param catUri   The Uri for the category
     * @param entryUri The Uri for the new entry
     * @param entry    The entry
     */
    private static void insertExtras(ContentResolver cr, Uri catUri, Uri entryUri,
                                     EntryHolder entry) {
        final Uri uri = Uri.withAppendedPath(entryUri, "extras");
        final ContentValues values = new ContentValues();
        for(ExtraFieldHolder extra : entry.getExtras()) {
            try {
                values.put(Tables.EntriesExtras.EXTRA, getExtraId(cr, catUri, extra.name));
                values.put(Tables.EntriesExtras.VALUE, extra.value);
                cr.insert(uri, values);
            } catch(SQLiteException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * Find the ID of an extra field, creating one if it doesn't exist.
     *
     * @param cr     The ContentResolver
     * @param catUri The Uri for the category
     * @param name   The name of the field
     * @return The ID for the extra field
     */
    private static long getExtraId(ContentResolver cr, Uri catUri, String name) {
        final Uri uri = Uri.withAppendedPath(catUri, "extras");
        final String[] projection = new String[] {Tables.Extras._ID};
        final String where = Tables.Extras.NAME + " = ?";
        final String[] whereArgs = new String[] {name};
        final Cursor cursor = cr.query(uri, projection, where, whereArgs, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndex(Tables.Extras._ID));
                }
            } finally {
                cursor.close();
            }
        }

        final ContentValues values = new ContentValues();
        values.put(Tables.Extras.NAME, name);
        values.put(Tables.Extras.POS, getNextExtraPos(cr, uri));
        final Uri extraUri = cr.insert(uri, values);
        if(extraUri == null) {
            throw new SQLiteException("Inserting new extra field failed");
        }

        return Long.valueOf(extraUri.getLastPathSegment());
    }

    /**
     * Get the next sort position for a new extra field.
     *
     * @param cr  The ContentResolver
     * @param uri The Uri for a category's extras
     * @return The next sort position for a new extra field
     */
    private static int getNextExtraPos(ContentResolver cr, Uri uri) {
        final String[] projection = new String[] {Tables.Extras.POS};
        final Cursor cursor = cr.query(uri, projection, null, null, Tables.Extras.POS + " DESC");
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    return cursor.getInt(0) + 1;
                }
            } finally {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Insert the flavors for the new entry.
     *
     * @param cr       The ContentResolver
     * @param entryUri The Uri for the new entry
     * @param entry    The entry
     */
    private static void insertFlavors(ContentResolver cr, Uri entryUri, EntryHolder entry) {
        final ArrayList<RadarHolder> flavors = entry.getFlavors();
        final Uri uri = Uri.withAppendedPath(entryUri, "flavor");
        final ContentValues values = new ContentValues();
        RadarHolder flavor;
        for(int i = 0; i < flavors.size(); i++) {
            flavor = flavors.get(i);
            values.put(Tables.EntriesFlavors.FLAVOR, filterName(flavor.name));
            values.put(Tables.EntriesFlavors.VALUE, flavor.value);
            values.put(Tables.EntriesFlavors.POS, i);
            cr.insert(uri, values);
        }
    }

    /**
     * Insert the photos for the new entry.
     *
     * @param cr       The ContentResolver
     * @param entryUri The Uri for the new entry
     * @param entry    The entry
     */
    private static void insertPhotos(ContentResolver cr, Uri entryUri, EntryHolder entry) {
        final ArrayList<PhotoHolder> photos = entry.getPhotos();
        final Uri uri = Uri.withAppendedPath(entryUri, "photos");
        final ContentValues values = new ContentValues();
        PhotoHolder photo;
        for(int i = 0; i < photos.size(); i++) {
            photo = photos.get(i);
            values.put(Tables.Photos.HASH, PhotoUtils.getMD5Hash(new File(photo.path)));
            values.put(Tables.Photos.PATH, photo.path);
            values.put(Tables.Photos.POS, i);
            cr.insert(uri, values);
        }
    }

    /**
     * Filter for category and field names.
     *
     * @param name The original text
     * @return The filtered text
     */
    private static String filterName(String name) {
        for(int i = 0; i < name.length(); i++) {
            if(name.charAt(i) != '_') {
                return name.substring(i);
            }
        }
        return name;
    }

    /**
     * Delete an entry.
     *
     * @param context The Context
     * @param id      The entry's database ID
     */
    public static void delete(Context context, long id) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, id);
        cr.delete(uri, null, null);
        PhotoUtils.deleteThumb(context, id);
        BackendUtils.requestSync(context);
    }

    /**
     * Remove a photo from an entry.
     *
     * @param context The Context
     * @param photoId The photo's database ID
     */
    public static void deletePhoto(Context context, long photoId) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, photoId);
        final String[] projection = new String[] {
                Tables.Photos.ENTRY
        };
        final Cursor cursor = cr.query(uri, projection, null, null, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    cr.delete(uri, null, null);

                    final long entryId = cursor.getLong(0);
                    PhotoUtils.generateThumb(context, entryId);
                    cr.notifyChange(ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE,
                            entryId), null);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Set the changed time of an entry.
     *
     * @param cr      The ContentResolver
     * @param entryId The entry's database ID
     */
    public static void markChanged(ContentResolver cr, long entryId) {
        final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, entryId);
        final ContentValues values = new ContentValues();
        values.put(Tables.Entries.UPDATED, System.currentTimeMillis());
        cr.update(uri, values, null, null);
    }

    /**
     * Sent a sharing Intent.
     *
     * @param context The Context
     * @param title   The message title
     * @param rating  The rating to show
     */
    public static void share(Context context, String title, float rating) {
        final Intent intent = getShareIntent(context, title, rating);
        if(intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(Intent.createChooser(intent,
                    context.getString(R.string.menu_share_entry)));
        } else {
            Toast.makeText(context, R.string.error_no_app, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Get a sharing Intent.
     *
     * @param context The Context
     * @param title   The message title
     * @param rating  The rating to show
     * @return A send action Intent
     */
    public static Intent getShareIntent(Context context, String title, float rating) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject(context, title));
        intent.putExtra(Intent.EXTRA_TEXT, getShareBody(context, title, rating));

        return intent;
    }

    /**
     * Get the message body for the share Intent.
     *
     * @param context The Context
     * @param title   The message title
     * @param rating  The rating to show
     * @return The message body
     */
    private static String getShareBody(Context context, String title, float rating) {
        final String app = context.getString(R.string.app_name);
        return context.getString(R.string.share_body, title, app, rating);
    }

    /**
     * Get the message subject for the share Intent.
     *
     * @param context The Context
     * @param title   The message title
     * @return The message subject
     */
    private static String getShareSubject(Context context, String title) {
        return context.getString(R.string.share_subject, title);
    }
}
