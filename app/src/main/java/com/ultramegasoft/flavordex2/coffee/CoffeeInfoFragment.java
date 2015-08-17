package com.ultramegasoft.flavordex2.coffee;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.EntryInfoFragment;
import com.ultramegasoft.flavordex2.R;

import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Coffee specific entry view fragment.
 *
 * @author Steve Guidetti
 */
public class CoffeeInfoFragment extends EntryInfoFragment {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtRoaster = (TextView)root.findViewById(R.id.entry_roaster);
        mTxtRoastDate = (TextView)root.findViewById(R.id.entry_roast_date);
        mTxtGrind = (TextView)root.findViewById(R.id.entry_grind);
        mTxtBrewMethod = (TextView)root.findViewById(R.id.entry_brew_method);

        mTxtDose = (TextView)root.findViewById(R.id.entry_stats_dose);
        mTxtMass = (TextView)root.findViewById(R.id.entry_stats_mass);
        mTxtRatio = (TextView)root.findViewById(R.id.entry_stats_ratio);
        mTxtTemp = (TextView)root.findViewById(R.id.entry_stats_temp);
        mTxtExtTime = (TextView)root.findViewById(R.id.entry_stats_ext_time);
        mTxtTDS = (TextView)root.findViewById(R.id.entry_stats_tds);
        mTxtYield = (TextView)root.findViewById(R.id.entry_stats_yield);

        mTxtLabelMass = (TextView)root.findViewById(R.id.label_mass);
        mTxtLabelRatio = (TextView)root.findViewById(R.id.label_ratio);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_entry_info_coffee;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, String> data) {
        setViewText(mTxtRoaster, data.get("_roaster"));
        setViewText(mTxtRoastDate, data.get("_roast_date"));
        setViewText(mTxtGrind, data.get("_grind"));

        final int brewMethod = stringToInt(data.get("_brew_method"));
        if(brewMethod > 0) {
            final Resources res = getResources();
            final String[] servingTypes = res.getStringArray(R.array.coffee_brew_methods);
            mTxtBrewMethod.setText(servingTypes[brewMethod]);
            setIsEspresso(brewMethod == 4);
        } else {
            mTxtBrewMethod.setText(R.string.hint_empty);
        }

        setTextWithUnit(mTxtDose, data.get("_stats_dose"), "g");
        setTextWithUnit(mTxtMass, data.get("_stats_mass"), "g");

        final float dose = stringToFloat(data.get("_stats_dose"));
        final float mass = stringToFloat(data.get("_stats_mass"));
        if(dose > 0 && mass > 0) {
            if(brewMethod == 4) {
                mTxtRatio.setText(String.format(Locale.US, "%.1f%%", dose / mass * 100));
            } else {
                mTxtRatio.setText(String.format(Locale.US, "%.1f", mass / dose));
            }
        } else {
            mTxtRatio.setText(null);
        }

        setTextWithUnit(mTxtTemp, data.get("_stats_temp"), "°C");

        final int extTime = stringToInt(data.get("_stats_extime"));
        if(extTime > 0) {
            final int extTimeM = extTime / 60;
            final int extTimeS = extTime % 60;
            mTxtExtTime.setText(String.format(Locale.US, "%d:%02d", extTimeM, extTimeS));
        } else {
            mTxtExtTime.setText(null);
        }

        setTextWithUnit(mTxtTDS, data.get("_stats_tds"), "%");
        setTextWithUnit(mTxtYield, data.get("_stats_yield"), "%");
    }

    private void setIsEspresso(boolean isEspresso) {
        if(isEspresso) {
            mTxtLabelMass.setText(R.string.coffee_label_esp_mass);
            mTxtLabelRatio.setText(R.string.coffee_label_ebf);
        } else {
            mTxtLabelMass.setText(R.string.coffee_label_water_mass);
            mTxtLabelRatio.setText(R.string.coffee_label_cbr);
        }
    }

    private void setTextWithUnit(TextView view, String text, String unit) {
        if(!TextUtils.isEmpty(text)) {
            view.setText(text + unit);
        } else {
            view.setText(null);
        }
    }
}
