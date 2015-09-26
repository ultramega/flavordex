package com.ultramegasoft.flavordex2.widget;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Custom widget for inputting dates. Shows a button to open a date picker and a button to clear the
 * date if set. The date is displayed between the buttons.
 *
 * @author Steve Guidetti
 */
public class DateInputWidget extends LinearLayout implements DatePickerDialog.OnDateSetListener {
    /**
     * Keys for the saved state
     */
    private static final String STATE_SUPER_STATE = "super_state";
    private static final String STATE_DATE = "date";
    private static final String STATE_DIALOG_OPEN = "dialog_open";
    private static final String STATE_DIALOG_DATE = "dialog_date";

    /**
     * Formatter for dates
     */
    private final SimpleDateFormat mDateFormat;

    /**
     * Views from the layout
     */
    private final TextView mTxtDate;
    private final ImageButton mBtnClear;

    /**
     * The currently set date
     */
    private Date mDate;

    /**
     * The DatePickerDialog
     */
    private DatePickerDialog mDatePickerDialog;

    /**
     * The Date from the DatePickerDialog restored from saved state
     */
    private Date mDatePickerDialogDate;

    /**
     * The current listener for date changes
     */
    private OnDateChangeListener mListener;

    /**
     * Interface for listeners for date changes.
     */
    public interface OnDateChangeListener {
        /**
         * Called when the date is set
         *
         * @param date The new Date
         */
        void onDateChanged(@NonNull Date date);

        /**
         * Called when the date is cleared
         */
        void onDateCleared();
    }

    public DateInputWidget(Context context) {
        this(context, null, 0);
    }

    public DateInputWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DateInputWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(getContext(), R.layout.date_input_widget, this);
        setOrientation(HORIZONTAL);

        mTxtDate = (TextView)findViewById(R.id.diw_date);
        findViewById(R.id.diw_button_set).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker(mDate);
            }
        });
        mBtnClear = (ImageButton)findViewById(R.id.diw_button_clear);
        mBtnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearDate();
            }
        });

        final Resources res = getResources();
        mDateFormat = new SimpleDateFormat(res.getString(R.string.date_format), Locale.US);
    }

    /**
     * Set the listener for date changes.
     *
     * @param listener An OnDateChangeListener
     */
    public void setListener(OnDateChangeListener listener) {
        mListener = listener;
    }

    /**
     * Clear the date.
     */
    public void clearDate() {
        setDate(null);
        if(mListener != null) {
            mListener.onDateCleared();
        }
    }

    /**
     * Set the time.
     *
     * @param timestamp A Unix timestamp with milliseconds
     */
    public void setTime(long timestamp) {
        setDate(new Date(timestamp));
    }

    /**
     * Get the currently set time.
     *
     * @return A Unix timestamp with milliseconds or -1 if not set
     */
    public long getTime() {
        if(mDate != null) {
            return mDate.getTime();
        }
        return -1;
    }

    /**
     * Set the date.
     *
     * @param date A Date
     */
    public void setDate(Date date) {
        mDate = date;
        if(date != null) {
            mTxtDate.setText(mDateFormat.format(date));
            mBtnClear.setVisibility(VISIBLE);
            if(mListener != null) {
                mListener.onDateChanged(date);
            }
        } else {
            mTxtDate.setText(null);
            mBtnClear.setVisibility(GONE);
        }
    }

    /**
     * Get the currently set date.
     *
     * @return A Date or null if not set
     */
    public Date getDate() {
        return mDate;
    }

    /**
     * Open the DatePickerDialog to set the date.
     *
     * @param initDate The initial date to select in the dialog
     */
    private void openDatePicker(Date initDate) {
        final Calendar calendar = Calendar.getInstance();
        if(initDate != null) {
            calendar.setTime(initDate);
        } else {
            calendar.setTimeInMillis(System.currentTimeMillis());
        }
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        final int day = calendar.get(Calendar.DAY_OF_MONTH);

        mDatePickerDialog = new DatePickerDialog(getContext(),
                android.support.v7.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert, this,
                year, month, day);
        mDatePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        setDate(calendar.getTime());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mDatePickerDialogDate != null) {
            openDatePicker(mDatePickerDialogDate);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle outState = new Bundle();
        outState.putParcelable(STATE_SUPER_STATE, super.onSaveInstanceState());
        outState.putSerializable(STATE_DATE, mDate);
        if(mDatePickerDialog != null && mDatePickerDialog.isShowing()) {
            outState.putBoolean(STATE_DIALOG_OPEN, true);
            final DatePicker picker = mDatePickerDialog.getDatePicker();
            final Calendar calendar = Calendar.getInstance();
            calendar.set(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
            outState.putSerializable(STATE_DIALOG_DATE, calendar.getTime());
        }
        return outState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final Bundle inState = (Bundle)state;
        super.onRestoreInstanceState(inState.getParcelable(STATE_SUPER_STATE));
        setDate((Date)inState.getSerializable(STATE_DATE));
        if(inState.getBoolean(STATE_DIALOG_OPEN, false)) {
            mDatePickerDialogDate = (Date)inState.getSerializable(STATE_DIALOG_DATE);
        }
    }
}
