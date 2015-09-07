package com.ultramegasoft.flavordex2.beer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;

import com.ultramegasoft.flavordex2.EditInfoFragment;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.SpecialArrayAdapter;

import java.util.HashMap;

/**
 * Fragment for editing details for a new or existing beer entry.
 *
 * @author Steve Guidetti
 */
public class EditBeerInfoFragment extends EditInfoFragment {
    /**
     * The Views for the form fields
     */
    private AutoCompleteTextView mTxtStyle;
    private Spinner mSpnServing;
    private EditText mTxtIBU;
    private EditText mTxtABV;
    private EditText mTxtOG;
    private EditText mTxtFG;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtStyle = (AutoCompleteTextView)root.findViewById(R.id.entry_style);
        mTxtStyle.setAdapter(SpecialArrayAdapter.createFromResource(
                getContext(),
                R.array.beer_styles,
                android.R.layout.simple_dropdown_item_1line
        ));

        mSpnServing = (Spinner)root.findViewById(R.id.entry_serving_type);

        mTxtIBU = (EditText)root.findViewById(R.id.entry_stats_ibu);
        mTxtABV = (EditText)root.findViewById(R.id.entry_stats_abv);
        mTxtOG = (EditText)root.findViewById(R.id.entry_stats_og);
        mTxtFG = (EditText)root.findViewById(R.id.entry_stats_fg);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_add_info_beer;
    }

    @Override
    protected void populateExtras(final HashMap<String, ExtraFieldHolder> extras) {
        super.populateExtras(extras);
        initEditText(mTxtStyle, extras.get(Tables.Extras.Beer.STYLE));
        initSpinner(mSpnServing, extras.get(Tables.Extras.Beer.SERVING));
        initEditText(mTxtIBU, extras.get(Tables.Extras.Beer.STATS_IBU));
        initEditText(mTxtABV, extras.get(Tables.Extras.Beer.STATS_ABV));
        initEditText(mTxtOG, extras.get(Tables.Extras.Beer.STATS_OG));
        initEditText(mTxtFG, extras.get(Tables.Extras.Beer.STATS_FG));
    }
}
