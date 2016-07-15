package com.ultramegasoft.flavordex2.coffee;

import android.view.View;
import android.widget.AdapterView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EditInfoFragment;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;

/**
 * Fragment for editing details for a new or existing coffee entry.
 *
 * @author Steve Guidetti
 */
public class EditCoffeeInfoFragment extends EditInfoFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info_coffee;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        final CoffeeEntryFormHelper helper = new CoffeeEntryFormHelper(this, root);
        helper.mSpnBrewMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                helper.setIsEspresso(position == 4);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return helper;
    }
}
