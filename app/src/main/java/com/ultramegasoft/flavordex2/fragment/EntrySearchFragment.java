package com.ultramegasoft.flavordex2.fragment;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import com.ultramegasoft.flavordex2.EntryListActivity;
import com.ultramegasoft.flavordex2.EntrySearchActivity;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.CatListAdapter;
import com.ultramegasoft.flavordex2.widget.DateInputWidget;

import java.util.ArrayList;
import java.util.Date;

/**
 * Dialog to contain the entry filter form.
 *
 * @author Steve Guidetti
 */
public class EntrySearchFragment extends DialogFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Arguments for the Fragment
     */
    public static final String ARG_FILTER_VALUES = "filter_values";
    private static final String ARG_DATE_MIN = "date_min";
    private static final String ARG_DATE_MAX = "date_max";

    /**
     * Keys for the result data Intent
     */
    public static final String EXTRA_FILTERS = "filter_values";
    public static final String EXTRA_SQL_WHERE = "where";
    public static final String EXTRA_SQL_ARGS = "args";

    /**
     * Views from the layout
     */
    private Spinner mSpnCat;
    private EditText mTxtTitle;
    private EditText mTxtMaker;
    private EditText mTxtOrigin;
    private EditText mTxtLocation;
    private DateInputWidget mDateMin;
    private DateInputWidget mDateMax;

    /**
     * The initial selected category ID
     */
    private long mCatId;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_search, null);

        mSpnCat = (Spinner)root.findViewById(R.id.cat);
        mTxtTitle = (EditText)root.findViewById(R.id.title);
        mTxtMaker = (EditText)root.findViewById(R.id.maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.origin);
        mTxtLocation = (EditText)root.findViewById(R.id.location);
        mDateMin = (DateInputWidget)root.findViewById(R.id.date_min);
        mDateMax = (DateInputWidget)root.findViewById(R.id.date_max);

        root.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFilter();
            }
        });

        getLoaderManager().initLoader(0, null, this);

        setupEventHandlers();
        populateFields();

        return root;
    }

    /**
     * Add event handlers to fields.
     */
    private void setupEventHandlers() {
        mDateMin.setListener(new DateInputWidget.OnDateChangeListener() {
            @Override
            public void onDateChanged(@NonNull Date date) {
                final Date maxDate = mDateMax.getDate();
                if(maxDate != null && date.after(maxDate)) {
                    mDateMax.setDate(date);
                }
            }

            @Override
            public void onDateCleared() {
            }
        });

        mDateMax.setListener(new DateInputWidget.OnDateChangeListener() {
            @Override
            public void onDateChanged(@NonNull Date date) {
                final Date minDate = mDateMin.getDate();
                if(minDate != null && date.before(minDate)) {
                    mDateMin.setDate(date);
                }
            }

            @Override
            public void onDateCleared() {
            }
        });
    }

    /**
     * Initialize form fields from Fragment arguments.
     */
    private void populateFields() {
        final Bundle args = getArguments();
        if(args != null) {
            final ContentValues filters = args.getParcelable(ARG_FILTER_VALUES);
            if(filters != null) {
                mCatId = filters.getAsLong(Tables.Entries.CAT_ID);

                mTxtTitle.setText(filters.getAsString(Tables.Entries.TITLE));
                mTxtMaker.setText(filters.getAsString(Tables.Entries.MAKER));
                mTxtOrigin.setText(filters.getAsString(Tables.Entries.ORIGIN));
                mTxtLocation.setText(filters.getAsString(Tables.Entries.LOCATION));

                if(filters.containsKey(ARG_DATE_MIN)) {
                    mDateMin.setDate(new Date(filters.getAsLong(ARG_DATE_MIN)));
                }
                if(filters.containsKey(ARG_DATE_MAX)) {
                    mDateMax.setDate(new Date(filters.getAsLong(ARG_DATE_MAX)));
                }
            }
        }
    }

    /**
     * Parse the form fields and send the results to the Activity.
     */
    private void performFilter() {
        final Intent intent = new Intent();
        parseFields(intent);
        if(getActivity() instanceof EntrySearchActivity) {
            ((EntrySearchActivity)getActivity()).publishResult(intent);
        } else if(getActivity() instanceof EntryListActivity) {
            final ContentValues filters = intent.getParcelableExtra(EXTRA_FILTERS);
            final String where = intent.getStringExtra(EXTRA_SQL_WHERE);
            final String[] whereArgs = intent.getStringArrayExtra(EXTRA_SQL_ARGS);
            ((EntryListActivity)getActivity()).onSearchSubmitted(filters, where, whereArgs);
        }
    }

    /**
     * Parse the form fields and place the data into the Intent.
     *
     * @param data An Intent to hold the data
     */
    private void parseFields(Intent data) {
        final ContentValues filterValues = new ContentValues();
        final StringBuilder where = new StringBuilder();
        final ArrayList<String> argList = new ArrayList<>();

        filterValues.put(Tables.Entries.CAT_ID, mSpnCat.getSelectedItemId());

        if(!TextUtils.isEmpty(mTxtTitle.getText())) {
            filterValues.put(Tables.Entries.TITLE, mTxtTitle.getText().toString());
            where.append(Tables.Entries.TITLE).append(" LIKE ? AND ");
            argList.add("%" + mTxtTitle.getText() + "%");
        }

        if(!TextUtils.isEmpty(mTxtMaker.getText())) {
            filterValues.put(Tables.Entries.MAKER, mTxtMaker.getText().toString());
            where.append(Tables.Entries.MAKER).append(" LIKE ? AND ");
            argList.add("%" + mTxtMaker.getText() + "%");
        }

        if(!TextUtils.isEmpty(mTxtOrigin.getText())) {
            filterValues.put(Tables.Entries.ORIGIN, mTxtOrigin.getText().toString());
            where.append(Tables.Entries.ORIGIN).append(" LIKE ? AND ");
            argList.add("%" + mTxtOrigin.getText() + "%");
        }

        if(!TextUtils.isEmpty(mTxtLocation.getText())) {
            filterValues.put(Tables.Entries.LOCATION, mTxtLocation.getText().toString());
            where.append(Tables.Entries.LOCATION).append(" LIKE ? AND ");
            argList.add("%" + mTxtLocation.getText() + "%");
        }

        final Date minDate = mDateMin.getDate();
        final Date maxDate = mDateMax.getDate();
        if(minDate != null || maxDate != null) {
            if(minDate != null) {
                final long minTime = minDate.getTime();
                filterValues.put(ARG_DATE_MIN, minTime);
                where.append(Tables.Entries.DATE).append(" >= ").append(minTime).append(" AND ");
            }
            if(maxDate != null) {
                final long maxTime = maxDate.getTime();
                filterValues.put(ARG_DATE_MAX, maxTime);
                where.append(Tables.Entries.DATE).append(" < ")
                        .append(maxTime + (24 * 60 * 60 * 1000)).append(" AND ");
            }
        }

        if(where.length() > 5) {
            where.delete(where.length() - 5, where.length());
        }

        data.putExtra(EXTRA_FILTERS, filterValues);
        data.putExtra(EXTRA_SQL_WHERE, where.toString());
        data.putExtra(EXTRA_SQL_ARGS, argList.toArray(new String[argList.size()]));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), Tables.Cats.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final CatListAdapter adapter = new CatListAdapter(getContext(), data,
                android.R.layout.simple_spinner_item,
                android.R.layout.simple_spinner_dropdown_item);
        adapter.setShowAllCats(true);
        mSpnCat.setAdapter(adapter);

        for(int i = 0; i < adapter.getCount(); i++) {
            if(adapter.getItemId(i) == mCatId) {
                mSpnCat.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CatListAdapter)mSpnCat.getAdapter()).swapCursor(null);
    }
}
