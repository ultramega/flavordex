/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.beer;

import android.support.annotation.NonNull;
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
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    protected int getLayoutId() {
        return R.layout.fragment_search_form_beer;
    }

    @NonNull
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    protected EntryFormHelper createHelper(@NonNull View root) {
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
    protected boolean parsePresetField(@NonNull ExtraFieldHolder extra) {
        switch(extra.name) {
            case Tables.Extras.Beer.SERVING:
                if(extra.value != null && !extra.value.equals("0")) {
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
