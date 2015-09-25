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

    /**
     * Whether this list item is being used in a multiple choice mode ListView
     */
    private boolean mMultiChoice = true;

    public CheckableEntryListItem(Context context) {
        super(context);
    }

    public CheckableEntryListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableEntryListItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        ((CheckBox)findViewById(R.id.checkbox)).setChecked(checked);
        if(!mMultiChoice) {
            setActivated(checked);
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    /**
     * Is this list item is being used in a multiple choice mode ListView?
     *
     * @return Whether this list item is being used in a multiple choice mode ListView
     */
    public boolean isMultiChoice() {
        return mMultiChoice;
    }

    /**
     * Set whether this list item is being used in a multiple choice mode ListView. If true
     * (default), this behaves like a regular Checkable list item. If false, the view will have the
     * activated state applied when checked.
     *
     * @param multiChoice Whether this list item is being used in a multiple choice mode ListView
     */
    public void setMultiChoice(boolean multiChoice) {
        mMultiChoice = multiChoice;
    }
}
