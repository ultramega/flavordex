package com.ultramegasoft.flavordex2.wine;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.SpecialArrayAdapter;

import java.util.LinkedHashMap;

/**
 * Wine specific entry form helper.
 *
 * @author Steve Guidetti
 */
public class WineEntryFormHelper extends EntryFormHelper {
    /**
     * The views for the form fields
     */
    public AutoCompleteTextView mTxtVarietal;
    public EditText mTxtVintage;
    public EditText mTxtABV;

    public WineEntryFormHelper(Fragment fragment, View layoutRoot) {
        super(fragment, layoutRoot);
    }

    @Override
    protected void loadLayout(View root) {
        super.loadLayout(root);
        mTxtVarietal = (AutoCompleteTextView)root.findViewById(R.id.entry_varietal);
        mTxtVarietal.setAdapter(SpecialArrayAdapter.createFromResource(mFragment.getContext(),
                R.array.wine_varietals, android.R.layout.simple_dropdown_item_1line));

        mTxtVintage = (EditText)root.findViewById(R.id.entry_stats_vintage);
        mTxtABV = (EditText)root.findViewById(R.id.entry_stats_abv);
    }

    @Override
    public void setExtras(LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.setExtras(extras);
        initEditText(mTxtVarietal, extras.get(Tables.Extras.Wine.VARIETAL));
        initEditText(mTxtVintage, extras.get(Tables.Extras.Wine.STATS_VINTAGE));
        initEditText(mTxtABV, extras.get(Tables.Extras.Wine.STATS_ABV));
    }
}
