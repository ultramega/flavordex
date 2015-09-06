package com.ultramegasoft.flavordex2.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;

/**
 * Utilities for managing journal entries.
 *
 * @author Steve Guidetti
 */
public class EntryUtils {
    /**
     * Delete an entry.
     *
     * @param context The Context
     * @param id      The entry's database ID
     */
    public static void delete(Context context, long id) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, id);
        cr.delete(uri, null, null);
    }

    /**
     * Remove a photo from an entry.
     *
     * @param context The Context
     * @param photoId The photo's database ID
     */
    public static void deletePhoto(Context context, long photoId) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(Tables.Photos.CONTENT_ID_URI_BASE, photoId);
        final String[] projection = new String[] {
                Tables.Photos.ENTRY
        };
        final Cursor cursor = cr.query(uri, projection, null, null, null);
        try {
            if(cursor.moveToFirst()) {
                cr.delete(uri, null, null);

                final long entryId = cursor.getLong(0);
                PhotoUtils.generateThumb(context, entryId);
                cr.notifyChange(ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE,
                        entryId), null);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Get a sharing Intent.
     *
     * @param context The Context
     * @param title   The message title
     * @param rating  The rating to show
     * @return A send action Intent or null if the title is blank
     */
    public static Intent getShareIntent(Context context, String title, float rating) {
        if(TextUtils.isEmpty(title)) {
            return null;
        }
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject(context, title));
        intent.putExtra(Intent.EXTRA_TEXT, getShareBody(context, title, rating));

        return intent;
    }

    /**
     * Get the message body for the share Intent.
     *
     * @param context The Context
     * @param title   The message title
     * @param rating  The rating to show
     * @return The message body
     */
    private static String getShareBody(Context context, String title, float rating) {
        final String app = context.getString(R.string.app_name);
        return context.getString(R.string.share_body, title, app, rating);
    }

    /**
     * Get the message subject for the share Intent.
     *
     * @param context The Context
     * @param title   The message title
     * @return The message subject
     */
    private static String getShareSubject(Context context, String title) {
        return context.getString(R.string.share_subject, title);
    }
}
