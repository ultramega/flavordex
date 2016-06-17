package com.ultramegasoft.flavordex2.whiskey;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;

/**
 * Whiskey specific entry form helper.
 *
 * @author Steve Guidetti
 */
public class WhiskeyEntryFormHelper extends EntryFormHelper {
    /**
     * The Views for the form fields
     */
    public AutoCompleteTextView mTxtType;
    public EditText mTxtAge;
    public EditText mTxtABV;

    public WhiskeyEntryFormHelper(Fragment fragment, View layoutRoot) {
        super(fragment, layoutRoot);
    }

    @Override
    protected void loadLayout(View root) {
        super.loadLayout(root);
        mTxtType = (AutoCompleteTextView)root.findViewById(R.id.entry_type);
        mTxtType.setAdapter(ArrayAdapter.createFromResource(mFragment.getContext(),
                R.array.whiskey_types, android.R.layout.simple_dropdown_item_1line));

        mTxtAge = (EditText)root.findViewById(R.id.entry_stats_age);
        mTxtABV = (EditText)root.findViewById(R.id.entry_stats_abv);
    }

    @Override
    public void setExtras(LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.setExtras(extras);
        initEditText(mTxtType, extras.get(Tables.Extras.Whiskey.STYLE));
        initEditText(mTxtAge, extras.get(Tables.Extras.Whiskey.STATS_AGE));
        initEditText(mTxtABV, extras.get(Tables.Extras.Whiskey.STATS_ABV));
    }
}
