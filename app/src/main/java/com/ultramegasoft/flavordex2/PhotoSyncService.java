package com.ultramegasoft.flavordex2;

import android.app.IntentService;
import android.content.ContentResolver;
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
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.ultramegasoft.flavordex2.provider.Tables;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Service to handle syncing photos with Google Drive.
 *
 * @author Steve Guidetti
 */
public class PhotoSyncService extends IntentService {
    /**
     * Keys for the Intent extras
     */
    private static final String EXTRA_COMMAND = "command";
    private static final String EXTRA_FILE = "file";

    /**
     * Commands this Service will accept
     */
    private static final int COMMAND_SYNC = 0;
    private static final int COMMAND_PUT = 1;

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
        intent.putExtra(EXTRA_COMMAND, COMMAND_SYNC);
        context.startService(intent);
    }

    /**
     * Send a local file to Drive.
     *
     * @param context The Context
     * @param path    The path to the local file
     */
    public static void putFile(Context context, String path) {
        final Intent intent = new Intent(context, PhotoSyncService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_PUT);
        intent.putExtra(EXTRA_FILE, path);
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
                switch(intent.getIntExtra(EXTRA_COMMAND, -1)) {
                    case COMMAND_SYNC:
                        syncPhotos(driveFolder);
                        break;
                    case COMMAND_PUT:
                        final File file = new File(intent.getStringExtra(EXTRA_FILE));
                        if(file.canRead() && !driveFileExists(driveFolder, file.getName())) {
                            putFile(driveFolder, file);
                        }
                        break;
                }
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
        final HashMap<String, DriveFile> driveFiles = getDriveFiles(driveFolder);
        if(driveFiles == null) {
            return;
        }

        final ContentResolver cr = getContentResolver();
        final Cursor cursor = cr.query(Tables.Photos.CONTENT_URI, null, null, null, null);
        if(cursor == null) {
            return;
        }
        try {
            File file;
            while(cursor.moveToNext()) {
                file = new File(cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH)));
                if(file.canRead()) {
                    if(!driveFiles.containsKey(file.getName())) {
                        putFile(driveFolder, file);
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Get a list of all files in the Drive folder.
     *
     * @param driveFolder The Drive root folder
     * @return Map of file names to DriveFiles
     */
    private HashMap<String, DriveFile> getDriveFiles(DriveFolder driveFolder) {
        final DriveApi.MetadataBufferResult result = driveFolder.listChildren(mClient).await();
        try {
            final MetadataBuffer buffer = result.getMetadataBuffer();
            if(buffer != null) {
                final HashMap<String, DriveFile> driveFiles = new HashMap<>();
                for(Metadata metadata : buffer) {
                    driveFiles.put(metadata.getTitle(), metadata.getDriveId().asDriveFile());
                }
                return driveFiles;
            }
        } finally {
            result.release();
        }

        return null;
    }

    /**
     * Check if a file exists on Drive.
     *
     * @param driveFolder The Drive folder
     * @param fileName    The file name
     * @return Whether the named file exists in the Drive folder
     */
    private boolean driveFileExists(DriveFolder driveFolder, String fileName) {
        final Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, fileName)).build();
        final DriveApi.MetadataBufferResult result =
                driveFolder.queryChildren(mClient, query).await();
        try {
            final MetadataBuffer buffer = result.getMetadataBuffer();
            return buffer != null && buffer.getCount() > 0;
        } finally {
            result.release();
        }
    }

    /**
     * Upload a file to Drive.
     *
     * @param driveFolder The Drive folder
     * @param file        The local file
     */
    private void putFile(DriveFolder driveFolder, File file) {
        final DriveFile driveFile = openNewFile(driveFolder, file.getName());
        if(driveFile == null) {
            return;
        }

        final DriveApi.DriveContentsResult result =
                driveFile.open(mClient, DriveFile.MODE_WRITE_ONLY, null).await();
        if(!result.getStatus().isSuccess()) {
            return;
        }

        final DriveContents driveContents = result.getDriveContents();
        try {
            final InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            final OutputStream outputStream = driveContents.getOutputStream();
            while(inputStream.available() > 0) {
                outputStream.write(inputStream.read());
            }

            driveContents.commit(mClient, null).await();
        } catch(IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
            driveContents.discard(mClient);
        }
    }

    /**
     * Create a new empty file on Drive.
     *
     * @param driveFolder The Drive folder
     * @param name        The file name
     * @return The DriveFile for the new file
     */
    private DriveFile openNewFile(DriveFolder driveFolder, String name) {
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(name).build();
        final DriveFolder.DriveFileResult result =
                driveFolder.createFile(mClient, changeSet, null).await();
        if(result.getStatus().isSuccess()) {
            return result.getDriveFile();
        }

        return null;
    }
}
