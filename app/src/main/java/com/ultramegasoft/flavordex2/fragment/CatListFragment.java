package com.ultramegasoft.flavordex2.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.EntryListActivity;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.CatListAdapter;

/**
 * Fragment for showing the list of categories.
 *
 * @author Steve Guidetti
 */
public class CatListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.list_toolbar);
        if(toolbar != null) {
            toolbar.getMenu().clear();
            toolbar.setTitle(R.string.title_categories);
        } else {
            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.setSubtitle(R.string.title_categories);
            }
        }
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final String catName = ((CatListAdapter)getListAdapter()).getItem(position).realName;
        ((EntryListActivity)getActivity()).onCatSelected(id, catName, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), Tables.Cats.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setListAdapter(new CatListAdapter(getContext(), data, android.R.layout.simple_list_item_1,
                android.R.id.text1));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
