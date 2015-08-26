package com.ultramegasoft.flavordex2.dialog;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryTypeAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog to contain the entry filter form.
 *
 * @author Steve Guidetti
 */
public class EntryFilterDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Tag to identify the fragment
     */
    private static final String TAG = "EntryFilterDialog";

    /**
     * Request codes
     */
    private static final int REQUEST_SET_DATE_MIN = 100;
    private static final int REQUEST_SET_DATE_MAX = 200;

    /**
     * Loader ids
     */
    private static final int LOADER_TYPES = 0;

    /**
     * Arguments for the fragment
     */
    public static final String ARG_FILTER_VALUES = "filter_values";

    /**
     * Keys for the result data intent
     */
    public static final String EXTRA_FILTER_VALUES = "filter_values";
    public static final String EXTRA_SQL_WHERE = "where";
    public static final String EXTRA_SQL_ARGS = "args";
    public static final String EXTRA_FIELDS_LIST = "fields_list";

    /**
     * Keys for the saved state
     */
    public static final String STATE_DATE_MIN = "date_min";
    public static final String STATE_DATE_MAX = "date_max";
    public static final String STATE_TYPE = "type";

    /**
     * Views from the layout
     */
    private Spinner mSpinnerType;
    private EditText mTxtMaker;
    private EditText mTxtOrigin;
    private EditText mTxtLocation;
    private Button mBtnDateMin;
    private Button mBtnDateMax;

    /**
     * The currently selected type id
     */
    private long mTypeId;

    /**
     * Minimum and maximum timestamps
     */
    private Long mDateMin;
    private Long mDateMax;

    /**
     * Formatter for dates
     */
    private SimpleDateFormat mDateFormat;

    /**
     * Show the filter dialog.
     *
     * @param fm          The FragmentManager to use
     * @param target      The fragment to send results to
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
        mDateFormat = new SimpleDateFormat(getString(R.string.date_format), Locale.US);

        if(savedInstanceState != null) {
            mTypeId = savedInstanceState.getLong(STATE_TYPE, 0);
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.title_filter)
                .setIcon(R.drawable.ic_filter_list)
                .setView(getLayout(savedInstanceState))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        performFilter();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    /**
     * Get the layout for the filter form.
     *
     * @param savedInstanceState The last saved state for the fragment
     * @return The layout
     */
    private View getLayout(Bundle savedInstanceState) {
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View root = inflater.inflate(R.layout.dialog_filter_list, null);

        mSpinnerType = (Spinner)root.findViewById(R.id.entry_type);
        mTxtMaker = (EditText)root.findViewById(R.id.maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.origin);
        mTxtLocation = (EditText)root.findViewById(R.id.location);

        mBtnDateMin = (Button)root.findViewById(R.id.button_date_min);
        mBtnDateMax = (Button)root.findViewById(R.id.button_date_max);

        setupEventHandlers(root);
        populateFields();
        getLoaderManager().initLoader(LOADER_TYPES, null, this);

        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(STATE_DATE_MIN)) {
                mDateMin = savedInstanceState.getLong(STATE_DATE_MIN);
            }
            if(savedInstanceState.containsKey(STATE_DATE_MAX)) {
                mDateMax = savedInstanceState.getLong(STATE_DATE_MAX);
            }
        }

        if(mDateMin != null) {
            mBtnDateMin.setText(mDateFormat.format(new Date(mDateMin)));
        }
        if(mDateMax != null) {
            mBtnDateMax.setText(mDateFormat.format(new Date(mDateMax)));
        }

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mDateMin != null) {
            outState.putLong(STATE_DATE_MIN, mDateMin);
        }
        if(mDateMax != null) {
            outState.putLong(STATE_DATE_MAX, mDateMax);
        }
        outState.putLong(STATE_TYPE, mTypeId);
    }

    /**
     * Add event handlers to fields.
     *
     * @param root The layout
     */
    private void setupEventHandlers(View root) {
        mSpinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mTypeId = id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mTypeId = 0;
            }
        });

        mBtnDateMin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DateDialog.showDialog(getFragmentManager(), EntryFilterDialog.this,
                        REQUEST_SET_DATE_MIN, mDateMin);
            }
        });
        mBtnDateMax.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DateDialog.showDialog(getFragmentManager(), EntryFilterDialog.this,
                        REQUEST_SET_DATE_MAX, mDateMax);
            }
        });

        final Button btnClearDateMin = (Button)root.findViewById(R.id.button_date_min_clear);
        btnClearDateMin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDateMin = null;
                mBtnDateMin.setText(null);
            }
        });
        final Button btnClearDateMax = (Button)root.findViewById(R.id.button_date_max_clear);
        btnClearDateMax.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDateMax = null;
                mBtnDateMax.setText(null);
            }
        });
    }

    /**
     * Initialize form fields from fragment arguments.
     */
    private void populateFields() {
        final Bundle args = getArguments();
        if(args != null) {
            final ContentValues filters = args.getParcelable(ARG_FILTER_VALUES);
            if(filters != null) {
                if(mTypeId == 0 && filters.containsKey(Tables.Entries.TYPE_ID)) {
                    mTypeId = filters.getAsInteger(Tables.Entries.TYPE_ID);
                }
                mTxtMaker.setText(filters.getAsString(Tables.Entries.MAKER));
                mTxtOrigin.setText(filters.getAsString(Tables.Entries.ORIGIN));
                mTxtLocation.setText(filters.getAsString(Tables.Entries.LOCATION));

                mDateMin = filters.getAsLong(STATE_DATE_MIN);
                mDateMax = filters.getAsLong(STATE_DATE_MAX);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK && data != null) {
            final long date = data.getLongExtra(DateDialog.ARG_DATE, 0);
            final String dateString = mDateFormat.format(new Date(date));
            if(requestCode == REQUEST_SET_DATE_MIN) {
                mDateMin = date;
                mBtnDateMin.setText(dateString);
                if(mDateMax != null && date > mDateMax) {
                    mDateMax = date;
                    mBtnDateMax.setText(dateString);
                }
            } else if(requestCode == REQUEST_SET_DATE_MAX) {
                mDateMax = date;
                mBtnDateMax.setText(dateString);
                if(mDateMin != null && date < mDateMin) {
                    mDateMin = date;
                    mBtnDateMin.setText(dateString);
                }
            }
        }
    }

    /**
     * Send the results to the target fragment.
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
     * Parse the form fields and place the data into the intent.
     *
     * @param data An intent to hold the data
     */
    private void parseFields(Intent data) {
        final ContentValues filterValues = new ContentValues();
        final StringBuilder where = new StringBuilder();
        final ArrayList<String> argList = new ArrayList<>();
        final StringBuilder fieldsList = new StringBuilder();

        if(mSpinnerType.getSelectedItemPosition() > 0) {
            filterValues.put(Tables.Entries.TYPE_ID, mSpinnerType.getSelectedItemId());
            filterValues.put(Tables.Entries.TYPE, mSpinnerType.getSelectedItem().toString());
            where.append(Tables.Entries.TYPE_ID).append(" = ")
                    .append(mSpinnerType.getSelectedItemId()).append(" AND ");
            fieldsList.append(getString(R.string.hint_entry_type)).append(", ");
        }

        if(!TextUtils.isEmpty(mTxtMaker.getText())) {
            filterValues.put(Tables.Entries.MAKER, mTxtMaker.getText().toString());
            where.append(Tables.Entries.MAKER).append(" LIKE ? AND ");
            argList.add("%" + mTxtMaker.getText() + "%");
            fieldsList.append(getString(R.string.hint_maker)).append(", ");
        }

        if(!TextUtils.isEmpty(mTxtOrigin.getText())) {
            filterValues.put(Tables.Entries.ORIGIN, mTxtOrigin.getText().toString());
            where.append(Tables.Entries.ORIGIN).append(" LIKE ? AND ");
            argList.add("%" + mTxtOrigin.getText() + "%");
            fieldsList.append(getString(R.string.hint_origin)).append(", ");
        }

        if(!TextUtils.isEmpty(mTxtLocation.getText())) {
            filterValues.put(Tables.Entries.LOCATION, mTxtLocation.getText().toString());
            where.append(Tables.Entries.LOCATION).append(" LIKE ? AND ");
            argList.add("%" + mTxtLocation.getText() + "%");
            fieldsList.append(getString(R.string.hint_location_filter)).append(", ");
        }

        if(mDateMin != null || mDateMax != null) {
            if(mDateMin != null) {
                filterValues.put(STATE_DATE_MIN, mDateMin);
                where.append(Tables.Entries.DATE).append(" >= ").append(mDateMin).append(" AND ");
            }
            if(mDateMax != null) {
                filterValues.put(STATE_DATE_MAX, mDateMax);
                where.append(Tables.Entries.DATE).append(" < ")
                        .append(mDateMax + (24 * 60 * 60 * 1000)).append(" AND ");
            }
            fieldsList.append(getString(R.string.label_date)).append(", ");
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
        return new CursorLoader(getActivity(), Tables.Types.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final EntryTypeAdapter adapter = new EntryTypeAdapter(getActivity(), data,
                android.R.layout.simple_dropdown_item_1line, android.R.id.text1);
        adapter.setHeader(R.string.type_any);
        mSpinnerType.setAdapter(adapter);
        mSpinnerType.setSelection(adapter.getItemIndex(mTypeId));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Dialog for showing a date picker
     */
    public static class DateDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        /**
         * Tag to identify the fragment
         */
        private static final String TAG = "DateDialog";
        /**
         * Arguments for the fragment
         */
        public static final String ARG_DATE = "date";

        /**
         * Show a date picker dialog.
         *
         * @param fm             The FragmentManager to use
         * @param targetFragment The fragment to send the results to
         * @param requestCode    The request code
         * @param initTime       The initial value of the date picker
         */
        public static void showDialog(FragmentManager fm, Fragment targetFragment, int requestCode,
                                      Long initTime) {
            final DialogFragment fragment = new DateDialog();
            fragment.setTargetFragment(targetFragment, requestCode);

            if(initTime != null) {
                final Bundle args = new Bundle();
                args.putLong(ARG_DATE, initTime);
                fragment.setArguments(args);
            }

            fragment.show(fm, TAG);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            long initTime = System.currentTimeMillis();
            final Bundle args = getArguments();
            if(args != null) {
                initTime = args.getLong(ARG_DATE, initTime);
            }

            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(initTime);
            final int year = calendar.get(Calendar.YEAR);
            final int month = calendar.get(Calendar.MONTH);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            final Fragment fragment = getTargetFragment();
            if(fragment != null) {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day, 0, 0, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                final Intent data = new Intent();
                data.putExtra(ARG_DATE, calendar.getTimeInMillis());

                fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
            }
        }
    }
}
