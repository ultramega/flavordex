package com.ultramegasoft.flavordex2.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Full implementation of the Fragment to display the main details of a journal entry. Adds support
 * for custom categories.
 *
 * @author Steve Guidetti
 */
public class ViewInfoFragment extends AbsViewInfoFragment {
    /**
     * List of extra field TableRows
     */
    private final ArrayList<View> mExtraRows = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_view_info;
    }

    @Override
    protected void populateExtras(LinkedHashMap<String, ExtraFieldHolder> data) {
        final TableLayout table = (TableLayout)getActivity().findViewById(R.id.entry_info);
        if(!mExtraRows.isEmpty()) {
            for(View tableRow : mExtraRows) {
                table.removeView(tableRow);
            }
            mExtraRows.clear();
        }
        if(data.size() > 0) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            for(ExtraFieldHolder extra : data.values()) {
                if(extra.preset) {
                    continue;
                }
                final View root = inflater.inflate(R.layout.view_info_extra, table, false);
                ((TextView)root.findViewById(R.id.label))
                        .setText(getString(R.string.label_field, extra.name));
                ((TextView)root.findViewById(R.id.value)).setText(extra.value);
                table.addView(root);
                mExtraRows.add(root);
            }
        }
    }
}
