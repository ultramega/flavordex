package com.ultramegasoft.flavordex2.coffee;

import android.view.View;

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
        return new CoffeeEntryFormHelper(this, root);
    }
}
