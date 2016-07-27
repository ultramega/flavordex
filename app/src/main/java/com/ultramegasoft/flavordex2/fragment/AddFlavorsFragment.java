/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.fragment;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.radarchart.RadarEditWidget;
import com.ultramegasoft.radarchart.RadarHolder;
import com.ultramegasoft.radarchart.RadarView;

import java.util.ArrayList;

/**
 * Fragment for adding the flavor radar values for a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddFlavorsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The Views from the layout
     */
    private RadarView mRadarView;

    /**
     * The category ID for the entry being added
     */
    private long mCatId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCatId = getArguments().getLong(AddEntryFragment.ARG_CAT_ID);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!mRadarView.hasData()) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            mRadarView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_add_flavors, container, false);
        mRadarView = (RadarView)root.findViewById(R.id.radar);
        ((RadarEditWidget)root.findViewById(R.id.edit_widget)).setTarget(mRadarView);
        return root;
    }

    /**
     * Load the flavor data into the entry.
     *
     * @param entry The entry
     */
    public void getData(EntryHolder entry) {
        if(mRadarView == null || !mRadarView.hasData()) {
            return;
        }
        for(RadarHolder flavor : mRadarView.getData()) {
            entry.addFlavor(flavor.name, flavor.value);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
        return new CursorLoader(getContext(), Uri.withAppendedPath(uri, "flavor"), null, null,
                null, Tables.Flavors.POS + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final ArrayList<RadarHolder> holders = new ArrayList<>();
        String name;
        while(data.moveToNext()) {
            name = data.getString(data.getColumnIndex(Tables.Flavors.NAME));
            holders.add(new RadarHolder(name, 0));
        }

        mRadarView.setData(holders);
        mRadarView.setInteractive(true);
        mRadarView.setVisibility(View.VISIBLE);

        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
