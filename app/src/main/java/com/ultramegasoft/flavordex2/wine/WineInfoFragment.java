package com.ultramegasoft.flavordex2.wine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.EntryInfoFragment;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;

import java.util.LinkedHashMap;

/**
 * Wine specific entry view fragment.
 *
 * @author Steve Guidetti
 */
public class WineInfoFragment extends EntryInfoFragment {
    /**
     * Views to hold details specific to wine
     */
    private TextView mTxtVarietal;

    private TextView mTxtVintage;
    private TextView mTxtABV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtVarietal = (TextView)root.findViewById(R.id.entry_varietal);

        mTxtVintage = (TextView)root.findViewById(R.id.entry_stats_vintage);
        mTxtABV = (TextView)root.findViewById(R.id.entry_stats_abv);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_entry_info_wine;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, String> data) {
        setViewText(mTxtVarietal, data.get(Tables.Extras.Wine.VARIETAL));

        mTxtVintage.setText(data.get(Tables.Extras.Wine.STATS_VINTAGE));
        mTxtABV.setText(data.get(Tables.Extras.Wine.STATS_ABV));
    }
}
