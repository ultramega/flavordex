package com.ultramegasoft.flavordex2.coffee;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;

import com.ultramegasoft.flavordex2.EditInfoFragment;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;

/**
 * Fragment for editing details for a new or existing coffee entry.
 *
 * @author Steve Guidetti
 */
public class EditCoffeeInfoFragment extends EditInfoFragment {
    /**
     * The Views for the form fields
     */
    private EditText mTxtRoaster;
    private EditText mTxtRoastDate;
    private EditText mTxtGrind;
    private Spinner mSpnBrewMethod;
    private EditText mTxtDose;
    private EditText mTxtEspMass;
    private EditText mTxtWaterMass;
    private EditText mTxtTemp;
    private EditText mTxtExtTimeM;
    private EditText mTxtExtTimeS;
    private EditText mTxtTDS;
    private EditText mTxtYield;

    /**
     * Table rows that are shown conditionally
     */
    private TableRow mRowEspMass;
    private TableRow mRowWaterMass;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtRoaster = (EditText)root.findViewById(R.id.entry_roaster);
        mTxtRoastDate = (EditText)root.findViewById(R.id.entry_roast_date);
        mTxtGrind = (EditText)root.findViewById(R.id.entry_grind);
        mSpnBrewMethod = (Spinner)root.findViewById(R.id.entry_brew_method);

        mSpnBrewMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setIsEspresso(position == 4);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mTxtDose = (EditText)root.findViewById(R.id.entry_stats_dose);
        mTxtEspMass = (EditText)root.findViewById(R.id.entry_stats_esp_mass);
        mTxtWaterMass = (EditText)root.findViewById(R.id.entry_stats_water_mass);
        mTxtTemp = (EditText)root.findViewById(R.id.entry_stats_temp);
        mTxtExtTimeM = (EditText)root.findViewById(R.id.entry_stats_ext_time_m);
        mTxtExtTimeS = (EditText)root.findViewById(R.id.entry_stats_ext_time_s);
        mTxtTDS = (EditText)root.findViewById(R.id.entry_stats_tds);
        mTxtYield = (EditText)root.findViewById(R.id.entry_stats_yield);

        mRowEspMass = (TableRow)root.findViewById(R.id.esp_mass_row);
        mRowWaterMass = (TableRow)root.findViewById(R.id.water_mass_row);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info_coffee;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.populateExtras(extras);
        initSpinner(mSpnBrewMethod, extras.get(Tables.Extras.Coffee.BREW_METHOD));

        initEditText(mTxtRoaster, extras.get(Tables.Extras.Coffee.ROASTER));
        initEditText(mTxtRoastDate, extras.get(Tables.Extras.Coffee.ROAST_DATE));
        initEditText(mTxtGrind, extras.get(Tables.Extras.Coffee.GRIND));

        initEditText(mTxtDose, extras.get(Tables.Extras.Coffee.STATS_DOSE));
        initEditText(mTxtEspMass, extras.get(Tables.Extras.Coffee.STATS_MASS));
        initEditText(mTxtWaterMass, extras.get(Tables.Extras.Coffee.STATS_MASS));
        initEditText(mTxtTemp, extras.get(Tables.Extras.Coffee.STATS_TEMP));
        initExtractionTime(mTxtExtTimeM, mTxtExtTimeS, extras.get(Tables.Extras.Coffee.STATS_EXTIME));
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
    private static void initExtractionTime(final EditText min, final EditText sec,
                                           final ExtraFieldHolder extra) {
        if(extra == null) {
            return;
        }
        if(!TextUtils.isEmpty(extra.value)) {
            final int extTime = Integer.valueOf(extra.value);
            min.setText(String.valueOf(extTime / 60));
            sec.setText(String.format("%02d", extTime % 60));
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
    private void setIsEspresso(boolean isEspresso) {
        if(isEspresso) {
            mRowWaterMass.setVisibility(View.GONE);
            mRowEspMass.setVisibility(View.VISIBLE);
        } else {
            mRowEspMass.setVisibility(View.GONE);
            mRowWaterMass.setVisibility(View.VISIBLE);
        }
    }
}
