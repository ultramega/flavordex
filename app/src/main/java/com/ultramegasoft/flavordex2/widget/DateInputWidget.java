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
package com.ultramegasoft.flavordex2.widget;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

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
public class DateInputWidget extends LinearLayout
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    /**
     * Keys for the saved state
     */
    private static final String STATE_SUPER_STATE = "super_state";
    private static final String STATE_DATE = "date";
    private static final String STATE_DIALOG_DATE = "dialog_date";
    private static final String STATE_DIALOG_TIME = "dialog_time";

    /**
     * Whether clearing of the date is allowed
     */
    private boolean mAllowClear;

    /**
     * Formatter for dates
     */
    private SimpleDateFormat mDateFormat;

    /**
     * Formatter for times
     */
    private SimpleDateFormat mTimeFormat;

    /**
     * Views from the layout
     */
    private final TextView mTxtDate;
    private final LinearLayout mLayoutTime;
    private final TextView mTxtTime;
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
     * Date constraints
     */
    private Date mMinDate;
    private Date mMaxDate;

    /**
     * The TimePickerDialog
     */
    private TimePickerDialog mTimePickerDialog;

    /**
     * The Date from the DatePickerDialog restored from saved state
     */
    private Date mDatePickerDialogDate;

    /**
     * The Date from the TimePickerDialog restored from saved state
     */
    private Date mTimePickerDialogDate;

    /**
     * The current listener for date changes
     */
    private OnDateChangeListener mListener;

    /**
     * Reusable Calendar object
     */
    private final Calendar mCalendar = Calendar.getInstance();

    /**
     * Interface for listeners for date changes.
     */
    @SuppressWarnings("EmptyMethod")
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

        inflate(getContext(), R.layout.widget_date_input, this);
        setOrientation(HORIZONTAL);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        mTxtDate = (TextView)findViewById(R.id.diw_date);
        findViewById(R.id.diw_button_set_date).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker(mDate);
            }
        });

        mLayoutTime = (LinearLayout)findViewById(R.id.diw_time_layout);
        mTxtTime = (TextView)findViewById(R.id.diw_time);
        findViewById(R.id.diw_button_set_time).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openTimePicker(mDate);
            }
        });

        mBtnClear = (ImageButton)findViewById(R.id.diw_button_clear);
        mBtnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setDate(null);
            }
        });

        applyAttrs(attrs);
    }

    /**
     * Apply the XML attributes.
     *
     * @param attrs The AttributeSet from the constructor
     */
    private void applyAttrs(AttributeSet attrs) {
        final Resources res = getResources();
        final TypedArray a =
                getContext().obtainStyledAttributes(attrs, R.styleable.DateInputWidget);
        final String dateFormat =
                res.getString(a.getResourceId(R.styleable.DateInputWidget_dateFormat,
                        R.string.diw_date_format));
        final String timeFormat =
                res.getString(a.getResourceId(R.styleable.DateInputWidget_timeFormat,
                        R.string.diw_time_format));
        setShowTime(a.getBoolean(R.styleable.DateInputWidget_showTime, false));
        setAllowClear(a.getBoolean(R.styleable.DateInputWidget_allowClear, false));
        a.recycle();

        mDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
        mTimeFormat = new SimpleDateFormat(timeFormat, Locale.US);
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
     * Set the date.
     *
     * @param date A Date
     */
    public void setDate(Date date) {
        if(date != null) {
            mDate = (Date)date.clone();
            mTxtDate.setText(mDateFormat.format(date));
            mTxtTime.setText(mTimeFormat.format(date));
            if(mAllowClear) {
                mBtnClear.setVisibility(VISIBLE);
            }
            if(mListener != null) {
                mListener.onDateChanged(date);
            }
        } else {
            mDate = null;
            mTxtDate.setText(null);
            mTxtTime.setText(null);
            mBtnClear.setVisibility(GONE);
            if(mListener != null) {
                mListener.onDateCleared();
            }
        }
    }

    /**
     * Get the currently set date.
     *
     * @return A Date or null if not set
     */
    public Date getDate() {
        if(mDate != null) {
            return (Date)mDate.clone();
        }
        return null;
    }

    /**
     * Enable or disable the time display.
     *
     * @param showTime Whether to show the time
     */
    @SuppressWarnings("WeakerAccess")
    public void setShowTime(boolean showTime) {
        mLayoutTime.setVisibility(showTime ? VISIBLE : GONE);
    }

    /**
     * Is the time display enabled?
     *
     * @return Whether the time display is showing
     */
    public boolean getShowTime() {
        return mLayoutTime.getVisibility() == VISIBLE;
    }

    /**
     * Enable or disable the clear button.
     *
     * @param allowClear Whether to allow clearing of the date
     */
    @SuppressWarnings("WeakerAccess")
    public void setAllowClear(boolean allowClear) {
        mAllowClear = allowClear;
        final boolean showClear = allowClear && mDate != null;
        mBtnClear.setVisibility(showClear ? VISIBLE : GONE);
    }

    /**
     * Is date clearing allowed?
     *
     * @return Whether the clear button is enabled
     */
    public boolean getAllowClear() {
        return mAllowClear;
    }

    /**
     * Set the minimum date allowed in the date picker dialog.
     *
     * @param minDate The minimum date the dialog should allow
     */
    public void setMinDate(Date minDate) {
        if(minDate != null) {
            mMinDate = (Date)minDate.clone();
        } else {
            mMinDate = null;
            mDatePickerDialog = null;
        }
    }

    /**
     * Get the minimum date allowed in the date picker dialog.
     *
     * @return The minimum date allowed in the date picker dialog
     */
    public Date getMinDate() {
        if(mMinDate != null) {
            return (Date)mMinDate.clone();
        }
        return null;
    }

    /**
     * Set the maximum date allowed in the date picker dialog.
     *
     * @param maxDate The maximum date the dialog should allow
     */
    public void setMaxDate(Date maxDate) {
        if(maxDate != null) {
            mMaxDate = (Date)maxDate.clone();
        } else {
            mMaxDate = null;
            mDatePickerDialog = null;
        }
    }

    /**
     * Get the maximum date allowed in the date picker dialog.
     *
     * @return The maximum date allowed in the date picker dialog
     */
    public Date getMaxDate() {
        if(mMaxDate != null) {
            return (Date)mMaxDate.clone();
        }
        return null;
    }

    /**
     * Open the DatePickerDialog to set the date.
     *
     * @param initDate The initial date to select in the dialog
     */
    private void openDatePicker(Date initDate) {
        if(initDate != null) {
            mCalendar.setTime(initDate);
        } else {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }
        final int year = mCalendar.get(Calendar.YEAR);
        final int month = mCalendar.get(Calendar.MONTH);
        final int day = mCalendar.get(Calendar.DAY_OF_MONTH);

        if(mDatePickerDialog == null) {
            mDatePickerDialog = new DatePickerDialog(getContext(), this, year, month, day);
        } else {
            mDatePickerDialog.updateDate(year, month, day);
        }

        if(mMinDate != null) {
            mDatePickerDialog.getDatePicker().setMinDate(mMinDate.getTime());
        }
        if(mMaxDate != null) {
            mDatePickerDialog.getDatePicker().setMaxDate(mMaxDate.getTime());
        }

        mDatePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mCalendar.clear();
        if(mDate != null) {
            mCalendar.setTime(mDate);
        }
        mCalendar.set(year, monthOfYear, dayOfMonth);
        mCalendar.set(Calendar.MILLISECOND, 0);
        setDate(mCalendar.getTime());
    }

    /**
     * Open the TimePickerDialog to set the time.
     *
     * @param initDate The initial date to select in the dialog
     */
    private void openTimePicker(Date initDate) {
        if(initDate != null) {
            mCalendar.setTime(initDate);
        } else {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }
        final int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
        final int minute = mCalendar.get(Calendar.MINUTE);

        if(mTimePickerDialog == null) {
            mTimePickerDialog = new TimePickerDialog(getContext(), this, hour, minute, false) {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    super.onTimeChanged(view, hourOfDay, minute);
                    onDialogTimeChange(hourOfDay, minute);
                }
            };
        } else {
            mTimePickerDialog.updateTime(hour, minute);
        }
        mTimePickerDialog.show();
    }

    /**
     * Track changes to the TimePicker in the TimePickerDialog since we can't access it directly.
     *
     * @param hourOfDay The hour
     * @param minute    The minute
     */
    private void onDialogTimeChange(int hourOfDay, int minute) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mCalendar.set(Calendar.MINUTE, minute);
        mTimePickerDialogDate = mCalendar.getTime();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if(mDate != null) {
            mCalendar.setTimeInMillis(mDate.getTime());
        } else {
            mCalendar.clear();
        }
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mCalendar.set(Calendar.MINUTE, minute);
        mCalendar.set(Calendar.MILLISECOND, 0);
        setDate(mCalendar.getTime());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mDatePickerDialogDate != null) {
            openDatePicker(mDatePickerDialogDate);
        } else if(mTimePickerDialogDate != null) {
            openTimePicker(mTimePickerDialogDate);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle outState = new Bundle();
        outState.putParcelable(STATE_SUPER_STATE, super.onSaveInstanceState());
        outState.putSerializable(STATE_DATE, mDate);
        if(mDatePickerDialog != null && mDatePickerDialog.isShowing()) {
            final DatePicker picker = mDatePickerDialog.getDatePicker();
            mCalendar.set(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
            outState.putSerializable(STATE_DIALOG_DATE, mCalendar.getTime());
        }
        if(mTimePickerDialog != null && mTimePickerDialog.isShowing()) {
            outState.putSerializable(STATE_DIALOG_TIME, mTimePickerDialogDate);
        }
        return outState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final Bundle inState = (Bundle)state;
        super.onRestoreInstanceState(inState.getParcelable(STATE_SUPER_STATE));
        setDate((Date)inState.getSerializable(STATE_DATE));
        mDatePickerDialogDate = (Date)inState.getSerializable(STATE_DIALOG_DATE);
        mTimePickerDialogDate = (Date)inState.getSerializable(STATE_DIALOG_TIME);
    }
}
