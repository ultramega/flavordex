package com.ultramegasoft.flavordex2.wine;

import android.view.View;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EntrySearchFragment;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;

/**
 * Wine specific entry search form Fragment.
 *
 * @author Steve Guidetti
 */
public class WineSearchFormFragment extends EntrySearchFragment.SearchFormFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_search_form_wine;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        return new WineEntryFormHelper(this, root);
    }

    @Override
    public void resetForm() {
        super.resetForm();
        final WineEntryFormHelper helper = (WineEntryFormHelper)mFormHelper;
        helper.mTxtVarietal.setText(null);
        helper.mTxtVintage.setText(null);
        helper.mTxtABV.setText(null);
    }
}
