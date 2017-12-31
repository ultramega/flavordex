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
package com.ultramegasoft.flavordex2.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.tasks.Tasks;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Helper for synchronizing photos with Google Drive.
 *
 * @author Steve Guidetti
 */
class PhotoSyncHelper {
    private static final String TAG = "PhotoSyncHelper";

    /**
     * The custom property key to identify photos
     */
    private static final CustomPropertyKey sHashKey =
            new CustomPropertyKey("hash", CustomPropertyKey.PUBLIC);

    /**
     * The Google Drive API Client
     */
    @Nullable
    private DriveClient mClient;

    /**
     * The Google Drive resource API Client
     */
    @Nullable
    private DriveResourceClient mResourceClient;

    /**
     * The Drive folder for the application
     */
    @Nullable
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
    @NonNull
    private final Context mContext;

    /**
     * @param context The Context
     */
    PhotoSyncHelper(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Connect to Google Drive.
     *
     * @return Whether the client connected successfully
     */
    boolean connect() {
        if(isConnected()) {
            return true;
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

        if(setupClient(prefs)) {
            mDriveFolder = getDriveFolder();
            if(mDriveFolder != null) {
                return true;
            }
        }

        disconnect();
        return false;
    }

    /**
     * Set up the Drive API client.
     *
     * @param prefs The SharedPreferences
     * @return Whether the setup was successful
     */
    private boolean setupClient(@NonNull SharedPreferences prefs) {
        final GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        final GoogleSignInClient signInClient = GoogleSignIn.getClient(mContext, signInOptions);
        try {
            final GoogleSignInAccount signInAccount = Tasks.await(signInClient.silentSignIn());

            mClient = Drive.getDriveClient(mContext, signInAccount);
            mResourceClient = Drive.getDriveResourceClient(mContext, signInAccount);

            Log.d(TAG, "Connection successful. sync: " + mShouldSync + " media: " + mMediaMounted);

            return true;
        } catch(ExecutionException e) {
            final ApiException result = (ApiException)e.getCause();
            switch(result.getStatusCode()) {
                case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                    Log.i(TAG, "User not signed in. Disabling photo syncing.");
                    prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false).apply();
                    break;
                case GoogleSignInStatusCodes.API_NOT_CONNECTED:
                case GoogleSignInStatusCodes.NETWORK_ERROR:
                case GoogleSignInStatusCodes.INTERNAL_ERROR:
                    Log.i(TAG, "Google Drive service unavailable. Disabling photo syncing.");
                    prefs.edit().putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false).apply();
            }

            Log.w(TAG, "Connection failed! Reason: " + result.getMessage());
        } catch(InterruptedException ignored) {
        }

        return false;
    }

    /**
     * Get the application folder.
     *
     * @return The DriveFolder
     */
    @Nullable
    private DriveFolder getDriveFolder() {
        if(mClient == null || mResourceClient == null) {
            return null;
        }

        try {
            Tasks.await(mClient.requestSync());
        } catch(ExecutionException | InterruptedException ignored) {
        }

        try {
            return Tasks.await(mResourceClient.getAppFolder());
        } catch(ExecutionException | InterruptedException ignored) {
        }

        Log.w(TAG, "Failed to get application folder.");
        return null;
    }

    /**
     * Disconnect the client from Google Drive.
     */
    void disconnect() {
        mClient = null;
        mResourceClient = null;
        mDriveFolder = null;
    }

    /**
     * Is this client connected?
     *
     * @return Whether the client is connected
     */
    private boolean isConnected() {
        return mDriveFolder != null;
    }

    /**
     * Upload photos without a Drive ID.
     */
    void pushPhotos() {
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
                if(uri == null) {
                    continue;
                }

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
    void deletePhotos() {
        if(mResourceClient == null) {
            return;
        }

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
                    if(driveFile == null) {
                        continue;
                    }

                    try {
                        Tasks.await(mResourceClient.delete(driveFile));
                    } catch(ExecutionException | InterruptedException e) {
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
    void fetchPhotos() {
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
    private void validateDriveIds() {
        if(mResourceClient == null || mDriveFolder == null) {
            return;
        }

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
            try {
                final MetadataBuffer buffer =
                        Tasks.await(mResourceClient.listChildren(mDriveFolder));
                for(Metadata metadata : buffer) {
                    driveIds.add(metadata.getDriveId().getResourceId());
                }
                buffer.release();
            } catch(ExecutionException | InterruptedException e) {
                return;
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
    @Nullable
    private DriveFile uploadFile(@NonNull Uri uri, @NonNull String hash) {
        DriveFile driveFile = getDriveFile(hash);
        if(driveFile != null) {
            return driveFile;
        }

        if(mResourceClient == null || !mShouldSync || !mMediaMounted) {
            return null;
        }

        final ContentResolver cr = mContext.getContentResolver();

        final String name = PhotoUtils.getName(cr, uri);
        if(name == null) {
            return null;
        }

        DriveContents driveContents = null;
        try {
            driveContents = Tasks.await(mResourceClient.createContents());
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
        } catch(ExecutionException | InterruptedException | IOException e) {
            Log.w(TAG, "Upload failed", e);
        }

        if(driveContents != null) {
            mResourceClient.discardContents(driveContents);
        }
        return null;
    }

    /**
     * Download a file from Drive.
     *
     * @param resourceId The Drive resource ID
     * @return The downloaded file
     */
    @Nullable
    private File downloadPhoto(@NonNull String resourceId) {
        if(mClient == null || mResourceClient == null || !mMediaMounted) {
            return null;
        }

        try {
            final DriveFile driveFile = Tasks.await(mClient.getDriveId(resourceId)).asDriveFile();
            final DriveContents driveContents =
                    Tasks.await(mResourceClient.openFile(driveFile, DriveFile.MODE_READ_ONLY));

            final Metadata metadata = Tasks.await(mResourceClient.getMetadata(driveFile));
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
        } catch(ExecutionException | InterruptedException | IOException e) {
            Log.w(TAG, "Download failed", e);
        }

        return null;
    }

    /**
     * Get a file from the Drive folder.
     *
     * @param hash The file hash
     * @return The DriveFile or null if it doesn't exist.
     */
    @Nullable
    private DriveFile getDriveFile(@NonNull String hash) {
        if(mResourceClient == null || mDriveFolder == null) {
            return null;
        }

        try {
            final Query query = new Query.Builder().addFilter(Filters.eq(sHashKey, hash)).build();
            final MetadataBuffer buffer =
                    Tasks.await(mResourceClient.queryChildren(mDriveFolder, query));
            if(buffer != null && buffer.getCount() > 0) {
                final DriveFile driveFile = buffer.get(0).getDriveId().asDriveFile();
                buffer.release();
                return driveFile;
            }
        } catch(ExecutionException | InterruptedException ignored) {
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
    private void createNewFile(@NonNull String title, @NonNull String hash,
                               @NonNull DriveContents contents) {
        if(mResourceClient == null || mDriveFolder == null) {
            return;
        }
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(title)
                .setCustomProperty(sHashKey, hash).build();
        final ExecutionOptions options = new ExecutionOptions.Builder()
                .setNotifyOnCompletion(true).build();
        try {
            Tasks.await(mResourceClient.createFile(mDriveFolder, changeSet, contents, options));
        } catch(ExecutionException | InterruptedException e) {
            Log.w(TAG, "Failed to create remote file", e);
        }
    }

    /**
     * Create a unique file name in case of conflicts.
     *
     * @param original The original file
     * @return The uniquely named file
     */
    @NonNull
    private static File getUniqueFile(@NonNull File original) {
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
