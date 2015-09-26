package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

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
public class EntryFilterDialog extends DialogFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Tag to identify the Fragment
     */
    private static final String TAG = "EntryFilterDialog";

    /**
     * Loader IDs
     */
    private static final int LOADER_CATS = 0;

    /**
     * Arguments for the Fragment
     */
    private static final String ARG_FILTER_VALUES = "filter_values";
    private static final String ARG_DATE_MIN = "date_min";
    private static final String ARG_DATE_MAX = "date_max";

    /**
     * Keys for the result data Intent
     */
    public static final String EXTRA_FILTER_VALUES = "filter_values";
    public static final String EXTRA_SQL_WHERE = "where";
    public static final String EXTRA_SQL_ARGS = "args";
    public static final String EXTRA_FIELDS_LIST = "fields_list";

    /**
     * Keys for the saved state
     */
    private static final String STATE_CAT = "cat";

    /**
     * Views from the layout
     */
    private Spinner mSpinnerCat;
    private EditText mTxtMaker;
    private EditText mTxtOrigin;
    private EditText mTxtLocation;
    private DateInputWidget mDateMin;
    private DateInputWidget mDateMax;

    /**
     * The currently selected category ID
     */
    private long mCatId;

    /**
     * Show the filter dialog.
     *
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to send results to
     * @param requestCode The request code
     * @param filters     Initial values for the form fields
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode,
                                  ContentValues filters) {
        final EntryFilterDialog fragment = new EntryFilterDialog();
        fragment.setTargetFragment(target, requestCode);
        if(filters != null) {
            final Bundle args = new Bundle();
            args.putParcelable(ARG_FILTER_VALUES, filters);
            fragment.setArguments(args);
        }
        fragment.show(fm, TAG);

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            mCatId = savedInstanceState.getLong(STATE_CAT, 0);
        }

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_filter)
                .setIcon(R.drawable.ic_filter_list)
                .setView(getLayout())
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        performFilter();
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    /**
     * Get the layout for the filter form.
     *
     * @return The layout
     */
    @SuppressLint("InflateParams")
    private View getLayout() {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View root = inflater.inflate(R.layout.dialog_filter_list, null);

        mSpinnerCat = (Spinner)root.findViewById(R.id.entry_cat);
        mTxtMaker = (EditText)root.findViewById(R.id.maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.origin);
        mTxtLocation = (EditText)root.findViewById(R.id.location);
        mDateMin = (DateInputWidget)root.findViewById(R.id.date_min);
        mDateMax = (DateInputWidget)root.findViewById(R.id.date_max);

        setupEventHandlers();
        populateFields();
        getLoaderManager().initLoader(LOADER_CATS, null, this);

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_CAT, mCatId);
    }

    /**
     * Add event handlers to fields.
     */
    private void setupEventHandlers() {
        mSpinnerCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCatId = id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCatId = 0;
            }
        });

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
                if(mCatId == 0 && filters.containsKey(Tables.Entries.CAT_ID)) {
                    mCatId = filters.getAsInteger(Tables.Entries.CAT_ID);
                }
                mTxtMaker.setText(filters.getAsString(Tables.Entries.MAKER));
                mTxtOrigin.setText(filters.getAsString(Tables.Entries.ORIGIN));
                mTxtLocation.setText(filters.getAsString(Tables.Entries.LOCATION));
                mDateMin.setTime(filters.getAsLong(ARG_DATE_MIN));
                mDateMax.setTime(filters.getAsLong(ARG_DATE_MAX));
            }
        }
    }

    /**
     * Send the results to the target Fragment.
     */
    private void performFilter() {
        final Fragment fragment = getTargetFragment();
        if(fragment != null) {
            final Intent data = new Intent();
            parseFields(data);

            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
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
        final StringBuilder fieldsList = new StringBuilder();

        if(mSpinnerCat.getSelectedItemPosition() > 0) {
            filterValues.put(Tables.Entries.CAT_ID, mSpinnerCat.getSelectedItemId());
            filterValues.put(Tables.Entries.CAT, mSpinnerCat.getSelectedItem().toString());
            where.append(Tables.Entries.CAT_ID).append(" = ")
                    .append(mSpinnerCat.getSelectedItemId()).append(" AND ");
            fieldsList.append(getString(R.string.filter_entry_cat)).append(", ");
        }

        if(!TextUtils.isEmpty(mTxtMaker.getText())) {
            filterValues.put(Tables.Entries.MAKER, mTxtMaker.getText().toString());
            where.append(Tables.Entries.MAKER).append(" LIKE ? AND ");
            argList.add("%" + mTxtMaker.getText() + "%");
            fieldsList.append(getString(R.string.filter_maker)).append(", ");
        }

        if(!TextUtils.isEmpty(mTxtOrigin.getText())) {
            filterValues.put(Tables.Entries.ORIGIN, mTxtOrigin.getText().toString());
            where.append(Tables.Entries.ORIGIN).append(" LIKE ? AND ");
            argList.add("%" + mTxtOrigin.getText() + "%");
            fieldsList.append(getString(R.string.filter_origin)).append(", ");
        }

        if(!TextUtils.isEmpty(mTxtLocation.getText())) {
            filterValues.put(Tables.Entries.LOCATION, mTxtLocation.getText().toString());
            where.append(Tables.Entries.LOCATION).append(" LIKE ? AND ");
            argList.add("%" + mTxtLocation.getText() + "%");
            fieldsList.append(getString(R.string.filter_location)).append(", ");
        }

        final long minDate = mDateMin.getTime();
        final long maxDate = mDateMax.getTime();
        if(minDate != -1 || maxDate != -1) {
            if(minDate != -1) {
                filterValues.put(ARG_DATE_MIN, minDate);
                where.append(Tables.Entries.DATE).append(" >= ").append(minDate).append(" AND ");
            }
            if(maxDate != -1) {
                filterValues.put(ARG_DATE_MAX, maxDate);
                where.append(Tables.Entries.DATE).append(" < ")
                        .append(maxDate + (24 * 60 * 60 * 1000)).append(" AND ");
            }
            fieldsList.append(getString(R.string.filter_date)).append(", ");
        }

        if(where.length() > 5) {
            where.delete(where.length() - 5, where.length());
        }

        if(fieldsList.length() > 2) {
            fieldsList.delete(fieldsList.length() - 2, fieldsList.length());
        }

        data.putExtra(EXTRA_FILTER_VALUES, filterValues);
        data.putExtra(EXTRA_SQL_WHERE, where.toString());
        data.putExtra(EXTRA_SQL_ARGS, argList.toArray(new String[argList.size()]));
        data.putExtra(EXTRA_FIELDS_LIST, fieldsList.toString());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), Tables.Cats.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final CatListAdapter adapter = new CatSpinnerAdapter(data);
        mSpinnerCat.setAdapter(adapter);
        mSpinnerCat.setSelection(adapter.getItemIndex(mCatId));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Adapter for the category Spinner.
     */
    private class CatSpinnerAdapter extends CatListAdapter {
        /**
         * @param cursor The Cursor
         */
        public CatSpinnerAdapter(Cursor cursor) {
            super(getContext(), cursor, android.R.layout.simple_dropdown_item_1line,
                    android.R.id.text1);
        }

        @Override
        protected void readCursor(Cursor cursor, ArrayList<Category> cats) {
            super.readCursor(cursor, cats);
            int count = 0;
            for(Category cat : cats) {
                count += cat.numEntries;
            }
            cats.add(0, new Category(getContext(), 0, getString(R.string.cat_any), false, count));
        }
    }
}
