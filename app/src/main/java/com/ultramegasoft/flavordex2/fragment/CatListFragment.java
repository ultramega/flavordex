package com.ultramegasoft.flavordex2.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.EditCatActivity;
import com.ultramegasoft.flavordex2.EntryListActivity;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.dialog.CatDeleteDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.CatListAdapter;

/**
 * Fragment for showing the list of categories.
 *
 * @author Steve Guidetti
 */
public class CatListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private CatListAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupToolbar();
        registerForContextMenu(getListView());
        getLoaderManager().initLoader(0, null, this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().remove(FlavordexApp.PREF_LIST_CAT_ID).remove(FlavordexApp.PREF_LIST_CAT_NAME)
                .apply();
    }

    private void setupToolbar() {
        final Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.list_toolbar);
        if(toolbar != null) {
            toolbar.setNavigationIcon(null);
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.cat_list_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            toolbar.setTitle(R.string.title_categories);
        } else {
            setHasOptionsMenu(true);
            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setSubtitle(R.string.title_categories);
            }
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if(enter) {
            return AnimationUtils.loadAnimation(getContext(), R.anim.fragment_in_from_left);
        } else {
            return AnimationUtils.loadAnimation(getContext(), R.anim.fragment_out_to_left);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final String catName;
        if(id > 0) {
            catName = ((CatListAdapter)getListAdapter()).getItem(position).realName;
        } else {
            catName = null;
        }
        ((EntryListActivity)getActivity()).onCatSelected(id, catName, false);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putLong(FlavordexApp.PREF_LIST_CAT_ID, id)
                .putString(FlavordexApp.PREF_LIST_CAT_NAME, catName).apply();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.cat_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_add_cat:
                startActivity(new Intent(getContext(), EditCatActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)menuInfo;
        if(mAdapter.getShowAllCats() && info.position == 0) {
            return;
        }
        getActivity().getMenuInflater().inflate(R.menu.cat_context_menu, menu);

        if(mAdapter.getItem(info.position).preset) {
            menu.findItem(R.id.menu_delete).setEnabled(false).setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.menu_edit:
                EditCatActivity.startActivity(getContext(), info.id,
                        mAdapter.getItem(info.position).name);
                return true;
            case R.id.menu_delete:
                CatDeleteDialog.showDialog(getFragmentManager(), null, 0, info.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), Tables.Cats.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter = new CatListAdapter(getContext(), data, android.R.layout.simple_list_item_2);
        mAdapter.setShowAllCats(true);
        setListAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
