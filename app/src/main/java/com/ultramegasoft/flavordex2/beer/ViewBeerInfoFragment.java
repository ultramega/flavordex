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

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.ViewInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;

/**
 * Beer specific entry view Fragment.
 *
 * @author Steve Guidetti
 */
public class ViewBeerInfoFragment extends ViewInfoFragment {
    /**
     * Views to hold details specific to beer
     */
    private TextView mTxtStyle;
    private TextView mTxtServingType;

    private TextView mTxtIBU;
    private TextView mTxtABV;
    private TextView mTxtOG;
    private TextView mTxtFG;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtStyle = root.findViewById(R.id.entry_style);

        mTxtServingType = root.findViewById(R.id.entry_serving_type);

        mTxtIBU = root.findViewById(R.id.entry_stats_ibu);
        mTxtABV = root.findViewById(R.id.entry_stats_abv);
        mTxtOG = root.findViewById(R.id.entry_stats_og);
        mTxtFG = root.findViewById(R.id.entry_stats_fg);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_view_info_beer;
    }

    @Override
    protected void populateExtras(@NonNull LinkedHashMap<String, ExtraFieldHolder> data) {
        super.populateExtras(data);
        setViewText(mTxtStyle, getExtraValue(data.get(Tables.Extras.Beer.STYLE)));

        final int servingType = stringToInt(getExtraValue(data.get(Tables.Extras.Beer.SERVING)));
        if(servingType > 0) {
            final Resources res = getResources();
            final String[] servingTypes = res.getStringArray(R.array.beer_serving_types);
            mTxtServingType.setText(servingTypes[servingType]);
        } else {
            mTxtServingType.setText(R.string.hint_empty);
        }

        mTxtIBU.setText(getExtraValue(data.get(Tables.Extras.Beer.STATS_IBU)));
        mTxtABV.setText(getExtraValue(data.get(Tables.Extras.Beer.STATS_ABV)));
        mTxtOG.setText(getExtraValue(data.get(Tables.Extras.Beer.STATS_OG)));
        mTxtFG.setText(getExtraValue(data.get(Tables.Extras.Beer.STATS_FG)));
    }
}
