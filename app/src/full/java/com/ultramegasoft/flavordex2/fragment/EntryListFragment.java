package com.ultramegasoft.flavordex2.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.dialog.ExportDialog;

/**
 * Full implementation of the The main entry list Fragment. Adds support for exporting entries.
 *
 * @author Steve Guidetti
 * @see BaseEntryListFragment
 */
public class EntryListFragment extends BaseEntryListFragment {
    /**
     * Keys for the saved state
     */
    private static final String STATE_EXPORT_MODE = "export_mode";

    /**
     * The Toolbar for export selection mode
     */
    private Toolbar mExportToolbar;

    /**
     * Toolbar Animations
     */
    private Animation mExportInAnimation;
    private Animation mExportOutAnimation;

    /**
     * Whether the list is in export selection mode
     */
    private boolean mExportMode = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        mExportMode = args.getBoolean(ARG_EXPORT_MODE, mExportMode);

        if(savedInstanceState != null) {
            mExportMode = savedInstanceState.getBoolean(STATE_EXPORT_MODE, mExportMode);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setExportMode(mExportMode, false);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if(mExportMode) {
            invalidateExportMenu();
            return;
        }
        super.onListItemClick(listView, view, position, id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_EXPORT_MODE, mExportMode);
    }

    /**
     * Enable or disable export mode.
     *
     * @param exportMode Whether to enable export mode
     * @param animate    Whether to animate the export toolbar
     */
    public void setExportMode(boolean exportMode, boolean animate) {
        final ListView listView = getListView();
        if(exportMode) {
            listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        } else {
            setActivateOnItemClick(mTwoPane);
            for(long id : getListView().getCheckedItemIds()) {
                listView.setItemChecked(mAdapter.getItemIndex(id), false);
            }
        }
        mAdapter.setMultiChoiceMode(exportMode);
        listView.setItemChecked(mAdapter.getItemIndex(mActivatedItem), !exportMode && mTwoPane);
        showExportToolbar(exportMode, animate);

        mExportMode = exportMode;
    }

    /**
     * Show or hide the export Toolbar.
     *
     * @param show Whether to show the export Toolbar
     */
    private void showExportToolbar(boolean show, boolean animate) {
        if(mExportToolbar == null) {
            mExportToolbar = (Toolbar)getActivity().findViewById(R.id.export_toolbar);
            mExportToolbar.inflateMenu(R.menu.export_menu);
            mExportToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_export_selected:
                            ExportDialog.showDialog(getFragmentManager(),
                                    getListView().getCheckedItemIds());
                            setExportMode(false, true);
                            return true;
                        case R.id.menu_cancel:
                            setExportMode(false, true);
                            return true;
                        case R.id.menu_check_all:
                        case R.id.menu_uncheck_all:
                            final ListView listView = getListView();
                            final boolean check = item.getItemId() == R.id.menu_check_all;
                            for(int i = 0; i < mAdapter.getCount(); i++) {
                                listView.setItemChecked(i, check);
                            }
                            invalidateExportMenu();
                            return true;
                    }
                    return false;
                }
            });

            mExportInAnimation = AnimationUtils.loadAnimation(getContext(),
                    R.anim.toolbar_slide_in_bottom);
            mExportOutAnimation = AnimationUtils.loadAnimation(getContext(),
                    R.anim.toolbar_slide_out_bottom);
        }

        invalidateExportMenu();

        mExportToolbar.setVisibility(show ? View.VISIBLE : View.GONE);
        if(animate) {
            mExportToolbar.startAnimation(show ? mExportInAnimation : mExportOutAnimation);
        }
    }

    /**
     * Update the enabled state of the export button based on whether any items are checked.
     */
    private void invalidateExportMenu() {
        if(mExportToolbar != null) {
            mExportToolbar.getMenu().findItem(R.id.menu_export_selected)
                    .setEnabled(getListView().getCheckedItemCount() > 0);
        }
    }

    @Override
    protected void setActivatedPosition(int position) {
        if(!mExportMode) {
            super.setActivatedPosition(position);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_ENTRIES:
                if(mExportMode) {
                    final ListView listView = getListView();
                    final long[] checkedItems = listView.getCheckedItemIds();
                    for(int i = 0; i < mAdapter.getCount(); i++) {
                        listView.setItemChecked(i, false);
                    }

                    mAdapter.swapCursor(data);
                    int pos;
                    for(long checked : checkedItems) {
                        pos = mAdapter.getItemIndex(checked);
                        if(pos != ListView.INVALID_POSITION) {
                            listView.setItemChecked(pos, true);
                        }
                    }

                    invalidateExportMenu();
                    setListShown(true);
                    return;
                }
        }
        super.onLoadFinished(loader, data);
    }
}
