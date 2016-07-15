package com.ultramegasoft.flavordex2.coffee;

import android.view.View;
import android.widget.AdapterView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EntrySearchFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

/**
 * Coffee specific entry search form Fragment.
 *
 * @author Steve Guidetti
 */
public class CoffeeSearchFormFragment extends EntrySearchFragment.SearchFormFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_search_form_coffee;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        final CoffeeEntryFormHelper helper = new CoffeeEntryFormHelper(this, root);
        helper.mSpnBrewMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                helper.setIsEspresso(position == 5);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return helper;
    }

    @Override
    public void resetForm() {
        super.resetForm();
        final CoffeeEntryFormHelper helper = (CoffeeEntryFormHelper)mFormHelper;
        helper.mTxtRoaster.setText(null);
        helper.mTxtRoastDate.setText(null);
        helper.mTxtGrind.setText(null);
        helper.mSpnBrewMethod.setSelection(0);
        helper.mTxtDose.setText(null);
        helper.mTxtEspMass.setText(null);
        helper.mTxtWaterMass.setText(null);
        helper.mTxtTemp.setText(null);
        helper.mTxtExtTimeM.setText(null);
        helper.mTxtExtTimeS.setText(null);
        helper.mTxtTDS.setText(null);
        helper.mTxtYield.setText(null);
    }

    @Override
    protected boolean parsePresetField(ExtraFieldHolder extra) {
        switch(extra.name) {
            case Tables.Extras.Coffee.BREW_METHOD:
                if(!extra.value.equals("0")) {
                    final int offset = Integer.parseInt(extra.value) - 1;
                    final ExtraFieldHolder offsetExtra =
                            new ExtraFieldHolder(extra.id, extra.name, true, offset + "");
                    parseExtraField(offsetExtra, COMP_EQ);
                }
                return true;
        }
        return super.parsePresetField(extra);
    }
}
