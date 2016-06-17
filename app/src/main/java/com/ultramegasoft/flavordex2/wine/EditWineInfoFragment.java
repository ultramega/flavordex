package com.ultramegasoft.flavordex2.wine;

import android.view.View;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EditInfoFragment;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;

/**
 * Fragment for editing details for a new or existing wine entry.
 *
 * @author Steve Guidetti
 */
public class EditWineInfoFragment extends EditInfoFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info_wine;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        return new WineEntryFormHelper(this, root);
    }
}
