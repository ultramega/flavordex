package com.ultramegasoft.flavordex2.whiskey;

import android.view.View;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EntrySearchFragment;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;

/**
 * Whiskey specific entry search form Fragment.
 *
 * @author Steve Guidetti
 */
public class WhiskeySearchFormFragment extends EntrySearchFragment.SearchFormFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_search_form_whiskey;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        return new WhiskeyEntryFormHelper(this, root);
    }

    @Override
    public void resetForm() {
        super.resetForm();
        final WhiskeyEntryFormHelper helper = (WhiskeyEntryFormHelper)mFormHelper;
        helper.mTxtType.setText(null);
        helper.mTxtAge.setText(null);
        helper.mTxtABV.setText(null);
    }
}
