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

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.sync.Sync;
import com.ultramegasoft.flavordex2.backend.sync.model.CatRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.EntryRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.ExtraRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.FlavorRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.PhotoRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.UpdateRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.UpdateResponse;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.io.IOException;
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
     * The client ID
     */
    private final long mClientId;

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
        mClientId = BackendUtils.getClientId(context);
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
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        if(accountName == null || mClientId == 0) {
            Log.i(TAG, "Client not registered. Aborting and disabling service.");
            prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_DATA, false).apply();
            return false;
        }
        final GoogleAccountCredential credential = BackendUtils.getCredential(mContext);
        credential.setSelectedAccountName(accountName);

        mSync = BackendUtils.getSync(credential);
        try {
            Log.d(TAG, "Syncing...");
            pushUpdates();
            fetchUpdates();
            Log.d(TAG, "Syncing complete.");

            sBackoffHelper.onSuccess();
            return true;
        } catch(IOException e) {
            Log.w(TAG, "Syncing with the backend failed", e);
        }

        sBackoffHelper.onFail();
        return false;
    }

    /**
     * Send updated journal data to the backend.
     *
     * @throws IOException
     */
    private void pushUpdates() throws IOException {
        final ContentResolver cr = mContext.getContentResolver();

        final UpdateRecord record = new UpdateRecord();
        record.setCats(getUpdatedCats());
        record.setEntries(getUpdatedEntries());

        final UpdateResponse response = mSync.pushUpdates(mClientId, record).execute();

        if(response.getCatStatuses() != null) {
            final String where = Tables.Cats.UUID + " = ?";
            final String[] whereArgs = new String[1];
            final ContentValues values = new ContentValues();
            values.put(Tables.Cats.PUBLISHED, true);
            values.put(Tables.Cats.SYNCED, true);
            for(Map.Entry<String, Object> status : response.getCatStatuses().entrySet()) {
                if((boolean)status.getValue()) {
                    whereArgs[0] = status.getKey();
                    cr.update(Tables.Cats.CONTENT_URI, values, where, whereArgs);
                }
            }
        }

        if(response.getEntryStatuses() != null) {
            final String where = Tables.Entries.UUID + " = ?";
            final String[] whereArgs = new String[1];
            final ContentValues values = new ContentValues();
            values.put(Tables.Entries.PUBLISHED, true);
            values.put(Tables.Entries.SYNCED, true);
            long remoteId;
            for(Map.Entry<String, Object> status : response.getEntryStatuses().entrySet()) {
                if((boolean)status.getValue()) {
                    whereArgs[0] = status.getKey();
                    remoteId = Long.valueOf(response.getEntryIds().get(status.getKey()).toString());
                    values.put(Tables.Entries.LINK, getLink(remoteId));
                    cr.update(Tables.Entries.CONTENT_URI, values, where, whereArgs);
                }
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
                    record.setDeleted(true);
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Deleted.UUID)));
                    record.setUpdated(cursor.getLong(cursor.getColumnIndex(Tables.Deleted.TIME)));
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
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Cats.UUID)));
                    record.setName(cursor.getString(cursor.getColumnIndex(Tables.Cats.NAME)));
                    record.setUpdated(cursor.getLong(cursor.getColumnIndex(Tables.Cats.UPDATED)));

                    id = cursor.getLong(cursor.getColumnIndex(Tables.Cats._ID));
                    record.setExtras(getCatExtras(id));
                    record.setFlavors(getCatFlavors(id));

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
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Extras.UUID)));
                    record.setName(cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME)));
                    record.setPos(cursor.getInt(cursor.getColumnIndex(Tables.Extras.POS)));
                    record.setDeleted(
                            cursor.getInt(cursor.getColumnIndex(Tables.Extras.DELETED)) == 1);
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
                    record.setName(cursor.getString(cursor.getColumnIndex(Tables.Flavors.NAME)));
                    record.setPos(cursor.getInt(cursor.getColumnIndex(Tables.Flavors.POS)));
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
                    record.setDeleted(true);
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Deleted.UUID)));
                    record.setUpdated(cursor.getLong(cursor.getColumnIndex(Tables.Deleted.TIME)));
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
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Entries.UUID)));
                    record.setCatUuid(
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT_UUID)));
                    record.setTitle(cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE)));
                    record.setMaker(cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER)));
                    record.setOrigin(
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.ORIGIN)));
                    record.setPrice(cursor.getString(cursor.getColumnIndex(Tables.Entries.PRICE)));
                    record.setLocation(
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.LOCATION)));
                    record.setDate(cursor.getLong(cursor.getColumnIndex(Tables.Entries.DATE)));
                    record.setRating(cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING)));
                    record.setNotes(cursor.getString(cursor.getColumnIndex(Tables.Entries.NOTES)));
                    record.setUpdated(
                            cursor.getLong(cursor.getColumnIndex(Tables.Entries.UPDATED)));
                    record.setShared(
                            cursor.getLong(cursor.getColumnIndex(Tables.Entries.SHARED)) == 1);

                    id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
                    record.setExtras(getEntryExtras(id));
                    record.setFlavors(getEntryFlavors(id));
                    record.setPhotos(getEntryPhotos(id));

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
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Extras.UUID)));
                    record.setValue(
                            cursor.getString(cursor.getColumnIndex(Tables.EntriesExtras.VALUE)));
                    record.setPos(cursor.getInt(cursor.getColumnIndex(Tables.Extras.POS)));
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
                    record.setName(
                            cursor.getString(cursor.getColumnIndex(Tables.EntriesFlavors.FLAVOR)));
                    record.setValue(
                            cursor.getInt(cursor.getColumnIndex(Tables.EntriesFlavors.VALUE)));
                    record.setPos(cursor.getInt(cursor.getColumnIndex(Tables.EntriesFlavors.POS)));
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
                    record.setHash(hash);
                    record.setDriveId(
                            cursor.getString(cursor.getColumnIndex(Tables.Photos.DRIVE_ID)));
                    record.setPos(cursor.getInt(cursor.getColumnIndex(Tables.Photos.POS)));
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
     *
     * @throws IOException
     */
    private void fetchUpdates() throws IOException {
        final UpdateRecord record = mSync.fetchUpdates(mClientId).execute();

        if(record.getCats() != null) {
            for(CatRecord catRecord : record.getCats()) {
                parseCat(catRecord);
            }
        }

        if(record.getEntries() != null) {
            for(EntryRecord entryRecord : record.getEntries()) {
                parseEntry(entryRecord);
            }
        }

        mSync.confirmSync(mClientId, record.getTimestamp()).execute();

        if(mRequestPhotoSync) {
            requestPhotoSync();
        }
    }

    /**
     * Parse a category record and save the category to the local database.
     *
     * @param record The category record
     */
    private void parseCat(CatRecord record) {
        final ContentResolver cr = mContext.getContentResolver();
        final long catId = getCatId(record.getUuid());
        Uri uri;
        final ContentValues values = new ContentValues();
        if(record.getDeleted()) {
            if(catId > 0) {
                uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, catId);
                values.put(Tables.Cats.PUBLISHED, false);
                cr.update(uri, values, null, null);
                cr.delete(uri, null, null);
            }
        } else {
            values.put(Tables.Cats.NAME, record.getName());
            values.put(Tables.Cats.PUBLISHED, true);
            values.put(Tables.Cats.SYNCED, true);
            if(catId > 0) {
                uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, catId);
                cr.update(uri, values, null, null);
            } else {
                uri = Tables.Cats.CONTENT_URI;
                values.put(Tables.Cats.UUID, record.getUuid());
                uri = cr.insert(uri, values);
                if(uri == null) {
                    return;
                }
            }

            parseCatExtras(uri, record);
            parseCatFlavors(uri, record);
        }
    }

    /**
     * Parse the extra fields from a category record and save them to the local database.
     *
     * @param catUri The category Uri
     * @param record The category record
     */
    private void parseCatExtras(Uri catUri, CatRecord record) {
        final ArrayList<ExtraRecord> extras = (ArrayList<ExtraRecord>)record.getExtras();
        if(extras == null) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        Uri uri = Uri.withAppendedPath(catUri, "extras");

        cr.delete(uri, null, null);

        final ContentValues values = new ContentValues();
        long id;
        for(ExtraRecord extra : extras) {
            values.put(Tables.Extras.UUID, extra.getUuid());
            values.put(Tables.Extras.NAME, extra.getName());
            values.put(Tables.Extras.POS, extra.getPos());
            values.put(Tables.Extras.DELETED, extra.getDeleted());

            id = getExtraId(extra.getUuid());
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
        final ArrayList<FlavorRecord> flavors = (ArrayList<FlavorRecord>)record.getFlavors();
        if(flavors == null) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(catUri, "flavor");

        cr.delete(uri, null, null);

        final ContentValues values = new ContentValues();
        for(FlavorRecord flavor : flavors) {
            values.put(Tables.Flavors.NAME, flavor.getName());
            values.put(Tables.Flavors.POS, flavor.getPos());
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
        final long entryId = getEntryId(record.getUuid());
        PhotoUtils.deleteThumb(mContext, entryId);

        Uri uri;
        final ContentValues values = new ContentValues();
        if(record.getDeleted()) {
            if(entryId > 0) {
                uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, entryId);
                values.put(Tables.Entries.PUBLISHED, false);
                cr.update(uri, values, null, null);
                cr.delete(uri, null, null);
            }
        } else {
            final long catId = getCatId(record.getCatUuid());
            if(catId == 0) {
                return;
            }
            values.put(Tables.Entries.TITLE, record.getTitle());
            values.put(Tables.Entries.MAKER, record.getMaker());
            values.put(Tables.Entries.ORIGIN, record.getOrigin());
            values.put(Tables.Entries.PRICE, record.getPrice());
            values.put(Tables.Entries.LOCATION, record.getLocation());
            values.put(Tables.Entries.DATE, record.getDate());
            values.put(Tables.Entries.RATING, record.getRating());
            values.put(Tables.Entries.NOTES, record.getNotes());
            values.put(Tables.Entries.PUBLISHED, true);
            values.put(Tables.Entries.SYNCED, true);
            values.put(Tables.Entries.SHARED, record.getShared());
            values.put(Tables.Entries.LINK, getLink(record.getId()));
            if(entryId > 0) {
                uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, entryId);
                cr.update(uri, values, null, null);
            } else {
                uri = Tables.Entries.CONTENT_URI;
                values.put(Tables.Entries.CAT, catId);
                values.put(Tables.Entries.UUID, record.getUuid());
                uri = cr.insert(uri, values);
                if(uri == null) {
                    return;
                }
            }

            parseEntryExtras(uri, record);
            parseEntryFlavors(uri, record);
            parseEntryPhotos(uri, record);
        }
    }

    /**
     * Parse the extra fields from an entry record and save them to the local database.
     *
     * @param entryUri The entry Uri
     * @param record   The entry record
     */
    private void parseEntryExtras(Uri entryUri, EntryRecord record) {
        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(entryUri, "extras");

        cr.delete(uri, null, null);

        final ArrayList<ExtraRecord> extras = (ArrayList<ExtraRecord>)record.getExtras();
        if(extras == null) {
            return;
        }

        long extraId;
        final ContentValues values = new ContentValues();
        for(ExtraRecord extra : extras) {
            extraId = getExtraId(extra.getUuid());
            if(extraId > 0) {
                values.put(Tables.EntriesExtras.EXTRA, extraId);
                values.put(Tables.EntriesExtras.VALUE, extra.getValue());
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
        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(entryUri, "flavor");

        final ArrayList<FlavorRecord> flavors = (ArrayList<FlavorRecord>)record.getFlavors();
        if(flavors == null) {
            return;
        }

        final ContentValues[] valuesArray = new ContentValues[flavors.size()];
        ContentValues values;
        FlavorRecord flavor;
        for(int i = 0; i < valuesArray.length; i++) {
            flavor = flavors.get(i);
            values = new ContentValues();
            values.put(Tables.EntriesFlavors.FLAVOR, flavor.getName());
            values.put(Tables.EntriesFlavors.VALUE, flavor.getValue());
            values.put(Tables.EntriesFlavors.POS, flavor.getPos());
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
        final ArrayList<PhotoRecord> photos = (ArrayList<PhotoRecord>)record.getPhotos();

        final ContentResolver cr = mContext.getContentResolver();
        final Uri uri = Uri.withAppendedPath(entryUri, "photos");

        if(photos == null) {
            cr.delete(uri, null, null);
            return;
        }

        final ArrayList<String> photoHashes = new ArrayList<>();
        final ContentValues values = new ContentValues();
        final String where = Tables.Photos.HASH + " = ?";
        final String[] whereArgs = new String[1];
        for(PhotoRecord photo : photos) {
            photoHashes.add(photo.getHash());
            whereArgs[0] = photo.getHash();
            values.put(Tables.Photos.DRIVE_ID, photo.getDriveId());
            values.put(Tables.Photos.POS, photo.getPos());
            if(cr.update(uri, values, where, whereArgs) == 0) {
                values.put(Tables.Photos.HASH, photo.getHash());
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

        if(!photos.isEmpty()) {
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
        link = link.substring(8) + link.substring(0, 8);
        link = Long.toString(Long.valueOf(link) + 1000000000, 34);
        link = link.replace('0', 'y').replace('1', 'z');
        return link;
    }
}
