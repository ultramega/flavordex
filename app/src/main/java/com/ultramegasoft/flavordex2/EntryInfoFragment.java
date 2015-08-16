package com.ultramegasoft.flavordex2;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.provider.Tables;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment to display the main details of a journal entry.
 *
 * @author Steve Guidetti
 */
public class EntryInfoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_MAIN = 0;

    /**
     * The database id for this entry
     */
    private long mEntryId;

    /**
     * All the view for displaying details
     */
    private TextView mTxtTitle;
    private RatingBar mRatingBar;
    private TextView mTxtMaker;
    private TextView mTxtOrigin;
    private TextView mTxtLocation;
    private TextView mTxtDate;
    private TextView mTxtPrice;
    private TextView mTxtNotes;

    public EntryInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntryId = getArguments().getLong(EntryDetailFragment.ARG_ITEM_ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTxtTitle = (TextView)getActivity().findViewById(R.id.entry_title);
        mRatingBar = (RatingBar)getActivity().findViewById(R.id.entry_rating);

        getLoaderManager().initLoader(LOADER_MAIN, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_entry_info, container, false);

        mTxtMaker = (TextView)rootView.findViewById(R.id.entry_maker);
        mTxtOrigin = (TextView)rootView.findViewById(R.id.entry_origin);
        mTxtLocation = (TextView)rootView.findViewById(R.id.entry_location);
        mTxtDate = (TextView)rootView.findViewById(R.id.entry_date);
        mTxtPrice = (TextView)rootView.findViewById(R.id.entry_price);
        mTxtNotes = (TextView)rootView.findViewById(R.id.entry_notes);

        return rootView;
    }

    /**
     * Fills the views with data.
     *
     * @param data The cursor set to the correct row
     */
    private void populateViews(Cursor data) {
        mTxtTitle.setText(data.getString(data.getColumnIndex(Tables.Entries.TITLE)));
        mRatingBar.setRating(data.getFloat(data.getColumnIndex(Tables.Entries.RATING)));

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

        String date = null;
        final long timestamp = data.getLong(data.getColumnIndex(Tables.Entries.DATE));
        if(timestamp > 0) {
            final String format = getActivity().getResources().getString(R.string.date_format);
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

        setViewText(mTxtPrice, data.getString(data.getColumnIndex(Tables.Entries.PRICE)));

        mTxtNotes.setText(data.getString(data.getColumnIndex(Tables.Entries.NOTES)));
    }

    /**
     * Set the text of a TextView, replacing empty values to a placeholder.
     *
     * @param view  The view
     * @param value The text
     */
    private void setViewText(TextView view, CharSequence value) {
        if(view == null) {
            return;
        }
        if(TextUtils.isEmpty(value)) {
            view.setText(R.string.hint_empty);
        } else {
            view.setText(value);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_MAIN:
                final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
                return new CursorLoader(getActivity(), uri, null, null, null, null);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_MAIN:
                if(data.moveToFirst()) {
                    populateViews(data);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
