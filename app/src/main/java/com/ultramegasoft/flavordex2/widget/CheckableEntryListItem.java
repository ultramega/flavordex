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

import android.content.Context;
import android.support.annotation.Nullable;
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

    public CheckableEntryListItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableEntryListItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("SameParameterValue")
    public void setMultiChoice(boolean multiChoice) {
        mMultiChoice = multiChoice;
    }
}
