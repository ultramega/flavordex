package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

import com.ultramegasoft.flavordex2.R;

/**
 * Custom View for allowing custom list items to be checkable.
 *
 * @author Steve Guidetti
 */
public class CheckableEntryListItem extends LinearLayout implements Checkable {
    /**
     * Whether this list item is checked
     */
    private boolean mChecked;

    public CheckableEntryListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the checked state of this list item.
     *
     * @param checked Whether to check or uncheck this list item
     */
    public void setChecked(boolean checked) {
        mChecked = checked;

        ((CheckBox)findViewById(R.id.checkbox)).setChecked(checked);
    }

    /**
     * Is this list item checked?
     *
     * @return Whether this list item is checked
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Toggle the checked state of this list item.
     */
    public void toggle() {
        setChecked(!mChecked);
    }
}
