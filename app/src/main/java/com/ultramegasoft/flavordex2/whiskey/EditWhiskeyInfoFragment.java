package com.ultramegasoft.flavordex2.whiskey;

import android.view.View;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EditInfoFragment;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;

/**
 * Fragment for editing details for a new or existing whiskey entry.
 *
 * @author Steve Guidetti
 */
public class EditWhiskeyInfoFragment extends EditInfoFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info_whiskey;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        return new WhiskeyEntryFormHelper(this, root);
    }
}
