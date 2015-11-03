package com.ultramegasoft.flavordex2.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.net.ConnectivityManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper for synchronizing photos with Google Drive.
 *
 * @author Steve Guidetti
 */
public class PhotoSyncHelper {
    private static final String TAG = "PhotoSyncHelper";

    /**
     * The name of the Drive folder to store photos
     */
    private static final String DRIVE_FOLDER = "Flavordex Photos";

    /**
     * The custom property key to identify photos
     */
    private static final CustomPropertyKey sHashKey =
            new CustomPropertyKey("hash", CustomPropertyKey.PUBLIC);

    /**
     * The Google API Client
     */
    private GoogleApiClient mClient;

    /**
     * The Drive folder for the application
     */
    private DriveFolder mDriveFolder;

    /**
     * The Context
     */
    private final Context mContext;

    /**
     * @param context The Context
     */
    public PhotoSyncHelper(Context context) {
        mContext = context;
    }

    /**
     * Connect to Google Drive.
     *
     * @return Whether the client connected successfully
     */
    public boolean connect() {
        Log.i(TAG, "Connecting to Google Drive...");
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.i(TAG, "External storage not mounted. Aborting.");
            return false;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if(!Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .canWrite()) {
            Log.i(TAG, "External storage directory not writable. Aborting.");
            if(!PermissionUtils.hasExternalStoragePerm(mContext)) {
                Log.i(TAG, "External storage access permission denied. Disabling service.");
                prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false).apply();
            }
            return false;
        }

        if(prefs.getBoolean(FlavordexApp.PREF_SYNC_PHOTOS_UNMETERED, true)) {
            final ConnectivityManager cm =
                    (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(ConnectivityManagerCompat.isActiveNetworkMetered(cm)) {
                Log.i(TAG, "Network is metered. Aborting.");
                return false;
            }
        }

        mClient = new GoogleApiClient.Builder(mContext)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();
        final ConnectionResult result = mClient.blockingConnect();
        if(result.isSuccess()) {
            final DriveFolder driveFolder = openDriveFolder();
            if(driveFolder != null) {
                mDriveFolder = driveFolder;
                Log.i(TAG, "Connected.");
                return true;
            }
        }

        mClient.disconnect();
        return false;
    }

    /**
     * Disconnect the client from Google Drive.
     */
    public void disconnect() {
        if(mClient != null) {
            mClient.disconnect();
            Log.i(TAG, "Disconnected.");
        }
    }

    /**
     * Is this client connected?
     *
     * @return Whether the client is connected
     */
    public boolean isConnected() {
        return mDriveFolder != null;
    }

    /**
     * Get a reference to the Drive folder, creating it if necessary.
     *
     * @return The Drive folder
     */
    private DriveFolder openDriveFolder() {
        final DriveFolder rootFolder = Drive.DriveApi.getRootFolder(mClient);
        final Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, DRIVE_FOLDER))
                .addFilter(Filters.eq(SearchableField.TRASHED, false)).build();
        final DriveApi.MetadataBufferResult result =
                rootFolder.queryChildren(mClient, query).await();
        try {
            final MetadataBuffer buffer = result.getMetadataBuffer();
            if(buffer != null && buffer.getCount() > 0) {
                return buffer.get(0).getDriveId().asDriveFolder();
            } else {
                return createDriveFolder(rootFolder);
            }
        } finally {
            result.release();
        }
    }

    /**
     * Create the Drive folder.
     *
     * @param rootFolder The Drive root folder
     * @return The new Drive folder
     */
    private DriveFolder createDriveFolder(DriveFolder rootFolder) {
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(DRIVE_FOLDER)
                .build();
        final DriveFolder.DriveFolderResult result =
                rootFolder.createFolder(mClient, changeSet).await();

        if(result.getStatus().isSuccess()) {
            return result.getDriveFolder();
        }

        return null;
    }

    /**
     * Upload photos without a Drive ID.
     */
    public void pushPhotos() {
        Log.i(TAG, "Pushing photos...");
        final ContentResolver cr = mContext.getContentResolver();
        final String[] projection = new String[] {
                Tables.Photos._ID,
                Tables.Photos.ENTRY,
                Tables.Photos.HASH,
                Tables.Photos.PATH
        };
        final String where = Tables.Photos.DRIVE_ID + " IS NULL";
        final Cursor cursor = cr.query(Tables.Photos.CONTENT_URI, projection, where, null, null);
        if(cursor == null) {
            return;
        }
        try {
            boolean changed = false;
            long id;
            long entryId;
            String hash;
            String filePath;
            File file;
            DriveFile driveFile;
            final ContentValues values = new ContentValues();
            while(cursor.moveToNext()) {
                filePath = cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH));
                if(filePath == null) {
                    continue;
                }
                file = new File(filePath);

                values.clear();

                hash = cursor.getString(cursor.getColumnIndex(Tables.Photos.HASH));
                if(hash == null) {
                    hash = PhotoUtils.getMD5Hash(file);
                    if(hash == null) {
                        continue;
                    }
                    values.put(Tables.Photos.HASH, hash);
                }

                driveFile = uploadFile(file, hash);
                if(driveFile != null) {
                    values.put(Tables.Photos.DRIVE_ID, driveFile.getDriveId().encodeToString());
                }

                if(values.size() > 0) {
                    changed = true;
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                    entryId = cursor.getLong(cursor.getColumnIndex(Tables.Photos.ENTRY));
                    cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, id),
                            values, null, null);

                    EntryUtils.markChanged(cr, entryId);
                }
            }

            if(changed) {
                BackendUtils.requestDataSync(mContext);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Download all photos without a local path.
     */
    public void fetchPhotos() {
        Log.i(TAG, "Fetching photos...");
        final ContentResolver cr = mContext.getContentResolver();
        final String[] projection = new String[] {
                Tables.Photos._ID,
                Tables.Photos.ENTRY,
                Tables.Photos.DRIVE_ID
        };
        final String where = Tables.Photos.PATH + " IS NULL";
        final Cursor cursor = cr.query(Tables.Photos.CONTENT_URI, projection, where, null, null);
        if(cursor == null) {
            return;
        }
        try {
            long id;
            long entryId;
            String driveId;
            String filePath;
            final ContentValues values = new ContentValues();
            while(cursor.moveToNext()) {
                driveId = cursor.getString(cursor.getColumnIndex(Tables.Photos.DRIVE_ID));
                if(driveId == null) {
                    continue;
                }
                filePath = downloadPhoto(driveId);
                if(filePath == null) {
                    continue;
                }
                id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                entryId = cursor.getLong(cursor.getColumnIndex(Tables.Photos.ENTRY));
                values.put(Tables.Photos.PATH, filePath);
                cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, id), values,
                        null, null);
                PhotoUtils.deleteThumb(mContext, entryId);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Upload a file to Drive if it does not already exist.
     *
     * @param file The local file
     * @param hash The file hash
     * @return The DriveFile
     */
    private DriveFile uploadFile(File file, String hash) {
        DriveFile driveFile = getDriveFile(hash);
        if(driveFile != null) {
            return driveFile;
        }

        final DriveApi.DriveContentsResult result =
                Drive.DriveApi.newDriveContents(mClient).await();
        if(!result.getStatus().isSuccess()) {
            return null;
        }

        final DriveContents driveContents = result.getDriveContents();
        try {
            final InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            final OutputStream outputStream =
                    new BufferedOutputStream(driveContents.getOutputStream());
            try {
                final byte[] buffer = new byte[8192];
                int read;
                while((read = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
            } finally {
                inputStream.close();
                outputStream.close();
            }

            createNewFile(file.getName(), hash, driveContents);
            return null;
        } catch(IOException e) {
            Log.w(TAG, "Upload failed", e);
        }

        driveContents.discard(mClient);
        return null;
    }

    /**
     * Download a file from Drive.
     *
     * @param driveId The Drive ID
     * @return The path to the downloaded file
     */
    private String downloadPhoto(String driveId) {
        final DriveFile driveFile = DriveId.decodeFromString(driveId).asDriveFile();
        if(driveFile == null) {
            return null;
        }

        final DriveApi.DriveContentsResult result =
                driveFile.open(mClient, DriveFile.MODE_READ_ONLY, null).await();
        if(!result.getStatus().isSuccess()) {
            return null;
        }

        final DriveContents driveContents = result.getDriveContents();
        if(driveContents == null) {
            return null;
        }

        try {
            final Metadata metadata = driveFile.getMetadata(mClient).await().getMetadata();
            final String fileName = metadata.getOriginalFilename();
            File outputFile = new File(PhotoUtils.getMediaStorageDir(), fileName);
            if(outputFile.exists()) {
                final String hash = metadata.getCustomProperties().get(sHashKey);
                if(hash != null && hash.equals(PhotoUtils.getMD5Hash(outputFile))) {
                    return outputFile.getPath();
                }
                outputFile = getUniqueFile(outputFile);
            }
            final OutputStream outputStream =
                    new BufferedOutputStream(new FileOutputStream(outputFile));
            final InputStream inputStream = new BufferedInputStream(driveContents.getInputStream());
            try {
                final byte[] buffer = new byte[8192];
                int read;
                while((read = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
            } finally {
                inputStream.close();
                outputStream.close();
            }
            return outputFile.getPath();
        } catch(IOException e) {
            Log.w(TAG, "Download failed", e);
        } finally {
            driveContents.discard(mClient);
        }

        return null;
    }

    /**
     * Get a file from the Drive folder.
     *
     * @param hash The file hash
     * @return The DriveFile or null if it doesn't exist.
     */
    private DriveFile getDriveFile(String hash) {
        final Query query = new Query.Builder()
                .addFilter(Filters.eq(sHashKey, hash)).build();
        final DriveApi.MetadataBufferResult result =
                mDriveFolder.queryChildren(mClient, query).await();
        try {
            final MetadataBuffer buffer = result.getMetadataBuffer();
            if(buffer != null && buffer.getCount() > 0) {
                return buffer.get(0).getDriveId().asDriveFile();
            }
        } finally {
            result.release();
        }
        return null;
    }

    /**
     * Create a new file on Drive.
     *
     * @param title    The name of the file
     * @param hash     The file hash
     * @param contents The file content
     */
    private void createNewFile(String title, String hash, DriveContents contents) {
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(title)
                .setCustomProperty(sHashKey, hash).build();
        final ExecutionOptions options = new ExecutionOptions.Builder()
                .setNotifyOnCompletion(true).build();
        mDriveFolder.createFile(mClient, changeSet, contents, options).await();
    }

    /**
     * Create a unique file name in case of conflicts.
     *
     * @param original The original file
     * @return The uniquely named file
     */
    private static File getUniqueFile(File original) {
        final String origName = original.getName();
        final int extPos = origName.lastIndexOf('.');
        final String name, ext;
        if(extPos == -1) {
            name = origName;
            ext = "";
        } else {
            name = origName.substring(0, extPos);
            ext = origName.substring(extPos);
        }
        int num = 1;
        while(original.exists()) {
            original = new File(original.getParentFile(), name + " (" + num++ + ")" + ext);
        }
        return original;
    }
}
