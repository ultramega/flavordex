package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

import com.ultramegasoft.flavordex2.R;

/**
 * Custom view for allowing custom list items to be checkable
 *
 * @author Steve Guidetti
 */
public class CheckableEntryListItem extends LinearLayout implements Checkable {
    private boolean mChecked;

    public CheckableEntryListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setChecked(boolean checked) {
        mChecked = checked;

        ((CheckBox)findViewById(R.id.checkbox)).setChecked(checked);
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void toggle() {
        setChecked(!mChecked);
    }
}
