package com.ultramegasoft.flavordex2.util;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Form helper for the full version. Adds support for extra fields.
 *
 * @author Steve Guidetti
 */
public class EntryFormHelper extends AbsEntryFormHelper {
    /**
     * The TableLayout for the main info
     */
    private TableLayout mInfoTable;

    /**
     * @param fragment   The Fragment using this helper object
     * @param layoutRoot The root of the layout
     */
    public EntryFormHelper(Fragment fragment, View layoutRoot) {
        super(fragment, layoutRoot);
    }

    @Override
    protected void loadLayout(View root) {
        super.loadLayout(root);
        mInfoTable = (TableLayout)root.findViewById(R.id.entry_info);
    }

    @Override
    public void setExtras(LinkedHashMap<String, ExtraFieldHolder> extras) {
        super.setExtras(extras);
        final LayoutInflater inflater = LayoutInflater.from(mFragment.getContext());
        for(Map.Entry<String, ExtraFieldHolder> extra : extras.entrySet()) {
            if(!extra.getValue().preset) {
                final View root = inflater.inflate(R.layout.edit_info_extra, mInfoTable, false);
                final TextView label = (TextView)root.findViewById(R.id.label);
                final EditText value = (EditText)root.findViewById(R.id.value);
                label.setText(mFragment.getString(R.string.label_field, extra.getValue().name));
                initEditText(value, extra.getValue());
                mInfoTable.addView(root);

                getExtraViews().put(extra.getValue(), value);
            }
        }
    }
}
