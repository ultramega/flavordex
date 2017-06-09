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
package com.ultramegasoft.flavordex2.coffee;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.ViewInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Coffee specific entry view Fragment.
 *
 * @author Steve Guidetti
 */
public class ViewCoffeeInfoFragment extends ViewInfoFragment {
    /**
     * Views to hold details specific to coffee
     */
    private TextView mTxtRoaster;
    private TextView mTxtRoastDate;
    private TextView mTxtGrind;
    private TextView mTxtBrewMethod;

    private TextView mTxtDose;
    private TextView mTxtMass;
    private TextView mTxtRatio;
    private TextView mTxtTemp;
    private TextView mTxtExtTime;
    private TextView mTxtTDS;
    private TextView mTxtYield;

    private TextView mTxtLabelMass;
    private TextView mTxtLabelRatio;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtRoaster = root.findViewById(R.id.entry_roaster);
        mTxtRoastDate = root.findViewById(R.id.entry_roast_date);
        mTxtGrind = root.findViewById(R.id.entry_grind);
        mTxtBrewMethod = root.findViewById(R.id.entry_brew_method);

        mTxtDose = root.findViewById(R.id.entry_stats_dose);
        mTxtMass = root.findViewById(R.id.entry_stats_mass);
        mTxtRatio = root.findViewById(R.id.entry_stats_ratio);
        mTxtTemp = root.findViewById(R.id.entry_stats_temp);
        mTxtExtTime = root.findViewById(R.id.entry_stats_ext_time);
        mTxtTDS = root.findViewById(R.id.entry_stats_tds);
        mTxtYield = root.findViewById(R.id.entry_stats_yield);

        mTxtLabelMass = root.findViewById(R.id.label_mass);
        mTxtLabelRatio = root.findViewById(R.id.label_ratio);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_view_info_coffee;
    }

    @Override
    protected void populateExtras(@NonNull LinkedHashMap<String, ExtraFieldHolder> data) {
        super.populateExtras(data);
        setViewText(mTxtRoaster, getExtraValue(data.get(Tables.Extras.Coffee.ROASTER)));
        setViewText(mTxtRoastDate, getExtraValue(data.get(Tables.Extras.Coffee.ROAST_DATE)));
        setViewText(mTxtGrind, getExtraValue(data.get(Tables.Extras.Coffee.GRIND)));

        final int brewMethod =
                stringToInt(getExtraValue(data.get(Tables.Extras.Coffee.BREW_METHOD)));
        if(brewMethod > 0) {
            final Resources res = getResources();
            final String[] servingTypes = res.getStringArray(R.array.coffee_brew_methods);
            mTxtBrewMethod.setText(servingTypes[brewMethod]);
            setIsEspresso(brewMethod == 4);
        } else {
            mTxtBrewMethod.setText(R.string.hint_empty);
        }

        setTextWithUnit(mTxtDose, getExtraValue(data.get(Tables.Extras.Coffee.STATS_DOSE)),
                getString(R.string.coffee_hint_grams));
        setTextWithUnit(mTxtMass, getExtraValue(data.get(Tables.Extras.Coffee.STATS_MASS)),
                getString(R.string.coffee_hint_grams));

        final float dose = stringToFloat(getExtraValue(data.get(Tables.Extras.Coffee.STATS_DOSE)));
        final float mass = stringToFloat(getExtraValue(data.get(Tables.Extras.Coffee.STATS_MASS)));
        if(dose > 0 && mass > 0) {
            if(brewMethod == 4) {
                mTxtRatio.setText(String.format(Locale.US, "%.1f%%", dose / mass * 100));
            } else {
                mTxtRatio.setText(String.format(Locale.US, "%.1f", mass / dose));
            }
        } else {
            mTxtRatio.setText(null);
        }

        setTextWithUnit(mTxtTemp, getExtraValue(data.get(Tables.Extras.Coffee.STATS_TEMP)),
                getString(R.string.coffee_hint_degrees));

        final int extTime = stringToInt(getExtraValue(data.get(Tables.Extras.Coffee.STATS_EXTIME)));
        if(extTime > 0) {
            final int extTimeM = extTime / 60;
            final int extTimeS = extTime % 60;
            mTxtExtTime.setText(String.format(Locale.US, "%d:%02d", extTimeM, extTimeS));
        } else {
            mTxtExtTime.setText(null);
        }

        setTextWithUnit(mTxtTDS, getExtraValue(data.get(Tables.Extras.Coffee.STATS_TDS)),
                getString(R.string.coffee_hint_percent));
        setTextWithUnit(mTxtYield, getExtraValue(data.get(Tables.Extras.Coffee.STATS_YIELD)),
                getString(R.string.coffee_hint_percent));
    }

    /**
     * Toggle display of the water mass and espresso mass based on whether this entry is an
     * espresso.
     *
     * @param isEspresso Whether this is an espresso
     */
    private void setIsEspresso(boolean isEspresso) {
        if(isEspresso) {
            mTxtLabelMass.setText(R.string.coffee_label_esp_mass);
            mTxtLabelRatio.setText(R.string.coffee_label_ebf);
        } else {
            mTxtLabelMass.setText(R.string.coffee_label_water_mass);
            mTxtLabelRatio.setText(R.string.coffee_label_cbr);
        }
    }

    /**
     * Set the text for a TextView with a unit of measurement appended.
     *
     * @param view The TextView
     * @param text The main text
     * @param unit The unit text
     */
    private void setTextWithUnit(@NonNull TextView view, @Nullable String text,
                                 @Nullable String unit) {
        if(!TextUtils.isEmpty(text)) {
            view.setText(getString(R.string.coffee_stat, text, unit));
        } else {
            view.setText(null);
        }
    }
}
