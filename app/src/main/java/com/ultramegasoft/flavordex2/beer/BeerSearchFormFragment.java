package com.ultramegasoft.flavordex2.beer;

import android.view.View;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.EntrySearchFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

/**
 * Beer specific entry search form Fragment.
 *
 * @author Steve Guidetti
 */
public class BeerSearchFormFragment extends EntrySearchFragment.SearchFormFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_search_form_beer;
    }

    @Override
    protected EntryFormHelper createHelper(View root) {
        return new BeerEntryFormHelper(this, root);
    }

    @Override
    public void resetForm() {
        super.resetForm();
        final BeerEntryFormHelper helper = (BeerEntryFormHelper)mFormHelper;
        helper.mTxtStyle.setText(null);
        helper.mSpnServing.setSelection(0);
        helper.mTxtIBU.setText(null);
        helper.mTxtABV.setText(null);
        helper.mTxtOG.setText(null);
        helper.mTxtFG.setText(null);
    }

    @Override
    protected boolean parsePresetField(ExtraFieldHolder extra) {
        switch(extra.name) {
            case Tables.Extras.Beer.SERVING:
                if(!extra.value.equals("0")) {
                    parseExtraField(extra, COMP_EQ);
                }
                return true;
        }
        return super.parsePresetField(extra);
    }
}
