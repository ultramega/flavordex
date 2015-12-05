package com.ultramegasoft.flavordex2.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
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
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Helper for synchronizing photos with Google Drive.
 *
 * @author Steve Guidetti
 */
public class PhotoSyncHelper {
    private static final String TAG = "PhotoSyncHelper";

    /**
     * Helper to implement exponential backoff
     */
    private static final BackendUtils.ExponentialBackoffHelper sBackoffHelper =
            new BackendUtils.ExponentialBackoffHelper(30, 30, 60 * 15);

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
     * Whether to sync the photo files
     */
    private boolean mShouldSync = true;

    /**
     * Whether the external storage is available
     */
    private boolean mMediaMounted = true;

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
        if(isConnected()) {
            return true;
        }

        if(!sBackoffHelper.shouldExecute()) {
            return false;
        }

        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mShouldSync = false;
            mMediaMounted = false;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if(!Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .canWrite()) {
            if(!PermissionUtils.hasExternalStoragePerm(mContext)) {
                Log.i(TAG, "External storage access permission denied. Disabling photo syncing.");
                prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false).apply();
                return false;
            }
            mShouldSync = false;
            mMediaMounted = false;
        }

        if(prefs.getBoolean(FlavordexApp.PREF_SYNC_PHOTOS_UNMETERED, true)) {
            final ConnectivityManager cm =
                    (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(ConnectivityManagerCompat.isActiveNetworkMetered(cm)) {
                mShouldSync = false;
            }
        }

        mClient = new GoogleApiClient.Builder(mContext)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .build();
        final ConnectionResult result = mClient.blockingConnect();
        if(result.isSuccess()) {
            Log.d(TAG, "Connection successful. sync: " + mShouldSync + " media: " + mMediaMounted);
            mDriveFolder = Drive.DriveApi.getAppFolder(mClient);
            if(mDriveFolder != null) {
                sBackoffHelper.onSuccess();
                return true;
            }
            Log.w(TAG, "Failed to get application folder.");
        } else {
            switch(result.getErrorCode()) {
                case ConnectionResult.SIGN_IN_REQUIRED:
                case ConnectionResult.SIGN_IN_FAILED:
                case ConnectionResult.INVALID_ACCOUNT:
                    Log.i(TAG, "User not signed in. Disabling photo syncing.");
                    prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false).apply();
                    break;
                case ConnectionResult.API_UNAVAILABLE:
                case ConnectionResult.LICENSE_CHECK_FAILED:
                case ConnectionResult.SERVICE_DISABLED:
                case ConnectionResult.SERVICE_MISSING:
                case ConnectionResult.SERVICE_INVALID:
                case ConnectionResult.SERVICE_MISSING_PERMISSION:
                    Log.i(TAG, "Google Drive service unavailable. Disabling photo syncing.");
                    prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false).apply();
            }
        }

        Log.w(TAG, "Connection failed! Reason: " + result.getErrorMessage());
        mClient.disconnect();
        sBackoffHelper.onFail();
        return false;
    }

    /**
     * Disconnect the client from Google Drive.
     */
    public void disconnect() {
        if(mClient != null) {
            mDriveFolder = null;
            mClient.disconnect();
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
     * Upload photos without a Drive ID.
     */
    public void pushPhotos() {
        Log.d(TAG, "Pushing photos.");
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
            boolean requestSync = false;
            boolean changed = false;
            long id;
            long entryId;
            String hash;
            String filePath;
            Uri uri;
            DriveFile driveFile;
            final ContentValues values = new ContentValues();
            while(cursor.moveToNext()) {
                filePath = cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH));
                if(filePath == null) {
                    continue;
                }
                uri = PhotoUtils.parsePath(filePath);

                values.clear();

                hash = cursor.getString(cursor.getColumnIndex(Tables.Photos.HASH));
                if(hash == null) {
                    if(mMediaMounted) {
                        hash = PhotoUtils.getMD5Hash(cr, uri);
                    }
                    if(hash == null) {
                        requestSync = true;
                        continue;
                    }
                    values.put(Tables.Photos.HASH, hash);
                }

                driveFile = uploadFile(uri, hash);
                if(driveFile != null) {
                    values.put(Tables.Photos.DRIVE_ID, driveFile.getDriveId().getResourceId());
                }

                if(values.size() > 0) {
                    changed = true;
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                    entryId = cursor.getLong(cursor.getColumnIndex(Tables.Photos.ENTRY));
                    cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, id),
                            values, null, null);

                    EntryUtils.markChanged(cr, entryId);
                } else if(!mShouldSync || !mMediaMounted) {
                    requestSync = true;
                }
            }

            if(requestSync) {
                BackendUtils.requestPhotoSync(mContext);
            }

            if(changed) {
                BackendUtils.requestDataSync(mContext);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Delete photos from Drive that were removed from the app.
     */
    public void deletePhotos() {
        Log.d(TAG, "Deleting photos.");
        final ContentResolver cr = mContext.getContentResolver();
        final String[] projection = new String[] {
                Tables.Deleted._ID,
                Tables.Deleted.UUID
        };
        final String where = Tables.Deleted.TYPE + " = " + Tables.Deleted.TYPE_PHOTO;
        final Cursor cursor = cr.query(Tables.Deleted.CONTENT_URI, projection, where, null, null);
        if(cursor != null) {
            try {
                DriveFile driveFile;
                long id;
                String hash;
                while(cursor.moveToNext()) {
                    hash = cursor.getString(cursor.getColumnIndex(Tables.Deleted.UUID));
                    driveFile = getDriveFile(hash);
                    if(driveFile != null && !driveFile.delete(mClient).await().isSuccess()) {
                        continue;
                    }
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Deleted._ID));
                    cr.delete(Tables.Deleted.CONTENT_URI, Tables.Deleted._ID + " = " + id, null);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Download all photos without a local path.
     */
    public void fetchPhotos() {
        validateDriveIds();
        Log.d(TAG, "Fetching photos.");
        final ContentResolver cr = mContext.getContentResolver();
        final String[] projection = new String[] {
                Tables.Photos._ID,
                Tables.Photos.ENTRY,
                Tables.Photos.DRIVE_ID
        };
        final String where = Tables.Photos.PATH + " IS NULL AND " + Tables.Photos.DRIVE_ID
                + " NOT NULL";
        final Cursor cursor = cr.query(Tables.Photos.CONTENT_URI, projection, where, null, null);
        if(cursor == null) {
            return;
        }
        try {
            if(!mMediaMounted && cursor.getCount() > 0) {
                BackendUtils.requestPhotoSync(mContext);
                return;
            }
            boolean requestSync = false;
            long id;
            long entryId;
            String driveId;
            File file;
            final ContentValues values = new ContentValues();
            while(cursor.moveToNext()) {
                driveId = cursor.getString(cursor.getColumnIndex(Tables.Photos.DRIVE_ID));
                file = downloadPhoto(driveId);
                if(file == null) {
                    requestSync = true;
                    continue;
                }
                id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                entryId = cursor.getLong(cursor.getColumnIndex(Tables.Photos.ENTRY));
                values.put(Tables.Photos.PATH, file.getName());
                cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, id), values,
                        null, null);
                PhotoUtils.deleteThumb(mContext, entryId);
            }

            if(requestSync) {
                BackendUtils.requestPhotoSync(mContext);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Ensure that the local Drive IDs exists in the Drive folder.
     */
    public void validateDriveIds() {
        Log.d(TAG, "Validating Drive IDs.");
        final ContentResolver cr = mContext.getContentResolver();
        final String[] projection = new String[] {
                Tables.Photos._ID,
                Tables.Photos.DRIVE_ID
        };
        final String where = Tables.Photos.DRIVE_ID + " NOT NULL";
        final Cursor cursor = cr.query(Tables.Photos.CONTENT_URI, projection, where, null, null);
        if(cursor == null) {
            return;
        }
        try {
            final ArrayList<String> driveIds = new ArrayList<>();
            final DriveApi.MetadataBufferResult result = mDriveFolder.listChildren(mClient).await();
            try {
                final MetadataBuffer buffer = result.getMetadataBuffer();
                for(Metadata metadata : buffer) {
                    driveIds.add(metadata.getDriveId().getResourceId());
                }
            } finally {
                result.release();
            }

            long id;
            String driveId;
            final ContentValues values = new ContentValues();
            values.put(Tables.Photos.DRIVE_ID, (String)null);
            while(cursor.moveToNext()) {
                driveId = cursor.getString(cursor.getColumnIndex(Tables.Photos.DRIVE_ID));
                if(!driveIds.contains(driveId)) {
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                    cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, id),
                            values, null, null);
                }
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Upload a file to Drive if it does not already exist.
     *
     * @param uri  The Uri to the image file
     * @param hash The file hash
     * @return The DriveFile
     */
    private DriveFile uploadFile(Uri uri, String hash) {
        DriveFile driveFile = getDriveFile(hash);
        if(driveFile != null) {
            return driveFile;
        }

        if(!mShouldSync || !mMediaMounted) {
            return null;
        }

        final ContentResolver cr = mContext.getContentResolver();

        final String name = PhotoUtils.getName(cr, uri);
        if(name == null) {
            return null;
        }

        final DriveApi.DriveContentsResult result =
                Drive.DriveApi.newDriveContents(mClient).await();
        if(!result.getStatus().isSuccess()) {
            return null;
        }

        final DriveContents driveContents = result.getDriveContents();
        try {
            final InputStream inputStream = cr.openInputStream(uri);
            if(inputStream == null) {
                return null;
            }
            final OutputStream outputStream = driveContents.getOutputStream();
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

            createNewFile(name, hash, driveContents);
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
     * @param resourceId The Drive resource ID
     * @return The downloaded file
     */
    private File downloadPhoto(String resourceId) {
        if(!mMediaMounted) {
            return null;
        }

        final DriveId driveId =
                Drive.DriveApi.fetchDriveId(mClient, resourceId).await().getDriveId();
        if(driveId == null) {
            return null;
        }
        final DriveFile driveFile = driveId.asDriveFile();

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
                if(hash != null && hash.equals(PhotoUtils.getMD5Hash(mContext.getContentResolver(),
                        Uri.fromFile(outputFile)))) {
                    return outputFile;
                }
                if(!mShouldSync) {
                    return null;
                }
                outputFile = getUniqueFile(outputFile);
            }
            if(!mShouldSync) {
                return null;
            }
            final OutputStream outputStream = new FileOutputStream(outputFile);
            final InputStream inputStream = driveContents.getInputStream();
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
            return outputFile;
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
        final Query query = new Query.Builder().addFilter(Filters.eq(sHashKey, hash)).build();
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
