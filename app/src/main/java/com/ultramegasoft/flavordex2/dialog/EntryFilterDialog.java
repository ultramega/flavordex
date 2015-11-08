package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.DateInputWidget;

import java.util.ArrayList;
import java.util.Date;

/**
 * Dialog to contain the entry filter form.
 *
 * @author Steve Guidetti
 */
public class EntryFilterDialog extends DialogFragment {
    private static final String TAG = "EntryFilterDialog";

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
     * Views from the layout
     */
    private EditText mTxtMaker;
    private EditText mTxtOrigin;
    private EditText mTxtLocation;
    private DateInputWidget mDateMin;
    private DateInputWidget mDateMax;

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

        mTxtMaker = (EditText)root.findViewById(R.id.maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.origin);
        mTxtLocation = (EditText)root.findViewById(R.id.location);
        mDateMin = (DateInputWidget)root.findViewById(R.id.date_min);
        mDateMax = (DateInputWidget)root.findViewById(R.id.date_max);

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
}
