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
import android.database.Cursor;
import android.util.Log;

import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;

/**
 * Service to handle events from Google Drive.
 *
 * @author Steve Guidetti
 */
public class DriveService extends DriveEventService {
    private static final String TAG = "DriveService";

    /**
     * The custom property key to identify photos
     */
    private static final CustomPropertyKey sHashKey =
            new CustomPropertyKey("hash", CustomPropertyKey.PUBLIC);

    @Override
    public void onCompletion(CompletionEvent event) {
        Log.i(TAG, "Received completion event from Drive.");
        if(event.getStatus() == CompletionEvent.STATUS_SUCCESS) {
            final MetadataChangeSet metadata = event.getModifiedMetadataChangeSet();
            final String hash = metadata.getCustomPropertyChangeMap().get(sHashKey);
            Log.i(TAG, "Photo hash: " + hash);
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
                            values.put(Tables.Photos.DRIVE_ID, event.getDriveId().getResourceId());
                            cr.update(ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE,
                                    id), values, null, null);
                            EntryUtils.markChanged(cr, entryId);
                            BackendUtils.requestDataSync(this);
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
