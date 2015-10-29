package com.ultramegasoft.flavordex2.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;

import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;

/**
 * Service to handle events from Google Drive.
 *
 * @author Steve Guidetti
 */
public class DriveService extends DriveEventService {
    /**
     * The custom property key to identify photos
     */
    private static final CustomPropertyKey sHashKey =
            new CustomPropertyKey("hash", CustomPropertyKey.PUBLIC);

    @Override
    public void onCompletion(CompletionEvent event) {
        if(event.getStatus() == CompletionEvent.STATUS_SUCCESS) {
            final MetadataChangeSet metadata = event.getModifiedMetadataChangeSet();
            final String hash = metadata.getCustomPropertyChangeMap().get(sHashKey);
            if(hash != null) {
                final ContentResolver cr = getContentResolver();

                final String[] projection = new String[] {
                        Tables.Photos._ID,
                        Tables.Photos.ENTRY
                };
                final String where = Tables.Photos.HASH + " = ?";
                final String[] whereArgs = new String[] {hash};
                final Cursor cursor =
                        cr.query(Tables.Photos.CONTENT_URI, projection, where, whereArgs, null);
                if(cursor != null) {
                    try {
                        if(cursor.moveToFirst()) {
                            final long id =
                                    cursor.getLong(cursor.getColumnIndex(Tables.Photos._ID));
                            final long entryId =
                                    cursor.getLong(cursor.getColumnIndex(Tables.Photos.ENTRY));
                            final ContentValues values = new ContentValues();
                            values.put(Tables.Photos.DRIVE_ID, event.getDriveId().encodeToString());
                            cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE,
                                    id), values, null, null);
                            EntryUtils.markChanged(cr, entryId);
                            BackendUtils.notifyDataChanged(this);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
        event.dismiss();
    }
}
