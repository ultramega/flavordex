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
package com.ultramegasoft.flavordex2.whiskey;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;

/**
 * Whiskey specific entry form helper.
 *
 * @author Steve Guidetti
 */
class WhiskeyEntryFormHelper extends EntryFormHelper {
    /**
     * The Views for the form fields
     */
    AutoCompleteTextView mTxtType;
    EditText mTxtAge;
    EditText mTxtABV;

    WhiskeyEntryFormHelper(@NonNull Fragment fragment, @NonNull View layoutRoot) {
        super(fragment, layoutRoot);
    }

    @Override
    protected void loadLayout(@NonNull View root) {
        super.loadLayout(root);

        mTxtType = root.findViewById(R.id.entry_type);

        final Context context = mFragment.getContext();
        if(context != null) {
            mTxtType.setAdapter(ArrayAdapter.createFromResource(context,
                    R.array.whiskey_types, android.R.layout.simple_dropdown_item_1line));
        }

        mTxtAge = root.findViewById(R.id.entry_stats_age);
        mTxtABV = root.findViewById(R.id.entry_stats_abv);
    }

    @Override
    public void setExtras(@NonNull LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.setExtras(extras);
        initEditText(mTxtType, extras.get(Tables.Extras.Whiskey.STYLE));
        initEditText(mTxtAge, extras.get(Tables.Extras.Whiskey.STATS_AGE));
        initEditText(mTxtABV, extras.get(Tables.Extras.Whiskey.STATS_ABV));
    }
}
