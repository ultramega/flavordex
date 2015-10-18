package com.ultramegasoft.flavordex2.whiskey;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.fragment.ViewInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;

/**
 * Whiskey specific entry view Fragment.
 *
 * @author Steve Guidetti
 */
public class ViewWhiskeyInfoFragment extends ViewInfoFragment {
    /**
     * Views to hold details specific to whiskey
     */
    private TextView mTxtType;

    private TextView mTxtAge;
    private TextView mTxtABV;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtType = (TextView)root.findViewById(R.id.entry_type);

        mTxtAge = (TextView)root.findViewById(R.id.entry_stats_age);
        mTxtABV = (TextView)root.findViewById(R.id.entry_stats_abv);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_view_info_whiskey;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, ExtraFieldHolder> data) {
        super.populateExtras(data);
        setViewText(mTxtType, getExtraValue(data.get(Tables.Extras.Whiskey.STYLE)));

        mTxtAge.setText(getExtraValue(data.get(Tables.Extras.Whiskey.STATS_AGE)));
        mTxtABV.setText(getExtraValue(data.get(Tables.Extras.Whiskey.STATS_ABV)));
    }
}
