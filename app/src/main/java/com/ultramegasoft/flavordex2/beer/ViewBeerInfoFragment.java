package com.ultramegasoft.flavordex2.beer;

import android.content.res.Resources;
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
 * Beer specific entry view Fragment.
 *
 * @author Steve Guidetti
 */
public class ViewBeerInfoFragment extends ViewInfoFragment {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        return R.layout.fragment_view_info_beer;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, ExtraFieldHolder> data) {
        super.populateExtras(data);
        setViewText(mTxtStyle, data.get(Tables.Extras.Beer.STYLE).value);

        final int servingType = stringToInt(data.get(Tables.Extras.Beer.SERVING).value);
        if(servingType > 0) {
            final Resources res = getResources();
            final String[] servingTypes = res.getStringArray(R.array.beer_serving_types);
            mTxtServingType.setText(servingTypes[servingType]);
        } else {
            mTxtServingType.setText(R.string.hint_empty);
        }

        mTxtIBU.setText(data.get(Tables.Extras.Beer.STATS_IBU).value);
        mTxtABV.setText(data.get(Tables.Extras.Beer.STATS_ABV).value);
        mTxtOG.setText(data.get(Tables.Extras.Beer.STATS_OG).value);
        mTxtFG.setText(data.get(Tables.Extras.Beer.STATS_FG).value);
    }
}
