package com.ultramegasoft.flavordex2.wine;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.ultramegasoft.flavordex2.EditInfoFragment;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.SpecialArrayAdapter;

import java.util.HashMap;

/**
 * Fragment for adding details for a new wine entry.
 *
 * @author Steve Guidetti
 */
public class EditWineInfoFragment extends EditInfoFragment {
    /**
     * The views for the form fields
     */
    private AutoCompleteTextView mTxtVarietal;
    private EditText mTxtVintage;
    private EditText mTxtABV;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtVarietal = (AutoCompleteTextView)root.findViewById(R.id.entry_varietal);
        mTxtVarietal.setAdapter(SpecialArrayAdapter.createFromResource(
                getContext(),
                R.array.wine_varietals,
                android.R.layout.simple_dropdown_item_1line
        ));

        mTxtVintage = (EditText)root.findViewById(R.id.entry_stats_vintage);
        mTxtABV = (EditText)root.findViewById(R.id.entry_stats_abv);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info_wine;
    }

    @Override
    protected void populateExtras(HashMap<String, ExtraFieldHolder> extras) {
        super.populateExtras(extras);
        initEditText(mTxtVarietal, extras.get(Tables.Extras.Wine.VARIETAL));
        initEditText(mTxtVintage, extras.get(Tables.Extras.Wine.STATS_VINTAGE));
        initEditText(mTxtABV, extras.get(Tables.Extras.Wine.STATS_ABV));
    }
}
