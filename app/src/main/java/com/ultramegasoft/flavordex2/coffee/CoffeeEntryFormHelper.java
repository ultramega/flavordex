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

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Coffee specific entry form helper.
 *
 * @author Steve Guidetti
 */
class CoffeeEntryFormHelper extends EntryFormHelper {
    /**
     * The Views for the form fields
     */
    EditText mTxtRoaster;
    EditText mTxtRoastDate;
    EditText mTxtGrind;
    Spinner mSpnBrewMethod;
    EditText mTxtDose;
    EditText mTxtEspMass;
    EditText mTxtWaterMass;
    EditText mTxtTemp;
    EditText mTxtExtTimeM;
    EditText mTxtExtTimeS;
    EditText mTxtTDS;
    EditText mTxtYield;

    /**
     * Table rows that are shown conditionally
     */
    private TableRow mRowEspMass;
    private TableRow mRowWaterMass;

    CoffeeEntryFormHelper(@NonNull Fragment fragment, @NonNull View layoutRoot) {
        super(fragment, layoutRoot);
    }

    @Override
    protected void loadLayout(@NonNull View root) {
        super.loadLayout(root);
        mTxtRoaster = root.findViewById(R.id.entry_roaster);
        mTxtRoastDate = root.findViewById(R.id.entry_roast_date);
        mTxtGrind = root.findViewById(R.id.entry_grind);
        mSpnBrewMethod = root.findViewById(R.id.entry_brew_method);
        mTxtDose = root.findViewById(R.id.entry_stats_dose);
        mTxtEspMass = root.findViewById(R.id.entry_stats_esp_mass);
        mTxtWaterMass = root.findViewById(R.id.entry_stats_water_mass);
        mTxtTemp = root.findViewById(R.id.entry_stats_temp);
        mTxtExtTimeM = root.findViewById(R.id.entry_stats_ext_time_m);
        mTxtExtTimeS = root.findViewById(R.id.entry_stats_ext_time_s);
        mTxtTDS = root.findViewById(R.id.entry_stats_tds);
        mTxtYield = root.findViewById(R.id.entry_stats_yield);

        mRowEspMass = root.findViewById(R.id.esp_mass_row);
        mRowWaterMass = root.findViewById(R.id.water_mass_row);
    }

    @Override
    public void setExtras(@NonNull LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.setExtras(extras);
        initSpinner(mSpnBrewMethod, extras.get(Tables.Extras.Coffee.BREW_METHOD));

        initEditText(mTxtRoaster, extras.get(Tables.Extras.Coffee.ROASTER));
        initEditText(mTxtRoastDate, extras.get(Tables.Extras.Coffee.ROAST_DATE));
        initEditText(mTxtGrind, extras.get(Tables.Extras.Coffee.GRIND));

        initEditText(mTxtDose, extras.get(Tables.Extras.Coffee.STATS_DOSE));
        initEditText(mTxtEspMass, extras.get(Tables.Extras.Coffee.STATS_MASS));
        initEditText(mTxtWaterMass, extras.get(Tables.Extras.Coffee.STATS_MASS));
        initEditText(mTxtTemp, extras.get(Tables.Extras.Coffee.STATS_TEMP));
        initExtractionTime(mTxtExtTimeM, mTxtExtTimeS,
                extras.get(Tables.Extras.Coffee.STATS_EXTIME));
        initEditText(mTxtTDS, extras.get(Tables.Extras.Coffee.STATS_TDS));
        initEditText(mTxtYield, extras.get(Tables.Extras.Coffee.STATS_YIELD));
    }

    /**
     * Set up the extraction time fields.
     *
     * @param min   The EditText for minutes
     * @param sec   The EditText for seconds
     * @param extra The extraction time extra field
     */
    private static void initExtractionTime(@NonNull final EditText min, @NonNull final EditText sec,
                                           @NonNull final ExtraFieldHolder extra) {
        if(extra == null) {
            return;
        }
        if(!TextUtils.isEmpty(extra.value)) {
            final int extTime = Integer.valueOf(extra.value);
            min.setText(String.valueOf(extTime / 60));
            sec.setText(String.format(Locale.US, "%02d", extTime % 60));
        }

        final TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int extTimeM = 0;
                int extTimeS = 0;
                try {
                    extTimeM = Integer.parseInt(min.getText().toString());
                } catch(NumberFormatException ignored) {
                }
                try {
                    extTimeS = Integer.parseInt(sec.getText().toString());
                } catch(NumberFormatException ignored) {
                }

                extra.value = (extTimeM * 60 + extTimeS) + "";
            }
        };
        min.addTextChangedListener(watcher);
        sec.addTextChangedListener(watcher);
    }

    /**
     * Toggle display of the water mass and espresso mass based on whether this entry is an
     * espresso.
     *
     * @param isEspresso Whether this is an espresso
     */
    void setIsEspresso(boolean isEspresso) {
        final ExtraFieldHolder field = getExtras().get(Tables.Extras.Coffee.STATS_MASS);
        if(field != null) {
            if(isEspresso) {
                mRowWaterMass.setVisibility(View.GONE);
                mRowEspMass.setVisibility(View.VISIBLE);
                field.value = mTxtEspMass.getText().toString();
            } else {
                mRowEspMass.setVisibility(View.GONE);
                mRowWaterMass.setVisibility(View.VISIBLE);
                field.value = mTxtWaterMass.getText().toString();
            }
        }
    }
}
