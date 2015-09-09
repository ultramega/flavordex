package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Fragment for importing journal entries from CSV files.
 *
 * @author Steve Guidetti
 */
public class ImportFragment extends ListFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_import, container, false);

        final FrameLayout list = (FrameLayout)root.findViewById(R.id.list);
        //noinspection ConstantConditions
        list.addView(super.onCreateView(inflater, container, savedInstanceState));

        return root;
    }
}
