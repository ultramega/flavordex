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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.EditEntryActivity;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Fragment to display the main details of a journal entry.
 *
 * @author Steve Guidetti
 */
public class ViewInfoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Loader IDs
     */
    private static final int LOADER_MAIN = 0;
    private static final int LOADER_EXTRAS = 1;

    /**
     * The database ID for this entry
     */
    private long mEntryId;

    /**
     * The name of the entry category
     */
    @Nullable
    private String mEntryCat;

    /**
     * All the Views for displaying details
     */
    private TextView mTxtTitle;
    private RatingBar mRatingBar;
    private TextView mTxtMaker;
    private TextView mTxtOrigin;
    private TextView mTxtPrice;
    private TextView mTxtLocation;
    private TextView mTxtDate;
    private TextView mTxtNotes;

    /**
     * The entry title
     */
    @Nullable
    private String mTitle;

    /**
     * The entry rating
     */
    private float mRating;

    /**
     * List of extra field TableRows
     */
    @NonNull
    private final ArrayList<View> mExtraRows = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if(args != null) {
            mEntryId = args.getLong(ViewEntryFragment.ARG_ENTRY_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_MAIN, null, this);
        getLoaderManager().initLoader(LOADER_EXTRAS, null, this);
    }

    @NonNull
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(getLayoutId(), container, false);

        mTxtTitle = rootView.findViewById(R.id.entry_title);
        mRatingBar = rootView.findViewById(R.id.entry_rating);
        mTxtMaker = rootView.findViewById(R.id.entry_maker);
        mTxtOrigin = rootView.findViewById(R.id.entry_origin);
        mTxtPrice = rootView.findViewById(R.id.entry_price);
        mTxtLocation = rootView.findViewById(R.id.entry_location);
        mTxtDate = rootView.findViewById(R.id.entry_date);
        mTxtNotes = rootView.findViewById(R.id.entry_notes);

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setHasOptionsMenu(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.view_info_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final Context context = getContext();
        if(context == null || mTitle == null) {
            return;
        }

        final MenuItem shareItem = menu.findItem(R.id.menu_share);
        if(shareItem != null) {
            final Intent shareIntent = EntryUtils.getShareIntent(context, mTitle, mRating);
            final ShareActionProvider actionProvider =
                    (ShareActionProvider)MenuItemCompat.getActionProvider(shareItem);
            if(actionProvider != null) {
                actionProvider.setShareIntent(shareIntent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_edit_entry:
                final Context context = getContext();
                if(context != null) {
                    EditEntryActivity.startActivity(context, mEntryId, mEntryCat);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the ID for the layout to use.
     *
     * @return An ID from R.layout
     */
    protected int getLayoutId() {
        return R.layout.fragment_view_info;
    }

    /**
     * Fills the Views with data.
     *
     * @param data The Cursor set to the correct row
     */
    private void populateViews(@NonNull Cursor data) {
        mTxtTitle.setText(mTitle);

        final String maker = data.getString(data.getColumnIndex(Tables.Entries.MAKER));
        final String origin = data.getString(data.getColumnIndex(Tables.Entries.ORIGIN));
        if(TextUtils.isEmpty(maker)) {
            setViewText(mTxtMaker, origin);
            mTxtOrigin.setVisibility(View.GONE);
        } else if(TextUtils.isEmpty(origin)) {
            setViewText(mTxtMaker, maker);
            mTxtOrigin.setVisibility(View.GONE);
        } else {
            setViewText(mTxtMaker, maker);
            setViewText(mTxtOrigin, origin);
            mTxtOrigin.setVisibility(View.VISIBLE);
        }

        setViewText(mTxtPrice, data.getString(data.getColumnIndex(Tables.Entries.PRICE)));

        String date = null;
        final long timestamp = data.getLong(data.getColumnIndex(Tables.Entries.DATE));
        if(timestamp > 0) {
            final String format = getResources().getString(R.string.date_time_format);
            date = new SimpleDateFormat(format, Locale.US).format(new Date(timestamp));
        }

        final String location = data.getString(data.getColumnIndex(Tables.Entries.LOCATION));
        if(TextUtils.isEmpty(location)) {
            setViewText(mTxtLocation, date);
            mTxtDate.setVisibility(View.GONE);
        } else {
            setViewText(mTxtLocation, location);
            setViewText(mTxtDate, date);
            mTxtDate.setVisibility(View.VISIBLE);
        }

        mRatingBar.setRating(mRating);
        mTxtNotes.setText(data.getString(data.getColumnIndex(Tables.Entries.NOTES)));

        final Activity activity = getActivity();
        if(activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    /**
     * Populates the table of extra fields.
     *
     * @param data A LinkedHashMap containing the extra values
     */
    protected void populateExtras(@NonNull LinkedHashMap<String, ExtraFieldHolder> data) {
        final Activity activity = getActivity();
        if(activity == null) {
            return;
        }

        final TableLayout table = activity.findViewById(R.id.entry_info);
        if(!mExtraRows.isEmpty()) {
            for(View tableRow : mExtraRows) {
                table.removeView(tableRow);
            }
            mExtraRows.clear();
        }
        if(data.size() > 0) {
            final LayoutInflater inflater = LayoutInflater.from(activity);
            for(ExtraFieldHolder extra : data.values()) {
                if(extra.preset) {
                    continue;
                }
                final View root = inflater.inflate(R.layout.view_info_extra, table, false);
                ((TextView)root.findViewById(R.id.label))
                        .setText(getString(R.string.label_field, extra.name));
                ((TextView)root.findViewById(R.id.value)).setText(extra.value);
                table.addView(root);
                mExtraRows.add(root);
            }
        }
    }

    /**
     * Set the text of a TextView, replacing empty values to a placeholder.
     *
     * @param view  The TextView
     * @param value The text
     */
    protected static void setViewText(@NonNull TextView view, @Nullable CharSequence value) {
        if(TextUtils.isEmpty(value)) {
            view.setText(R.string.hint_empty);
        } else {
            view.setText(value);
        }
    }

    /**
     * Convert a numeric string to an integer.
     *
     * @param string A numeric string
     * @return The integer value or 0 if the string is not numeric
     */
    protected static int stringToInt(@Nullable String string) {
        try {
            return Integer.valueOf(string);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Convert a numeric string to a float.
     *
     * @param string A numeric string
     * @return The float value or 0 if the string is not numeric
     */
    protected static float stringToFloat(@Nullable String string) {
        if(string == null) {
            return 0;
        }
        try {
            return Float.valueOf(string);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get the value from an extra field with a null check.
     *
     * @param extra The extra field
     * @return The value or null if extra is null
     */
    @Nullable
    protected static String getExtraValue(@Nullable ExtraFieldHolder extra) {
        if(extra == null) {
            return null;
        }
        return extra.value;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Context context = getContext();
        if(context == null) {
            return null;
        }

        Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
        switch(id) {
            case LOADER_MAIN:
                return new CursorLoader(context, uri, null, null, null, null);
            case LOADER_EXTRAS:
                uri = Uri.withAppendedPath(uri, "extras");
                return new CursorLoader(context, uri, null, null, null,
                        Tables.EntriesExtras.EXTRA + " ASC");
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final int id = loader.getId();
        switch(id) {
            case LOADER_MAIN:
                if(data.moveToFirst()) {
                    mEntryCat = data.getString(data.getColumnIndex(Tables.Entries.CAT));
                    mTitle = data.getString(data.getColumnIndex(Tables.Entries.TITLE));
                    mRating = data.getFloat(data.getColumnIndex(Tables.Entries.RATING));
                    populateViews(data);
                }
                break;
            case LOADER_EXTRAS:
                data.moveToPosition(-1);
                final LinkedHashMap<String, ExtraFieldHolder> extras = new LinkedHashMap<>();
                String name;
                String value;
                boolean preset;
                while(data.moveToNext()) {
                    name = data.getString(data.getColumnIndex(Tables.Extras.NAME));
                    value = data.getString(data.getColumnIndex(Tables.EntriesExtras.VALUE));
                    preset = data.getInt(data.getColumnIndex(Tables.Extras.PRESET)) == 1;
                    extras.put(name, new ExtraFieldHolder(0, name, preset, value));
                }
                populateExtras(extras);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
