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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
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
    /**
     * The Adapter backing the list
     */
    private CatListAdapter mAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupToolbar();
        getLoaderManager().initLoader(0, null, this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().remove(FlavordexApp.PREF_LIST_CAT_ID).apply();

        registerForContextMenu(getListView());
    }

    /**
     * Set up the list Toolbar.
     */
    private void setupToolbar() {
        final Activity activity = getActivity();
        if(activity == null) {
            return;
        }

        final Toolbar toolbar = activity.findViewById(R.id.list_toolbar);
        if(toolbar != null) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.cat_list_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            toolbar.setNavigationIcon(null);
            toolbar.setTitle(R.string.title_categories);
        } else {
            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setSubtitle(R.string.title_categories);
            }
            setHasOptionsMenu(true);
        }
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

        final Activity activity = getActivity();
        if(activity == null) {
            return;
        }

        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)menuInfo;
        if(mAdapter.getShowAllCats() && info.position == 0) {
            return;
        }
        activity.getMenuInflater().inflate(R.menu.cat_context_menu, menu);

        if(mAdapter.getItem(info.position).preset) {
            menu.findItem(R.id.menu_delete).setEnabled(false).setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final Context context = getContext();
        final FragmentManager fm = getFragmentManager();
        if(context == null || fm == null) {
            return super.onContextItemSelected(item);
        }

        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.menu_edit:
                EditCatActivity.startActivity(context, info.id,
                        mAdapter.getItem(info.position).name);
                return true;
            case R.id.menu_delete:
                CatDeleteDialog.showDialog(fm, null, 0, info.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
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

        final EntryListActivity activity = (EntryListActivity)getActivity();
        if(activity != null) {
            activity.onCatSelected(id, false);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Context context = getContext();
        if(context == null) {
            return null;
        }

        return new CursorLoader(context, Tables.Cats.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        final Context context = getContext();
        if(context == null) {
            return;
        }

        mAdapter = new CatListAdapter(context, data, android.R.layout.simple_list_item_2);
        mAdapter.setShowAllCats(true);
        setListAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}
