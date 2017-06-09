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
package com.ultramegasoft.flavordex2.fragment;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.DateInputWidget;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Fragment for editing details for a new or existing journal entry.
 *
 * @author Steve Guidetti
 */
public class EditInfoFragment extends LoadingProgressFragment
        implements LoaderManager.LoaderCallbacks {
    /**
     * Keys for the Fragment arguments
     */
    public static final String ARG_ENTRY_ID = "entry_id";

    /**
     * Loader IDs
     */
    private static final int LOADER_MAIN = 0;

    /**
     * Keys for the saved state
     */
    private static final String STATE_EXTRAS = "extras";

    /**
     * The Views for the form fields
     */
    private DateInputWidget mDateInputWidget;
    private RatingBar mRatingBar;

    /**
     * The category ID for the entry being added
     */
    private long mCatId;

    /**
     * The entry ID to edit
     */
    private long mEntryId;

    /**
     * True while data is loading
     */
    private boolean mIsLoading;

    /**
     * The EntryFormHelper
     */
    private EntryFormHelper mFormHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        final Bundle args = getArguments();
        mCatId = args.getLong(AddEntryFragment.ARG_CAT_ID);
        mEntryId = args.getLong(ARG_ENTRY_ID);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState == null) {
            getLoaderManager().initLoader(LOADER_MAIN, null, this).forceLoad();
        } else {
            //noinspection unchecked
            mFormHelper.setExtras((LinkedHashMap<String, ExtraFieldHolder>)savedInstanceState
                    .getSerializable(STATE_EXTRAS));
            hideLoadingIndicator(false);
        }
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mFormHelper = createHelper(root);

        mDateInputWidget = (DateInputWidget)root.findViewById(R.id.entry_date);
        mRatingBar = (RatingBar)root.findViewById(R.id.entry_rating);

        final Date date = new Date();
        mDateInputWidget.setDate(date);
        mDateInputWidget.setMaxDate(date);

        mFormHelper.mTxtLocation.setText(((FlavordexApp)getActivity().getApplication())
                .getLocationName());

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info;
    }

    /**
     * Create the EntryFormHelper.
     *
     * @param root The root of the layout
     * @return The EntryFormHelper
     */
    @NonNull
    protected EntryFormHelper createHelper(@NonNull View root) {
        return new EntryFormHelper(this, root);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_EXTRAS, mFormHelper.getExtras());
    }

    /**
     * Load the values for the main entry fields.
     *
     * @param entry The entry
     */
    private void populateFields(@NonNull EntryHolder entry) {
        if(entry != null) {
            mFormHelper.mTxtTitle.setText(entry.title);
            mFormHelper.mTxtMaker.setText(entry.maker);
            mFormHelper.mTxtOrigin.setText(entry.origin);
            mFormHelper.mTxtPrice.setText(entry.price);
            mFormHelper.mTxtLocation.setText(entry.location);
            mDateInputWidget.setDate(new Date(entry.date));
            mRatingBar.setRating(entry.rating);
            mFormHelper.mTxtNotes.setText(entry.notes);
        }
    }

    /**
     * Test if the required fields are properly filled out.
     *
     * @return Whether the form is valid
     */
    public boolean isValid() {
        if(TextUtils.isEmpty(mFormHelper.mTxtTitle.getText().toString())) {
            mFormHelper.mTxtTitle.setError(getString(R.string.error_required));
            mFormHelper.mTxtTitle.requestFocus();
            return false;
        }
        return !mIsLoading;
    }

    /**
     * Is this fragment currently loading data?
     *
     * @return True while the fragment is loading data.
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Get the data for this entry, including the main info fields and extra fields.
     *
     * @param entry An EntryHolder
     */
    public final void getData(@NonNull EntryHolder entry) {
        entry.id = mEntryId;

        if(entry.id == 0) {
            entry.catId = mCatId;
        }

        entry.title = mFormHelper.mTxtTitle.getText().toString();
        entry.maker = mFormHelper.mTxtMaker.getText().toString();
        entry.origin = mFormHelper.mTxtOrigin.getText().toString();
        entry.price = mFormHelper.mTxtPrice.getText().toString();
        entry.location = mFormHelper.mTxtLocation.getText().toString();
        entry.date = mDateInputWidget.getDate().getTime();
        entry.rating = mRatingBar.getRating();
        entry.notes = mFormHelper.mTxtNotes.getText().toString();

        entry.getExtras().addAll(mFormHelper.getExtras().values());
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_MAIN:
                mIsLoading = true;
                ActivityCompat.invalidateOptionsMenu(getActivity());
                return new DataLoader(getContext(), mCatId, mEntryId);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch(loader.getId()) {
            case LOADER_MAIN:
                final DataLoader.Holder holder = (DataLoader.Holder)data;

                populateFields(holder.entry);
                mFormHelper.setExtras(holder.extras);

                hideLoadingIndicator(true);
                mFormHelper.mTxtTitle.setSelection(mFormHelper.mTxtTitle.getText().length());

                mIsLoading = false;
                ActivityCompat.invalidateOptionsMenu(getActivity());
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    /**
     * Custom Loader to load everything in one task
     */
    public static class DataLoader extends AsyncTaskLoader<DataLoader.Holder> {
        /**
         * The ContentResolver to use
         */
        @NonNull
        private final ContentResolver mResolver;

        /**
         * The category ID
         */
        private long mCatId;

        /**
         * The entry ID, if editing
         */
        private final long mEntryId;

        /**
         * @param context The Context
         * @param catId   The category ID
         * @param entryId The entry ID, if editing
         */
        DataLoader(@NonNull Context context, long catId, long entryId) {
            super(context);
            mResolver = context.getContentResolver();
            mCatId = catId;
            mEntryId = entryId;
        }

        @Override
        public Holder loadInBackground() {
            final Holder holder = new Holder();
            if(mEntryId > 0) {
                final Uri uri =
                        ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
                holder.entry = loadEntry(uri);
                loadExtras(holder);
                loadExtrasValues(holder, uri);
            } else {
                loadExtras(holder);
            }
            return holder;
        }

        /**
         * Load the entry from the database.
         *
         * @param entryUri The Uri for the entry
         * @return The entry
         */
        @NonNull
        private EntryHolder loadEntry(@NonNull Uri entryUri) {
            final EntryHolder entry = new EntryHolder();
            final Cursor cursor =
                    getContext().getContentResolver().query(entryUri, null, null, null, null);
            if(cursor != null) {
                try {
                    if(cursor.moveToFirst()) {
                        entry.title = cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                        entry.maker = cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER));
                        entry.origin =
                                cursor.getString(cursor.getColumnIndex(Tables.Entries.ORIGIN));
                        entry.price = cursor.getString(cursor.getColumnIndex(Tables.Entries.PRICE));
                        entry.location =
                                cursor.getString(cursor.getColumnIndex(Tables.Entries.LOCATION));
                        entry.date = cursor.getLong(cursor.getColumnIndex(Tables.Entries.DATE));
                        entry.rating =
                                cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
                        entry.notes = cursor.getString(cursor.getColumnIndex(Tables.Entries.NOTES));

                        mCatId = cursor.getLong(cursor.getColumnIndex(Tables.Entries.CAT_ID));
                    }
                } finally {
                    cursor.close();
                }
            }
            return entry;
        }

        /**
         * Load the extra fields from the database.
         *
         * @param holder The Holder
         */
        private void loadExtras(@NonNull Holder holder) {
            final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
            final Cursor cursor = mResolver.query(Uri.withAppendedPath(uri, "extras"), null, null,
                    null, Tables.Extras.POS + " ASC");
            if(cursor != null) {
                long id;
                String name;
                boolean preset;
                try {
                    while(cursor.moveToNext()) {
                        id = cursor.getLong(cursor.getColumnIndex(Tables.Extras._ID));
                        name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
                        preset = cursor.getInt(cursor.getColumnIndex(Tables.Extras.PRESET)) == 1;
                        holder.extras.put(name, new ExtraFieldHolder(id, name, preset));
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        /**
         * Load the extra field values from the database.
         *
         * @param holder   The Holder
         * @param entryUri The Uri for the entry
         */
        private void loadExtrasValues(@NonNull Holder holder, @NonNull Uri entryUri) {
            final Cursor cursor = mResolver.query(Uri.withAppendedPath(entryUri, "extras"), null,
                    null, null, null);
            if(cursor != null) {
                String name;
                String value;
                try {
                    while(cursor.moveToNext()) {
                        name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
                        value = cursor.getString(cursor.getColumnIndex(Tables.EntriesExtras.VALUE));
                        holder.extras.get(name).value = value;
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        /**
         * The holder for return data
         */
        public static class Holder {
            /**
             * The entry
             */
            @Nullable
            public EntryHolder entry;

            /**
             * Map of extra field names to their data
             */
            public final LinkedHashMap<String, ExtraFieldHolder> extras = new LinkedHashMap<>();
        }
    }
}
