package com.ultramegasoft.flavordex2.coffee;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;

import com.ultramegasoft.flavordex2.AddEntryInfoFragment;
import com.ultramegasoft.flavordex2.R;

/**
 * Fragment for adding details for a new coffee entry.
 *
 * @author Steve Guidetti
 */
public class AddCoffeeInfoFragment extends AddEntryInfoFragment {
    /**
     * The views for the form fields
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        return R.layout.fragment_add_info_coffee;
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
