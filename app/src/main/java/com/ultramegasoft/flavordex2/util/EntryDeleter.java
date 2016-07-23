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
