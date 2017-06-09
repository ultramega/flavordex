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
package com.ultramegasoft.flavordex2.beer;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.SpecialArrayAdapter;

import java.util.LinkedHashMap;

/**
 * Beer specific entry form helper.
 *
 * @author Steve Guidetti
 */
class BeerEntryFormHelper extends EntryFormHelper {
    /**
     * The Views for the form fields
     */
    AutoCompleteTextView mTxtStyle;
    Spinner mSpnServing;
    EditText mTxtIBU;
    EditText mTxtABV;
    EditText mTxtOG;
    EditText mTxtFG;

    BeerEntryFormHelper(@NonNull Fragment fragment, @NonNull View layoutRoot) {
        super(fragment, layoutRoot);
    }

    @Override
    protected void loadLayout(@NonNull View root) {
        super.loadLayout(root);
        mTxtStyle = (AutoCompleteTextView)root.findViewById(R.id.entry_style);
        mTxtStyle.setAdapter(SpecialArrayAdapter.createFromResource(mFragment.getContext(),
                R.array.beer_styles, android.R.layout.simple_dropdown_item_1line));

        mSpnServing = (Spinner)root.findViewById(R.id.entry_serving_type);

        mTxtIBU = (EditText)root.findViewById(R.id.entry_stats_ibu);
        mTxtABV = (EditText)root.findViewById(R.id.entry_stats_abv);
        mTxtOG = (EditText)root.findViewById(R.id.entry_stats_og);
        mTxtFG = (EditText)root.findViewById(R.id.entry_stats_fg);
    }

    @Override
    public void setExtras(@NonNull LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.setExtras(extras);
        initEditText(mTxtStyle, extras.get(Tables.Extras.Beer.STYLE));
        initSpinner(mSpnServing, extras.get(Tables.Extras.Beer.SERVING));
        initEditText(mTxtIBU, extras.get(Tables.Extras.Beer.STATS_IBU));
        initEditText(mTxtABV, extras.get(Tables.Extras.Beer.STATS_ABV));
        initEditText(mTxtOG, extras.get(Tables.Extras.Beer.STATS_OG));
        initEditText(mTxtFG, extras.get(Tables.Extras.Beer.STATS_FG));
    }
}
