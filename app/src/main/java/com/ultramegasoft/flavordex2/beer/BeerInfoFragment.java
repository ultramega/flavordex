package com.ultramegasoft.flavordex2.beer;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.EntryInfoFragment;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;

import java.util.LinkedHashMap;

/**
 * Beer specific entry view fragment.
 *
 * @author Steve Guidetti
 */
public class BeerInfoFragment extends EntryInfoFragment {
    /**
     * Views to hold details specific to beer
     */
    private TextView mTxtStyle;
    private TextView mTxtServingType;

    private TextView mTxtIBU;
    private TextView mTxtABV;
    private TextView mTxtOG;
    private TextView mTxtFG;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtStyle = (TextView)root.findViewById(R.id.entry_style);

        mTxtServingType = (TextView)root.findViewById(R.id.entry_serving_type);

        mTxtIBU = (TextView)root.findViewById(R.id.entry_stats_ibu);
        mTxtABV = (TextView)root.findViewById(R.id.entry_stats_abv);
        mTxtOG = (TextView)root.findViewById(R.id.entry_stats_og);
        mTxtFG = (TextView)root.findViewById(R.id.entry_stats_fg);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_entry_info_beer;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, String> data) {
        setViewText(mTxtStyle, data.get(Tables.Extras.Beer.STYLE));

        final int servingType = stringToInt(data.get(Tables.Extras.Beer.SERVING));
        if(servingType > 0) {
            final Resources res = getResources();
            final String[] servingTypes = res.getStringArray(R.array.beer_serving_types);
            mTxtServingType.setText(servingTypes[servingType]);
        } else {
            mTxtServingType.setText(R.string.hint_empty);
        }

        mTxtIBU.setText(data.get(Tables.Extras.Beer.STATS_IBU));
        mTxtABV.setText(data.get(Tables.Extras.Beer.STATS_ABV));
        mTxtOG.setText(data.get(Tables.Extras.Beer.STATS_OG));
        mTxtFG.setText(data.get(Tables.Extras.Beer.STATS_FG));
    }
}
