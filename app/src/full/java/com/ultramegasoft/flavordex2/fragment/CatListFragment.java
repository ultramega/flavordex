package com.ultramegasoft.flavordex2.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.ultramegasoft.flavordex2.EditCatActivity;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.dialog.CatDeleteDialog;

/**
 * Full implementation of the Fragment for showing the list of categories. Adds a menu item to
 * create a category and context menus for editing and deleting categories.
 *
 * @author Steve Guidetti
 */
public class CatListFragment extends BaseCatListFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
    }

    @Override
    protected void setupMenu(Toolbar toolbar) {
        if(toolbar != null) {
            super.setupMenu(toolbar);
            toolbar.inflateMenu(R.menu.cat_list_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
        } else {
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
}
