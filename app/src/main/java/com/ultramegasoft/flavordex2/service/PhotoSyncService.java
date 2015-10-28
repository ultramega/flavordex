package com.ultramegasoft.flavordex2.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Service to handle syncing photos with Google Drive.
 *
 * @author Steve Guidetti
 */
public class PhotoSyncService extends IntentService {
    /**
     * The name of the Drive folder to store photos
     */
    private static final String DRIVE_FOLDER = "Flavordex Photos";

    /**
     * The Google API Client
     */
    private GoogleApiClient mClient;

    /**
     * Send all local photos that do not exist on Drive to Drive.
     *
     * @param context The Context
     */
    public static void syncPhotos(Context context) {
        final Intent intent = new Intent(context, PhotoSyncService.class);
        context.startService(intent);
    }

    public PhotoSyncService() {
        super("PhotoSyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();
        final ConnectionResult result = mClient.blockingConnect();
        if(result.isSuccess()) {
            final DriveFolder driveFolder = openDriveFolder();
            if(driveFolder != null) {
                syncPhotos(driveFolder);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mClient != null) {
            mClient.disconnect();
        }
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
     * Send all local photos that do not exist on Drive to Drive.
     *
     * @param driveFolder The Drive folder
     */
    private void syncPhotos(DriveFolder driveFolder) {
        final ContentResolver cr = getContentResolver();
        final String where = Tables.Photos.DRIVE_ID + " IS NULL";
        final Cursor cursor = cr.query(Tables.Photos.CONTENT_URI, null, where, null, null);
        if(cursor == null) {
            return;
        }
        try {
            long id;
            long entryId;
            File file;
            DriveFile driveFile;
            final ContentValues values = new ContentValues();
            while(cursor.moveToNext()) {
                file = new File(cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH)));
                if(file.canRead()) {
                    driveFile = putFile(driveFolder, file);
                    if(driveFile == null) {
                        continue;
                    }

                    id = cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                    entryId = cursor.getLong(cursor.getColumnIndex(Tables.Photos.ENTRY));
                    values.put(Tables.Photos.DRIVE_ID, driveFile.getDriveId().getResourceId());
                    cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, id),
                            values, null, null);
                    EntryUtils.markChanged(cr, entryId);
                }
            }
            BackendUtils.notifyDataChanged(this);
        } finally {
            cursor.close();
        }
    }

    /**
     * Get a file from the Drive folder.
     *
     * @param driveFolder The Drive folder
     * @param fileName    The file name
     * @return The DriveFile or null if it doesn't exist.
     */
    private DriveFile getDriveFile(DriveFolder driveFolder, String fileName) {
        final Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, fileName)).build();
        final DriveApi.MetadataBufferResult result =
                driveFolder.queryChildren(mClient, query).await();
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
     * Upload a file to Drive if it does not already exist.
     *
     * @param driveFolder The Drive folder
     * @param file        The local file
     * @return The DriveFile
     */
    private DriveFile putFile(DriveFolder driveFolder, File file) {
        DriveFile driveFile = getDriveFile(driveFolder, file.getName());
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
            final OutputStream outputStream = driveContents.getOutputStream();
            try {
                while(inputStream.available() > 0) {
                    outputStream.write(inputStream.read());
                }
            } finally {
                outputStream.close();
            }

            driveFile = createNewFile(driveFolder, file.getName(), driveContents);
            if(driveFile != null) {
                return driveFile;
            }
        } catch(IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }

        driveContents.discard(mClient);
        return null;
    }

    /**
     * Create a new file on Drive.
     *
     * @param driveFolder The Drive folder
     * @param name        The file name
     * @param contents    The file content
     * @return The DriveFile for the new file
     */
    private DriveFile createNewFile(DriveFolder driveFolder, String name, DriveContents contents) {
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(name).build();
        final DriveFolder.DriveFileResult result =
                driveFolder.createFile(mClient, changeSet, contents).await();
        if(result.getStatus().isSuccess()) {
            return result.getDriveFile();
        }

        return null;
    }
}
