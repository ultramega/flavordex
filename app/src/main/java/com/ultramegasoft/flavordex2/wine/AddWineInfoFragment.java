package com.ultramegasoft.flavordex2.wine;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.ultramegasoft.flavordex2.AddEntryInfoFragment;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.SpecialArrayAdapter;

import java.util.HashMap;

/**
 * Fragment for adding details for a new wine entry.
 *
 * @author Steve Guidetti
 */
public class AddWineInfoFragment extends AddEntryInfoFragment {
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
                getActivity(),
                R.array.wine_varietals,
                android.R.layout.simple_dropdown_item_1line
        ));

        mTxtVintage = (EditText)root.findViewById(R.id.entry_stats_vintage);
        mTxtABV = (EditText)root.findViewById(R.id.entry_stats_abv);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_add_info_wine;
    }

    @Override
    protected void addExtraRow(String name) {
    }

    @Override
    protected void readExtras(HashMap<String, String> values) {
        values.put(Tables.Extras.Wine.VARIETAL, mTxtVarietal.getText().toString());
        values.put(Tables.Extras.Wine.STATS_VINTAGE, mTxtVintage.getText().toString());
        values.put(Tables.Extras.Wine.STATS_ABV, mTxtABV.getText().toString());
    }
}
