package com.ultramegasoft.flavordex2.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.ApiException;
import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.backend.Sync;
import com.ultramegasoft.flavordex2.backend.model.CatRecord;
import com.ultramegasoft.flavordex2.backend.model.EntryRecord;
import com.ultramegasoft.flavordex2.backend.model.ExtraRecord;
import com.ultramegasoft.flavordex2.backend.model.FlavorRecord;
import com.ultramegasoft.flavordex2.backend.model.PhotoRecord;
import com.ultramegasoft.flavordex2.backend.model.SyncRecord;
import com.ultramegasoft.flavordex2.backend.model.UpdateResponse;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Helper for synchronizing journal data with the backend.
 *
 * @author Steve Guidetti
 */
public class DataSyncHelper {
    private static final String TAG = "DataSyncHelper";

    /**
     * Helper to implement exponential backoff
     */
    private static final BackendUtils.ExponentialBackoffHelper sBackoffHelper =
            new BackendUtils.ExponentialBackoffHelper(30, 30, 60 * 15);

    /**
     * The Context
     */
    private final Context mContext;

    /**
     * Helper for syncing photos
     */
    private final PhotoSyncHelper mPhotoSyncHelper;

    /**
     * Whether to request a photo sync
     */
    private boolean mRequestPhotoSync;

    /**
     * The Sync endpoint client
     */
    private Sync mSync;

    /**
     * @param context         The Context
     * @param photoSyncHelper Helper for syncing photos
     */
    public DataSyncHelper(Context context, PhotoSyncHelper photoSyncHelper) {
        mContext = context;
        mPhotoSyncHelper = photoSyncHelper;
    }

    /**
     * Sync data with the backend.
     *
     * @return Whether the sync completed successfully
     */
    public boolean sync() {
        if(!sBackoffHelper.shouldExecute()) {
            return false;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if(FirebaseAuth.getInstance().getCurrentUser() == null ||
                BackendUtils.getClientId(mContext) == 0) {
            Log.i(TAG, "Client not registered. Aborting and disabling service.");
            prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_DATA, false).apply();
            return false;
        }
        mSync = new Sync(mContext);
        try {
            Log.d(TAG, "Syncing...");
            mSync.startSync();
            pushUpdates();
            fetchUpdates();
            mSync.endSync();
            Log.d(TAG, "Syncing complete.");

            if(mRequestPhotoSync) {
                requestPhotoSync();
            }
            sBackoffHelper.onSuccess();
            return true;
        } catch(ApiException e) {
            Log.w(TAG, "Syncing with the backend failed", e);
        }

        sBackoffHelper.onFail();
        return false;
    }

    /**
     * Send updated journal data to the backend.
     */
    private void pushUpdates() throws ApiException {
        final ContentResolver cr = mContext.getContentResolver();

        String where = Tables.Cats.UUID + " = ?";
        final String[] whereArgs = new String[1];
        final ContentValues values = new ContentValues();
        values.put(Tables.Cats.PUBLISHED, true);
        values.put(Tables.Cats.SYNCED, true);
        for(CatRecord catRecord : getUpdatedCats()) {
            final UpdateResponse response = mSync.putCat(catRecord);
            if(response.success) {
                whereArgs[0] = catRecord.uuid;
                cr.update(Tables.Cats.CONTENT_URI, values, where, whereArgs);
            }
        }

        where = Tables.Entries.UUID + " = ?";
        values.clear();
        values.put(Tables.Entries.PUBLISHED, true);
        values.put(Tables.Entries.SYNCED, true);
        for(EntryRecord entryRecord : getUpdatedEntries()) {
            final UpdateResponse response = mSync.putEntry(entryRecord);
            if(response.success) {
                whereArgs[0] = entryRecord.uuid;
                values.put(Tables.Entries.LINK, getLink(response.remoteId));
                cr.update(Tables.Entries.CONTENT_URI, values, where, whereArgs);
            }
        }

        cr.delete(Tables.Deleted.CONTENT_URI, Tables.Deleted.TYPE + " < 2", null);
    }

    /**
     * Get all categories that have changed since the last sync with the backend.
     *
     * @return The list of updated categories
     */
    private ArrayList<CatRecord> getUpdatedCats() {
        final ContentResolver cr = mContext.getContentResolver();
        final ArrayList<CatRecord> records = new ArrayList<>();
        CatRecord record;

        String where = Tables.Deleted.TYPE + " = " + Tables.Deleted.TYPE_CAT;
        Cursor cursor = cr.query(Tables.Deleted.CONTENT_URI, null, where, null, null);
        if(cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    record = new CatRecord();
                    record.deleted = true;
                    record.uuid = cursor.getString(cursor.getColumnIndex(Tables.Deleted.UUID));
                    record.age =
                            subTime(cursor.getLong(cursor.getColumnIndex(Tables.Deleted.TIME)));
                    records.add(record);
                }
            } finally {
                cursor.close();
            }
        }

        where = Tables.Cats.SYNCED + " = 0";
        cursor = cr.query(Tables.Cats.CONTENT_URI, null, where, null, null);
        if(cursor != null) {
            try {
                long id;
                while(cursor.moveToNext()) {
                    record = new CatRecord();
                    record.uuid = cursor.getString(cursor.getColumnIndex(Tables.Cats.UUID));
                    record.name = cursor.getString(cursor.getColumnIndex(Tables.Cats.NAME));
                    record.age =
                            subTime(cursor.getLong(cursor.getColumnIndex(Tables.Cats.UPDATED)));

                    id = cursor.getLong(cursor.getColumnIndex(Tables.Cats._ID));
                    record.extras = getCatExtras(id);
                    record.flavors = getCatFlavors(id);

                    records.add(record);
                }
            } finally {
                cursor.close();
            }
        }

        return records;
    }

    /**
     * Get all the extra fields for a category from the local database.
     *
     * @param catId The local database ID of the category
     * @return A list of extra records
     */
    private ArrayList<ExtraRecord> getCatExtras(long catId) {
        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(Tables.Cats.CONTENT_ID_URI_BASE, catId + "/extras");
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                final ArrayList<ExtraRecord> records = new ArrayList<>();
                ExtraRecord record;
                while(cursor.moveToNext()) {
                    record = new ExtraRecord();
                    record.uuid = cursor.getString(cursor.getColumnIndex(Tables.Extras.UUID));
                    record.name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
                    record.pos = cursor.getInt(cursor.getColumnIndex(Tables.Extras.POS));
                    record.deleted =
                            cursor.getInt(cursor.getColumnIndex(Tables.Extras.DELETED)) == 1;
                    records.add(record);
                }

                return records;
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Get all the flavors for a category from the local database.
     *
     * @param catId The local database ID of the category
     * @return A list of flavor records
     */
    private ArrayList<FlavorRecord> getCatFlavors(long catId) {
        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(Tables.Cats.CONTENT_ID_URI_BASE, catId + "/flavor");
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                final ArrayList<FlavorRecord> records = new ArrayList<>();
                FlavorRecord record;
                while(cursor.moveToNext()) {
                    record = new FlavorRecord();
                    record.name = cursor.getString(cursor.getColumnIndex(Tables.Flavors.NAME));
                    record.pos = cursor.getInt(cursor.getColumnIndex(Tables.Flavors.POS));
                    records.add(record);
                }

                return records;
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Get all entries that have changed since the last sync with the backend.
     *
     * @return The list of updated entries
     */
    private ArrayList<EntryRecord> getUpdatedEntries() {
        final ContentResolver cr = mContext.getContentResolver();
        final ArrayList<EntryRecord> records = new ArrayList<>();
        EntryRecord record;

        String where = Tables.Deleted.TYPE + " = " + Tables.Deleted.TYPE_ENTRY;
        Cursor cursor = cr.query(Tables.Deleted.CONTENT_URI, null, where, null, null);
        if(cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    record = new EntryRecord();
                    record.deleted = true;
                    record.uuid = cursor.getString(cursor.getColumnIndex(Tables.Deleted.UUID));
                    record.age =
                            subTime(cursor.getLong(cursor.getColumnIndex(Tables.Deleted.TIME)));
                    records.add(record);
                }
            } finally {
                cursor.close();
            }
        }

        where = Tables.Entries.SYNCED + " = 0";
        cursor = cr.query(Tables.Entries.CONTENT_URI, null, where, null, null);
        if(cursor != null) {
            try {
                long id;
                while(cursor.moveToNext()) {
                    record = new EntryRecord();
                    record.uuid = cursor.getString(cursor.getColumnIndex(Tables.Entries.UUID));
                    record.catUuid =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT_UUID));
                    record.title = cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                    record.maker = cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER));
                    record.origin = cursor.getString(cursor.getColumnIndex(Tables.Entries.ORIGIN));
                    record.price = cursor.getString(cursor.getColumnIndex(Tables.Entries.PRICE));
                    record.location =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.LOCATION));
                    record.date = cursor.getLong(cursor.getColumnIndex(Tables.Entries.DATE));
                    record.rating = cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
                    record.notes = cursor.getString(cursor.getColumnIndex(Tables.Entries.NOTES));
                    record.age =
                            subTime(cursor.getLong(cursor.getColumnIndex(Tables.Entries.UPDATED)));
                    record.shared =
                            cursor.getLong(cursor.getColumnIndex(Tables.Entries.SHARED)) == 1;

                    id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
                    record.extras = getEntryExtras(id);
                    record.flavors = getEntryFlavors(id);
                    record.photos = getEntryPhotos(id);

                    records.add(record);
                }
            } finally {
                cursor.close();
            }
        }

        return records;
    }

    /**
     * Get all the extra fields for an entry from the local database.
     *
     * @param entryId The local database ID of the entry
     * @return A list of extra records
     */
    private ArrayList<ExtraRecord> getEntryExtras(long entryId) {
        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri =
                Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, entryId + "/extras");
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                final ArrayList<ExtraRecord> records = new ArrayList<>();
                ExtraRecord record;
                while(cursor.moveToNext()) {
                    record = new ExtraRecord();
                    record.uuid = cursor.getString(cursor.getColumnIndex(Tables.Extras.UUID));
                    record.value =
                            cursor.getString(cursor.getColumnIndex(Tables.EntriesExtras.VALUE));
                    record.pos = cursor.getInt(cursor.getColumnIndex(Tables.Extras.POS));
                    records.add(record);
                }

                return records;
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Get all the flavors for an entry from the local database.
     *
     * @param entryId The local database ID of the entry
     * @return A list of flavor records
     */
    private ArrayList<FlavorRecord> getEntryFlavors(long entryId) {
        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri =
                Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, entryId + "/flavor");
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                final ArrayList<FlavorRecord> records = new ArrayList<>();
                FlavorRecord record;
                while(cursor.moveToNext()) {
                    record = new FlavorRecord();
                    record.name =
                            cursor.getString(cursor.getColumnIndex(Tables.EntriesFlavors.FLAVOR));
                    record.value =
                            cursor.getInt(cursor.getColumnIndex(Tables.EntriesFlavors.VALUE));
                    record.pos = cursor.getInt(cursor.getColumnIndex(Tables.EntriesFlavors.POS));
                    records.add(record);
                }

                return records;
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Get all the photos for an entry from the local database.
     *
     * @param entryId The local database ID of the entry
     * @return A list of photo records
     */
    private ArrayList<PhotoRecord> getEntryPhotos(long entryId) {
        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri =
                Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, entryId + "/photos");
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                final ArrayList<PhotoRecord> records = new ArrayList<>();
                PhotoRecord record;
                String hash;
                long id;
                String path;
                while(cursor.moveToNext()) {
                    hash = cursor.getString(cursor.getColumnIndex(Tables.Photos.HASH));
                    if(hash == null) {
                        id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                        path = cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH));
                        if(path != null) {
                            hash = generatePhotoHash(id, path);
                        }
                        if(hash == null) {
                            continue;
                        }
                    }
                    record = new PhotoRecord();
                    record.hash = hash;
                    record.driveId =
                            cursor.getString(cursor.getColumnIndex(Tables.Photos.DRIVE_ID));
                    record.pos = cursor.getInt(cursor.getColumnIndex(Tables.Photos.POS));
                    records.add(record);
                }

                return records;
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Generate and save the MD5 hash of a photo.
     *
     * @param photoId  The database ID of the photo
     * @param filePath The path to the photo file
     * @return The generated MD5 hash
     */
    private String generatePhotoHash(long photoId, String filePath) {
        if(filePath == null) {
            return null;
        }

        final ContentResolver cr = mContext.getContentResolver();
        final String hash = PhotoUtils.getMD5Hash(cr, PhotoUtils.parsePath(filePath));
        if(hash != null) {
            final Uri uri = ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, photoId);
            final ContentValues values = new ContentValues();
            values.put(Tables.Photos.HASH, hash);
            cr.update(uri, values, null, null);
        }

        return hash;
    }

    /**
     * Fetch all the changed records from the backend.
     */
    private void fetchUpdates() throws ApiException {
        final ContentResolver cr = mContext.getContentResolver();
        final SyncRecord syncRecord = mSync.getUpdates();

        String[] whereArgs = new String[2];
        if(syncRecord.deletedCats != null) {
            final String where = Tables.Cats.UUID + " = ? AND " + Tables.Cats.UPDATED + " < ?";
            for(Map.Entry<String, Long> entry : syncRecord.deletedCats.entrySet()) {
                whereArgs[0] = entry.getKey();
                whereArgs[1] = subTime(entry.getValue()) + "";
                cr.delete(Tables.Cats.CONTENT_URI, where, whereArgs);
            }
        }

        if(syncRecord.deletedEntries != null) {
            final String where =
                    Tables.Entries.UUID + " = ? AND " + Tables.Entries.UPDATED + " < ?";
            for(Map.Entry<String, Long> entry : syncRecord.deletedEntries.entrySet()) {
                whereArgs[0] = entry.getKey();
                whereArgs[1] = subTime(entry.getValue()) + "";
                cr.delete(Tables.Entries.CONTENT_URI, where, whereArgs);
            }
        }

        whereArgs = new String[1];
        if(syncRecord.updatedCats != null) {
            final String[] projection = new String[] {Tables.Cats.UPDATED};
            final String where = Tables.Cats.UUID + " = ?";
            for(Map.Entry<String, Long> entry : syncRecord.updatedCats.entrySet()) {
                whereArgs[0] = entry.getKey();
                final Cursor cursor =
                        cr.query(Tables.Cats.CONTENT_URI, projection, where, whereArgs, null);
                if(cursor != null) {
                    try {
                        if(!cursor.moveToFirst() ||
                                subTime(entry.getValue()) > cursor.getLong(cursor.getColumnIndex(
                                        Tables.Cats.UPDATED))) {
                            parseCat(mSync.getCat(entry.getKey()));
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }

        if(syncRecord.updatedEntries != null) {
            final String[] projection = new String[] {Tables.Entries.UPDATED};
            final String where = Tables.Entries.UUID + " = ?";
            for(Map.Entry<String, Long> entry : syncRecord.updatedEntries.entrySet()) {
                whereArgs[0] = entry.getKey();
                final Cursor cursor =
                        cr.query(Tables.Entries.CONTENT_URI, projection, where, whereArgs, null);
                if(cursor != null) {
                    try {
                        if(!cursor.moveToFirst() ||
                                subTime(entry.getValue()) > cursor.getLong(cursor.getColumnIndex(
                                        Tables.Entries.UPDATED))) {
                            parseEntry(mSync.getEntry(entry.getKey()));
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
    }

    /**
     * Parse a category record and save the category to the local database.
     *
     * @param record The category record
     */
    private void parseCat(CatRecord record) {
        final ContentResolver cr = mContext.getContentResolver();
        final long catId = getCatId(record.uuid);
        Uri uri;
        final ContentValues values = new ContentValues();
        values.put(Tables.Cats.NAME, record.name);
        values.put(Tables.Cats.UPDATED, subTime(record.age));
        values.put(Tables.Cats.PUBLISHED, true);
        values.put(Tables.Cats.SYNCED, true);
        if(catId > 0) {
            uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, catId);
            cr.update(uri, values, null, null);
        } else {
            uri = Tables.Cats.CONTENT_URI;
            values.put(Tables.Cats.UUID, record.uuid);
            uri = cr.insert(uri, values);
            if(uri == null) {
                return;
            }
        }

        parseCatExtras(uri, record);
        parseCatFlavors(uri, record);
    }

    /**
     * Parse the extra fields from a category record and save them to the local database.
     *
     * @param catUri The category Uri
     * @param record The category record
     */
    private void parseCatExtras(Uri catUri, CatRecord record) {
        if(record.extras == null) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        Uri uri = Uri.withAppendedPath(catUri, "extras");

        cr.delete(uri, null, null);

        final ContentValues values = new ContentValues();
        long id;
        for(ExtraRecord extra : record.extras) {
            values.put(Tables.Extras.UUID, extra.uuid);
            values.put(Tables.Extras.NAME, extra.name);
            values.put(Tables.Extras.POS, extra.pos);
            values.put(Tables.Extras.DELETED, extra.deleted);

            id = getExtraId(extra.uuid);
            if(id > 0) {
                uri = ContentUris.withAppendedId(Tables.Extras.CONTENT_ID_URI_BASE, id);
                cr.update(uri, values, null, null);
            } else {
                uri = Uri.withAppendedPath(catUri, "extras");
                cr.insert(uri, values);
            }
        }
    }

    /**
     * Parse the flavors from a category record and save them to the local database.
     *
     * @param catUri The category Uri
     * @param record The category record
     */
    private void parseCatFlavors(Uri catUri, CatRecord record) {
        if(record.flavors == null) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(catUri, "flavor");

        cr.delete(uri, null, null);

        final ContentValues values = new ContentValues();
        for(FlavorRecord flavor : record.flavors) {
            values.put(Tables.Flavors.NAME, flavor.name);
            values.put(Tables.Flavors.POS, flavor.pos);
            cr.insert(uri, values);
        }
    }

    /**
     * Parse an entry record and save the entry to the local database.
     *
     * @param record The entry record
     */
    private void parseEntry(EntryRecord record) {
        final ContentResolver cr = mContext.getContentResolver();
        final long entryId = getEntryId(record.uuid);
        PhotoUtils.deleteThumb(mContext, entryId);

        Uri uri;
        final ContentValues values = new ContentValues();
        final long catId = getCatId(record.catUuid);
        if(catId == 0) {
            return;
        }
        values.put(Tables.Entries.TITLE, record.title);
        values.put(Tables.Entries.MAKER, record.maker);
        values.put(Tables.Entries.ORIGIN, record.origin);
        values.put(Tables.Entries.PRICE, record.price);
        values.put(Tables.Entries.LOCATION, record.location);
        values.put(Tables.Entries.DATE, record.date);
        values.put(Tables.Entries.RATING, record.rating);
        values.put(Tables.Entries.NOTES, record.notes);
        values.put(Tables.Entries.UPDATED, subTime(record.age));
        values.put(Tables.Entries.PUBLISHED, true);
        values.put(Tables.Entries.SYNCED, true);
        values.put(Tables.Entries.SHARED, record.shared);
        values.put(Tables.Entries.LINK, getLink(record.id));
        if(entryId > 0) {
            uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, entryId);
            cr.update(uri, values, null, null);
        } else {
            uri = Tables.Entries.CONTENT_URI;
            values.put(Tables.Entries.CAT, catId);
            values.put(Tables.Entries.UUID, record.uuid);
            uri = cr.insert(uri, values);
            if(uri == null) {
                return;
            }
        }

        parseEntryExtras(uri, record);
        parseEntryFlavors(uri, record);
        parseEntryPhotos(uri, record);
    }

    /**
     * Parse the extra fields from an entry record and save them to the local database.
     *
     * @param entryUri The entry Uri
     * @param record   The entry record
     */
    private void parseEntryExtras(Uri entryUri, EntryRecord record) {
        if(record.extras == null) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(entryUri, "extras");

        cr.delete(uri, null, null);

        long extraId;
        final ContentValues values = new ContentValues();
        for(ExtraRecord extra : record.extras) {
            extraId = getExtraId(extra.uuid);
            if(extraId > 0) {
                values.put(Tables.EntriesExtras.EXTRA, extraId);
                values.put(Tables.EntriesExtras.VALUE, extra.value);
                cr.insert(uri, values);
            }
        }
    }

    /**
     * Parse the flavors from an entry record and save them to the local database.
     *
     * @param entryUri The entry Uri
     * @param record   The entry record
     */
    private void parseEntryFlavors(Uri entryUri, EntryRecord record) {
        if(record.flavors == null) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(entryUri, "flavor");

        final ContentValues[] valuesArray = new ContentValues[record.flavors.size()];
        ContentValues values;
        FlavorRecord flavor;
        for(int i = 0; i < valuesArray.length; i++) {
            flavor = record.flavors.get(i);
            values = new ContentValues();
            values.put(Tables.EntriesFlavors.FLAVOR, flavor.name);
            values.put(Tables.EntriesFlavors.VALUE, flavor.value);
            values.put(Tables.EntriesFlavors.POS, flavor.pos);
            valuesArray[i] = values;
        }
        cr.bulkInsert(uri, valuesArray);
    }

    /**
     * Parse the photos from an entry record and save them to the local database.
     *
     * @param entryUri The entry Uri
     * @param record   The entry record
     */
    private void parseEntryPhotos(Uri entryUri, EntryRecord record) {
        if(record.photos == null) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(entryUri, "photos");

        final ArrayList<String> photoHashes = new ArrayList<>();
        final ContentValues values = new ContentValues();
        final String where = Tables.Photos.HASH + " = ?";
        final String[] whereArgs = new String[1];
        for(PhotoRecord photo : record.photos) {
            photoHashes.add(photo.hash);
            whereArgs[0] = photo.hash;
            values.put(Tables.Photos.DRIVE_ID, photo.driveId);
            values.put(Tables.Photos.POS, photo.pos);
            if(cr.update(uri, values, where, whereArgs) == 0) {
                values.put(Tables.Photos.HASH, photo.hash);
                cr.insert(uri, values);
            }
        }

        final String[] projection = new String[] {
                Tables.Photos._ID,
                Tables.Photos.HASH
        };
        final Cursor cursor =
                cr.query(uri, projection, Tables.Photos.HASH + " NOT NULL", null, null);
        if(cursor != null) {
            try {
                long id;
                String hash;
                while(cursor.moveToNext()) {
                    hash = cursor.getString(cursor.getColumnIndex(Tables.Photos.HASH));
                    if(!photoHashes.contains(hash)) {
                        id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                        cr.delete(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, id),
                                null, null);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        if(!record.photos.isEmpty()) {
            mRequestPhotoSync = true;
        }
    }

    /**
     * Request a photo sync.
     */
    private void requestPhotoSync() {
        mRequestPhotoSync = false;
        if(mPhotoSyncHelper != null && mPhotoSyncHelper.connect()) {
            mPhotoSyncHelper.fetchPhotos();
            return;
        }
        BackendUtils.requestPhotoSync(mContext);
    }

    /**
     * Get the local database ID of a category based on the UUID.
     *
     * @param uuid The UUID of the category
     * @return The local database ID of the category or 0 if not found
     */
    private long getCatId(String uuid) {
        if(uuid == null) {
            return 0;
        }

        final ContentResolver cr = mContext.getContentResolver();

        final String[] projection = new String[] {Tables.Cats._ID};
        final String where = Tables.Cats.UUID + " = ?";
        final String[] whereArgs = new String[] {uuid};
        final Cursor cursor = cr.query(Tables.Cats.CONTENT_URI, projection, where, whereArgs, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Get the local database ID of an entry based on the UUID.
     *
     * @param uuid The UUID of the entry
     * @return The local database ID of the entry or 0 if not found
     */
    private long getEntryId(String uuid) {
        if(uuid == null) {
            return 0;
        }

        final ContentResolver cr = mContext.getContentResolver();

        final String[] projection = new String[] {Tables.Entries._ID};
        final String where = Tables.Entries.UUID + " = ?";
        final String[] whereArgs = new String[] {uuid};
        final Cursor cursor =
                cr.query(Tables.Entries.CONTENT_URI, projection, where, whereArgs, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Get the local database ID of an extra field based on the UUID.
     *
     * @param uuid The UUID of the extra field
     * @return The local database ID of the extra field or 0 if not found
     */
    private long getExtraId(String uuid) {
        if(uuid == null) {
            return 0;
        }

        final ContentResolver cr = mContext.getContentResolver();

        final String[] projection = new String[] {Tables.Extras._ID};
        final String where = Tables.Extras.UUID + " = ?";
        final String[] whereArgs = new String[] {uuid};
        final Cursor cursor =
                cr.query(Tables.Extras.CONTENT_URI, projection, where, whereArgs, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Get the public link for an entry based on its remote ID.
     *
     * @param remoteId The remote ID
     * @return The link text version of the remote ID
     */
    private static String getLink(long remoteId) {
        String link = String.format(Locale.US, "%010d", remoteId);
        link = link.substring(link.length() - 2) + link.substring(0, link.length() - 2);
        link = Long.toString(Long.valueOf(link) + 1000000000, 34);
        link = link.replace('0', 'y').replace('1', 'z');
        return link;
    }

    /**
     * Subtract a value from the current time in milliseconds.
     *
     * @param milliseconds The value to subtract
     * @return The result value
     */
    private static long subTime(long milliseconds) {
        return System.currentTimeMillis() - milliseconds;
    }
}
