package com.ultramegasoft.flavordex2.wine;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.ViewInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;

/**
 * Wine specific entry view fragment.
 *
 * @author Steve Guidetti
 */
public class ViewWineInfoFragment extends ViewInfoFragment {
    /**
     * Views to hold details specific to wine
     */
    private TextView mTxtVarietal;

    private TextView mTxtVintage;
    private TextView mTxtABV;

    @NonNull
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
        return R.layout.fragment_view_info_wine;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, ExtraFieldHolder> data) {
        super.populateExtras(data);
        setViewText(mTxtVarietal, getExtraValue(data.get(Tables.Extras.Wine.VARIETAL)));

        mTxtVintage.setText(getExtraValue(data.get(Tables.Extras.Wine.STATS_VINTAGE)));
        mTxtABV.setText(getExtraValue(data.get(Tables.Extras.Wine.STATS_ABV)));
    }
}
