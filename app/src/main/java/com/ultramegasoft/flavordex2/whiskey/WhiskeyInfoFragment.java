package com.ultramegasoft.flavordex2.whiskey;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.EntryInfoFragment;
import com.ultramegasoft.flavordex2.R;

import java.util.LinkedHashMap;

/**
 * Whiskey specific entry view fragment.
 *
 * @author Steve Guidetti
 */
public class WhiskeyInfoFragment extends EntryInfoFragment {
    /**
     * Views to hold details specific to whiskey
     */
    private TextView mTxtType;

    private TextView mTxtAge;
    private TextView mTxtABV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtType = (TextView)root.findViewById(R.id.entry_type);

        mTxtAge = (TextView)root.findViewById(R.id.entry_stats_age);
        mTxtABV = (TextView)root.findViewById(R.id.entry_stats_abv);

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_entry_info_whiskey;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, String> data) {
        setViewText(mTxtType, data.get("_style"));

        mTxtAge.setText(data.get("_stats_age"));
        mTxtABV.setText(data.get("_stats_abv"));
    }
}
