package com.ultramegasoft.flavordex2;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.provider.Tables;

/**
 * Fragment to display the main details of a journal entry.
 *
 * @author Steve Guidetti
 */
public class EntryInfoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The database id for this entry
     */
    private long mEntryId;

    private TextView mTxtTitle;
    private RatingBar mRatingBar;

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
        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mTxtTitle = (TextView)getActivity().findViewById(R.id.entry_title);
        mRatingBar = (RatingBar)getActivity().findViewById(R.id.entry_rating);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
        return new CursorLoader(getActivity(), uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToFirst();
        mTxtTitle.setText(data.getString(data.getColumnIndex(Tables.Entries.TITLE)));
        mRatingBar.setRating(data.getFloat(data.getColumnIndex(Tables.Entries.RATING)));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
