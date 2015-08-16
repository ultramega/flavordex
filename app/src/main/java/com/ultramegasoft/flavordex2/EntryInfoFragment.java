package com.ultramegasoft.flavordex2;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TableRow;
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
    private static final int LOADER_EXTRAS = 1;

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

    private TableLayout mTableExtras;

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
        getLoaderManager().initLoader(LOADER_EXTRAS, null, this);
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

        mTableExtras = (TableLayout)rootView.findViewById(R.id.entry_extras);

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
     * Populates the table of extra fields.
     *
     * @param data The cursor containing the data
     */
    private void populateExtras(Cursor data) {
        if(data.moveToFirst()) {
            TableRow tableRow;
            TextView textView;
            View divider;

            final int padding = getPixelValue(TypedValue.COMPLEX_UNIT_DIP, 4);

            while(!data.isLast()) {
                data.moveToNext();

                tableRow = new TableRow(getActivity());

                textView = new TextView(getActivity());
                textView.setPadding(padding, 0, padding, 0);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                textView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                textView.setText(data.getString(data.getColumnIndex(Tables.Extras.NAME)) + ": ");
                tableRow.addView(textView);

                textView = new TextView(getActivity());
                textView.setPadding(padding, 0, padding, 0);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                textView.setTextIsSelectable(true);
                textView.setText(data.getString(data.getColumnIndex(Tables.EntriesExtras.VALUE)));
                tableRow.addView(textView);

                divider = new View(getActivity());
                divider.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getPixelValue(TypedValue.COMPLEX_UNIT_DIP, 1)));
                divider.setBackgroundResource(android.R.drawable.divider_horizontal_dark);

                mTableExtras.addView(divider);
                mTableExtras.addView(tableRow);
            }

            mTableExtras.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Convert a typed value to pixels.
     *
     * @param fromType  One of the unit constants in TypedValue
     * @param fromValue The value to convert
     * @return The pixel equivalent of the value as an integer
     */
    private int getPixelValue(int fromType, int fromValue) {
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int)TypedValue.applyDimension(fromType, fromValue, metrics);
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
        Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
        switch(id) {
            case LOADER_MAIN:
                return new CursorLoader(getActivity(), uri, null, null, null, null);
            case LOADER_EXTRAS:
                uri = Uri.withAppendedPath(uri, "/extras");
                return new CursorLoader(getActivity(), uri, null, null, null,
                        Tables.Extras._ID + " ASC");
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final int id = loader.getId();
        switch(id) {
            case LOADER_MAIN:
                if(data.moveToFirst()) {
                    populateViews(data);
                }
                break;
            case LOADER_EXTRAS:
                populateExtras(data);
                getLoaderManager().destroyLoader(LOADER_EXTRAS);
        }

        getLoaderManager().destroyLoader(id);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
