package com.ultramegasoft.flavordex2.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;

/**
 * Full implementation of the Fragment for editing details for a new or existing journal entry. Adds
 * support for custom categories.
 *
 * @author Steve Guidetti
 */
public class EditInfoFragment extends AbsEditInfoFragment implements LoaderManager.LoaderCallbacks {
    /**
     * The TableLayout for the main info
     */
    private TableLayout mInfoTable;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mInfoTable = (TableLayout)root.findViewById(R.id.entry_info);

        mTxtLocation.setText(((FlavordexApp)getActivity().getApplication()).getLocationName());

        return root;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_info;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, ExtraFieldHolder> extras) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        for(ExtraFieldHolder extra : extras.values()) {
            if(!extra.preset) {
                final View root = inflater.inflate(R.layout.edit_info_extra, mInfoTable, false);
                ((TextView)root.findViewById(R.id.label))
                        .setText(getString(R.string.label_field, extra.name));
                initEditText((EditText)root.findViewById(R.id.value), extra);
                mInfoTable.addView(root);
            }
        }
    }
}
