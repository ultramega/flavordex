package com.ultramegasoft.flavordex2.beer;

import com.ultramegasoft.flavordex2.AddEntryInfoFragment;
import com.ultramegasoft.flavordex2.R;

/**
 * Fragment for adding details for a new beer entry.
 *
 * @author Steve Guidetti
 */
public class AddBeerInfoFragment extends AddEntryInfoFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_add_info_beer;
    }
}
