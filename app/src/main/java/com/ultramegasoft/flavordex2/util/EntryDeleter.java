package com.ultramegasoft.flavordex2.util;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Task for deleting an entry in the background.
 *
 * @author Steve Guidetti
 */
public class EntryDeleter extends AsyncTask<Void, Void, Void> {
    /**
     * The Context
     */
    private final Context mContext;

    /**
     * The entry ID
     */
    private final long mEntryId;

    /**
     * @param context The Context
     * @param entryId The entry ID
     */
    public EntryDeleter(Context context, long entryId) {
        mContext = context.getApplicationContext();
        mEntryId = entryId;
    }

    @Override
    protected Void doInBackground(Void... params) {
        EntryUtils.delete(mContext, mEntryId);
        return null;
    }
}
