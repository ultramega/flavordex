package com.ultramegasoft.flavordex2.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.registration.Registration;
import com.ultramegasoft.flavordex2.backend.sync.Sync;
import com.ultramegasoft.flavordex2.backend.sync.model.CatRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.EntryRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.ExtraRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.FlavorRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.PhotoRecord;
import com.ultramegasoft.flavordex2.backend.sync.model.UpdateRecord;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Service for accessing the backend.
 *
 * @author Steve Guidetti
 */
public class BackendService extends IntentService {
    /**
     * Action value for broadcast Intents
     */
    public static final String ACTION_COMPLETED = "com.ultramegasoft.flavordex2.service.COMPLETE";

    /**
     * Keys for broadcast Intent extras
     */
    public static final String EXTRA_ERROR = "error";

    /**
     * Keys for the Intent extras
     */
    private static final String EXTRA_COMMAND = "command";
    private static final String EXTRA_ACCOUNT_NAME = "account_name";

    /**
     * Commands this service will accept
     */
    private static final int COMMAND_REGISTER = 0;
    private static final int COMMAND_UNREGISTER = 1;
    private static final int COMMAND_SYNC = 2;

    /**
     * The API project number
     */
    private static final String PROJECT_NUMBER = "1001621163874";

    /**
     * The error message in case of failure
     */
    private String mError;

    /**
     * Whether to notify all clients that data has changed
     */
    private boolean mNotifyClients;

    public BackendService() {
        super("BackendService");
    }

    /**
     * Register the client with the backend.
     *
     * @param context     The Context
     * @param accountName The name of the account to use
     */
    public static void registerClient(Context context, String accountName) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_REGISTER);
        intent.putExtra(EXTRA_ACCOUNT_NAME, accountName);
        context.startService(intent);
    }

    /**
     * Unregister the client from the backend.
     *
     * @param context The Context
     */
    public static void unregisterClient(Context context) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_UNREGISTER);
        context.startService(intent);
    }

    /**
     * Sync the journal data with the backend.
     *
     * @param context The Context
     */
    public static void syncData(Context context) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_SYNC);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch(intent.getIntExtra(EXTRA_COMMAND, -1)) {
            case COMMAND_REGISTER:
                doRegisterClient(intent.getStringExtra(EXTRA_ACCOUNT_NAME));
                break;
            case COMMAND_UNREGISTER:
                doUnregisterClient();
                break;
            case COMMAND_SYNC:
                doSyncData();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final Intent intent = new Intent(ACTION_COMPLETED);
        if(mError != null) {
            intent.putExtra(EXTRA_ERROR, mError);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Handle client registration.
     *
     * @param accountName The name of the account to use
     */
    private void doRegisterClient(String accountName) {
        if(accountName == null) {
            return;
        }

        final GoogleAccountCredential credential = BackendUtils.getCredential(this);
        credential.setSelectedAccountName(accountName);

        final InstanceID instanceID = InstanceID.getInstance(this);

        try {
            final String token =
                    instanceID.getToken(PROJECT_NUMBER, GoogleCloudMessaging.INSTANCE_ID_SCOPE);

            final Registration registration = BackendUtils.getRegistration(credential);
            final long clientId = registration.register(token).execute().getClientId();
            if(clientId > 0) {
                BackendUtils.setClientId(this, clientId);
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString(FlavordexApp.PREF_ACCOUNT_NAME, accountName)
                        .putBoolean(FlavordexApp.PREF_SYNC_DATA, true).apply();
            }
        } catch(IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
            mError = e.getMessage();
        }
    }

    /**
     * Handle client unregistration.
     */
    private void doUnregisterClient() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        if(accountName == null) {
            return;
        }

        final GoogleAccountCredential credential = BackendUtils.getCredential(this);
        credential.setSelectedAccountName(accountName);

        final Registration registration = BackendUtils.getRegistration(credential);
        try {
            InstanceID.getInstance(this)
                    .deleteToken(PROJECT_NUMBER, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            registration.unregister(BackendUtils.getClientId(this)).execute();
        } catch(IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
            mError = e.getMessage();
        }

        BackendUtils.setClientId(this, 0);
        prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_DATA, false).apply();
    }

    /**
     * Handle journal data synchronization.
     */
    private void doSyncData() {
        mNotifyClients = false;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        final long clientId = BackendUtils.getClientId(this);
        if(accountName == null || clientId == 0) {
            prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_DATA, false).apply();
            return;
        }
        final GoogleAccountCredential credential = BackendUtils.getCredential(this);
        credential.setSelectedAccountName(accountName);

        final Sync sync = BackendUtils.getSync(credential);
        try {
            pushCats(sync, clientId);
            pushEntries(sync, clientId);

            if(mNotifyClients) {
                sync.notifyClients(clientId).execute();
            }

            BackendUtils.setLastSync(this);

            fetchUpdates(sync, clientId);
        } catch(IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Push all categories that have changed since the last sync to the backend.
     *
     * @param sync     The Sync endpoint client
     * @param clientId The client ID
     * @throws IOException
     */
    private void pushCats(Sync sync, long clientId) throws IOException {
        final ContentResolver cr = getContentResolver();
        final CatRecord record = new CatRecord();

        String where = Tables.Deleted.TYPE + " = " + Tables.Deleted.TYPE_CAT;
        Cursor cursor = cr.query(Tables.Deleted.CONTENT_URI, null, where, null, null);
        record.setDeleted(true);
        if(cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    mNotifyClients = true;
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Deleted.UUID)));
                    if(sync.pushCategory(clientId, record).execute().getSuccess()) {
                        where = Tables.Deleted._ID + " = "
                                + cursor.getLong(cursor.getColumnIndex(Tables.Deleted._ID));
                        cr.delete(Tables.Deleted.CONTENT_URI, where, null);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        where = Tables.Cats.UPDATED + " > " + BackendUtils.getLastSync(this);
        cursor = cr.query(Tables.Cats.CONTENT_URI, null, where, null, null);
        record.setDeleted(false);
        if(cursor != null) {
            try {
                long id;
                while(cursor.moveToNext()) {
                    mNotifyClients = true;
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Cats.UUID)));
                    record.setName(cursor.getString(cursor.getColumnIndex(Tables.Cats.NAME)));

                    id = cursor.getLong(cursor.getColumnIndex(Tables.Cats._ID));
                    record.setExtras(getCatExtras(cr, id));
                    record.setFlavors(getCatFlavors(cr, id));

                    sync.pushCategory(clientId, record).execute();
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Get all the extra fields for a category from the local database.
     *
     * @param cr    The ContentResolver
     * @param catId The local database ID of the category
     * @return A list of extra records
     */
    private static ArrayList<ExtraRecord> getCatExtras(ContentResolver cr, long catId) {
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
     * @param cr    The ContentResolver
     * @param catId The local database ID of the category
     * @return A list of flavor records
     */
    private static ArrayList<FlavorRecord> getCatFlavors(ContentResolver cr, long catId) {
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
     * Push all entries that have changed since the last sync to the backend.
     *
     * @param sync     The Sync endpoint client
     * @param clientId The client ID
     * @throws IOException
     */
    private void pushEntries(Sync sync, long clientId) throws IOException {
        final ContentResolver cr = getContentResolver();
        final EntryRecord record = new EntryRecord();

        String where = Tables.Deleted.TYPE + " = " + Tables.Deleted.TYPE_ENTRY;
        Cursor cursor = cr.query(Tables.Deleted.CONTENT_URI, null, where, null, null);
        record.setDeleted(true);
        if(cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    mNotifyClients = true;
                    record.setUuid(cursor.getString(cursor.getColumnIndex(Tables.Deleted.UUID)));
                    if(sync.pushEntry(clientId, record).execute().getSuccess()) {
                        where = Tables.Deleted._ID + " = "
                                + cursor.getLong(cursor.getColumnIndex(Tables.Deleted._ID));
                        cr.delete(Tables.Deleted.CONTENT_URI, where, null);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        where = Tables.Entries.UPDATED + " > " + BackendUtils.getLastSync(this);
        cursor = cr.query(Tables.Entries.CONTENT_URI, null, where, null, null);
        record.setDeleted(false);
        if(cursor != null) {
            try {
                long id;
                while(cursor.moveToNext()) {
                    mNotifyClients = true;
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

                    id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
                    record.setExtras(getEntryExtras(cr, id));
                    record.setFlavors(getEntryFlavors(cr, id));
                    record.setPhotos(getEntryPhotos(cr, id));

                    sync.pushEntry(clientId, record).execute();
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Get all the extra fields for an entry from the local database.
     *
     * @param cr      The ContentResolver
     * @param entryId The local database ID of the entry
     * @return A list of extra records
     */
    private static ArrayList<ExtraRecord> getEntryExtras(ContentResolver cr, long entryId) {
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
     * @param cr      The ContentResolver
     * @param entryId The local database ID of the entry
     * @return A list of flavor records
     */
    private static ArrayList<FlavorRecord> getEntryFlavors(ContentResolver cr, long entryId) {
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
     * @param cr      The ContentResolver
     * @param entryId The local database ID of the entry
     * @return A list of photo records
     */
    private static ArrayList<PhotoRecord> getEntryPhotos(ContentResolver cr, long entryId) {
        final Uri uri =
                Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, entryId + "/photos");
        final Cursor cursor = cr.query(uri, null, null, null, null);
        if(cursor != null) {
            try {
                final ArrayList<PhotoRecord> records = new ArrayList<>();
                PhotoRecord record;
                while(cursor.moveToNext()) {
                    record = new PhotoRecord();
                    record.setPath(cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH)));
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
     * Fetch all the changed records from the backend.
     *
     * @param sync     The Sync endpoint client
     * @param clientId The client ID
     * @throws IOException
     */
    private void fetchUpdates(Sync sync, long clientId) throws IOException {
        final ContentResolver cr = getContentResolver();
        final UpdateRecord record = sync.fetchUpdates(clientId).execute();

        if(record.getCats() != null) {
            for(CatRecord catRecord : record.getCats()) {
                parseCat(cr, catRecord);
            }
        }

        if(record.getEntries() != null) {
            for(EntryRecord entryRecord : record.getEntries()) {
                parseEntry(cr, entryRecord);
            }
        }

        sync.confirmFetch(clientId, record.getTimestamp()).execute();
    }

    /**
     * Parse a category record and save the category to the local database.
     *
     * @param cr     The ContentResolver
     * @param record The category record
     */
    private static void parseCat(ContentResolver cr, CatRecord record) {
        final long catId = getCatId(cr, record.getUuid());
        Uri uri;
        if(record.getDeleted()) {
            if(catId > 0) {
                uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, catId);
                cr.delete(uri, null, null);
            }
        } else {
            final ContentValues values = new ContentValues();
            values.put(Tables.Cats.NAME, record.getName());
            values.put(Tables.Cats.UPDATED, System.currentTimeMillis());
            if(catId > 0) {
                uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, catId);
                cr.update(uri, values, null, null);
            } else {
                uri = Tables.Cats.CONTENT_URI;
                values.put(Tables.Cats.UUID, record.getUuid());
                uri = cr.insert(uri, values);
            }

            parseCatExtras(cr, uri, record);
            parseCatFlavors(cr, uri, record);
        }
    }

    /**
     * Parse the extra fields from a category record and save them to the local database.
     *
     * @param cr     The ContentResolver
     * @param catUri The category Uri
     * @param record The category record
     */
    private static void parseCatExtras(ContentResolver cr, Uri catUri, CatRecord record) {
        final ArrayList<ExtraRecord> extras = (ArrayList<ExtraRecord>)record.getExtras();
        if(extras == null) {
            return;
        }

        Uri uri = Uri.withAppendedPath(catUri, "extras");
        cr.delete(uri, null, null);

        final ContentValues values = new ContentValues();
        long id;
        for(ExtraRecord extra : extras) {
            values.put(Tables.Extras.UUID, extra.getUuid());
            values.put(Tables.Extras.NAME, extra.getName());
            values.put(Tables.Extras.POS, extra.getPos());
            values.put(Tables.Extras.DELETED, extra.getDeleted());

            id = getExtraId(cr, extra.getUuid());
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
     * @param cr     The ContentResolver
     * @param catUri The category Uri
     * @param record The category record
     */
    private static void parseCatFlavors(ContentResolver cr, Uri catUri, CatRecord record) {
        final ArrayList<FlavorRecord> flavors = (ArrayList<FlavorRecord>)record.getFlavors();
        if(flavors == null) {
            return;
        }

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
     * @param cr     The ContentResolver
     * @param record The entry record
     */
    private static void parseEntry(ContentResolver cr, EntryRecord record) {
        final long entryId = getEntryId(cr, record.getUuid());
        Uri uri;
        if(record.getDeleted()) {
            if(entryId > 0) {
                uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, entryId);
                cr.delete(uri, null, null);
            }
        } else {
            final long catId = getCatId(cr, record.getCatUuid());
            if(catId == 0) {
                return;
            }
            final ContentValues values = new ContentValues();
            values.put(Tables.Entries.TITLE, record.getTitle());
            values.put(Tables.Entries.MAKER, record.getMaker());
            values.put(Tables.Entries.ORIGIN, record.getOrigin());
            values.put(Tables.Entries.PRICE, record.getPrice());
            values.put(Tables.Entries.LOCATION, record.getLocation());
            values.put(Tables.Entries.DATE, record.getDate());
            values.put(Tables.Entries.RATING, record.getRating());
            values.put(Tables.Entries.NOTES, record.getNotes());
            values.put(Tables.Entries.UPDATED, System.currentTimeMillis());
            if(entryId > 0) {
                uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, entryId);
                cr.update(uri, values, null, null);
            } else {
                uri = Tables.Entries.CONTENT_URI;
                values.put(Tables.Entries.CAT, catId);
                values.put(Tables.Entries.UUID, record.getUuid());
                uri = cr.insert(uri, values);
            }

            parseEntryExtras(cr, uri, record);
            parseEntryFlavors(cr, uri, record);
            parseEntryPhotos(cr, uri, record);
        }
    }

    /**
     * Parse the extra fields from an entry record and save them to the local database.
     *
     * @param cr       The ContentResolver
     * @param entryUri The entry Uri
     * @param record   The entry record
     */
    private static void parseEntryExtras(ContentResolver cr, Uri entryUri, EntryRecord record) {
        final ArrayList<ExtraRecord> extras = (ArrayList<ExtraRecord>)record.getExtras();
        if(extras == null) {
            return;
        }

        final Uri uri = Uri.withAppendedPath(entryUri, "extras");
        cr.delete(uri, null, null);

        long extraId;
        final ContentValues values = new ContentValues();
        for(ExtraRecord extra : extras) {
            extraId = getExtraId(cr, extra.getUuid());
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
     * @param cr       The ContentResolver
     * @param entryUri The entry Uri
     * @param record   The entry record
     */
    private static void parseEntryFlavors(ContentResolver cr, Uri entryUri, EntryRecord record) {
        final ArrayList<FlavorRecord> flavors = (ArrayList<FlavorRecord>)record.getFlavors();
        if(flavors == null) {
            return;
        }

        final Uri uri = Uri.withAppendedPath(entryUri, "flavor");
        cr.delete(uri, null, null);

        final ContentValues values = new ContentValues();
        for(FlavorRecord flavor : flavors) {
            values.put(Tables.EntriesFlavors.FLAVOR, flavor.getName());
            values.put(Tables.EntriesFlavors.VALUE, flavor.getValue());
            values.put(Tables.EntriesFlavors.POS, flavor.getPos());
            cr.insert(uri, values);
        }
    }

    /**
     * Parse the photos from an entry record and save them to the local database.
     *
     * @param cr       The ContentResolver
     * @param entryUri The entry Uri
     * @param record   The entry record
     */
    private static void parseEntryPhotos(ContentResolver cr, Uri entryUri, EntryRecord record) {
        final ArrayList<PhotoRecord> photos = (ArrayList<PhotoRecord>)record.getPhotos();
        if(photos == null) {
            return;
        }

        final Uri uri = Uri.withAppendedPath(entryUri, "photos");
        cr.delete(uri, null, null);

        final ContentValues values = new ContentValues();
        for(PhotoRecord photo : photos) {
            values.put(Tables.Photos.PATH, photo.getPath());
            values.put(Tables.Photos.DRIVE_ID, photo.getDriveId());
            values.put(Tables.Photos.POS, photo.getPos());
            cr.insert(uri, values);
        }
    }

    /**
     * Get the local database ID of a category based on the UUID.
     *
     * @param cr   The ContentResolver
     * @param uuid The UUID of the category
     * @return The local database ID of the category or 0 if not found
     */
    private static long getCatId(ContentResolver cr, String uuid) {
        if(uuid == null) {
            return 0;
        }
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
     * @param cr   The ContentResolver
     * @param uuid The UUID of the entry
     * @return The local database ID of the entry or 0 if not found
     */
    private static long getEntryId(ContentResolver cr, String uuid) {
        if(uuid == null) {
            return 0;
        }
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
     * @param cr   The ContentResolver
     * @param uuid The UUID of the extra field
     * @return The local database ID of the extra field or 0 if not found
     */
    private static long getExtraId(ContentResolver cr, String uuid) {
        if(uuid == null) {
            return 0;
        }
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
}
