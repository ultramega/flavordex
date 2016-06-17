package com.ultramegasoft.flavordex2.beer;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.SpecialArrayAdapter;

import java.util.LinkedHashMap;

/**
 * Beer specific entry form helper.
 *
 * @author Steve Guidetti
 */
public class BeerEntryFormHelper extends EntryFormHelper {
    /**
     * The Views for the form fields
     */
    public AutoCompleteTextView mTxtStyle;
    public Spinner mSpnServing;
    public EditText mTxtIBU;
    public EditText mTxtABV;
    public EditText mTxtOG;
    public EditText mTxtFG;

    public BeerEntryFormHelper(Fragment fragment, View layoutRoot) {
        super(fragment, layoutRoot);
    }

    @Override
    protected void loadLayout(View root) {
        super.loadLayout(root);
        mTxtStyle = (AutoCompleteTextView)root.findViewById(R.id.entry_style);
        mTxtStyle.setAdapter(SpecialArrayAdapter.createFromResource(mFragment.getContext(),
                R.array.beer_styles, android.R.layout.simple_dropdown_item_1line));

        mSpnServing = (Spinner)root.findViewById(R.id.entry_serving_type);

        mTxtIBU = (EditText)root.findViewById(R.id.entry_stats_ibu);
        mTxtABV = (EditText)root.findViewById(R.id.entry_stats_abv);
        mTxtOG = (EditText)root.findViewById(R.id.entry_stats_og);
        mTxtFG = (EditText)root.findViewById(R.id.entry_stats_fg);
    }

    @Override
    public void setExtras(LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.setExtras(extras);
        initEditText(mTxtStyle, extras.get(Tables.Extras.Beer.STYLE));
        initSpinner(mSpnServing, extras.get(Tables.Extras.Beer.SERVING));
        initEditText(mTxtIBU, extras.get(Tables.Extras.Beer.STATS_IBU));
        initEditText(mTxtABV, extras.get(Tables.Extras.Beer.STATS_ABV));
        initEditText(mTxtOG, extras.get(Tables.Extras.Beer.STATS_OG));
        initEditText(mTxtFG, extras.get(Tables.Extras.Beer.STATS_FG));
    }
}
