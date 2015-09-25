package com.ultramegasoft.flavordex2.dialog;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Dialog for showing a date picker.
 *
 * @author Steve Guidetti
 */
public class DateDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    /**
     * Tag to identify the Fragment
     */
    private static final String TAG = "DateDialog";
    /**
     * Arguments for the Fragment
     */
    public static final String ARG_DATE = "date";

    /**
     * Show a date picker dialog.
     *
     * @param fm             The FragmentManager to use
     * @param targetFragment The Fragment to send the results to
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

        return new DatePickerDialog(getContext(),
                android.support.v7.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert, this,
                year, month, day);
    }

    @Override
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
