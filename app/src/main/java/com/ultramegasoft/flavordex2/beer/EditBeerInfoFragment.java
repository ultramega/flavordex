package com.ultramegasoft.flavordex2.beer;

import android.view.View;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EditInfoFragment;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;

/**
 * Fragment for editing details for a new or existing beer entry.
 *
 * @author Steve Guidetti
 */
public class EditBeerInfoFragment extends EditInfoFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info_beer;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        return new BeerEntryFormHelper(this, root);
    }
}
