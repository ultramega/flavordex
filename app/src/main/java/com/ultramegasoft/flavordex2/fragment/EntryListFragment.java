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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.AddEntryActivity;
import com.ultramegasoft.flavordex2.EditEntryActivity;
import com.ultramegasoft.flavordex2.EntryListActivity;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.dialog.CatListDialog;
import com.ultramegasoft.flavordex2.dialog.ConfirmationDialog;
import com.ultramegasoft.flavordex2.dialog.ExportDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryDeleter;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.widget.EntryListAdapter;

/**
 * The main entry list Fragment. Shows a list of all the journal entries in a category.
 *
 * @author Steve Guidetti
 */
public class EntryListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keys for the Fragment arguments
     */
    private static final String ARG_CAT = "cat";
    private static final String ARG_TWO_PANE = "two_pane";
    private static final String ARG_SELECTED_ITEM = "selected_item";
    private static final String ARG_WHERE = "where";
    private static final String ARG_WHERE_ARGS = "where_args";
    private static final String ARG_EXPORT_MODE = "export_mode";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_ADD_ENTRY = 301;
    private static final int REQUEST_DELETE_ENTRY = 302;
    private static final int REQUEST_SELECT_CAT = 303;

    /**
     * Extras for Activity results
     */
    private static final String EXTRA_ENTRY_ID = "entry_id";

    /**
     * Keys for the saved state
     */
    private static final String STATE_SELECTED_ITEM = "selected_item";
    private static final String STATE_EXPORT_MODE = "export_mode";

    /**
     * Loader IDs
     */
    private static final int LOADER_ENTRIES = 0;
    private static final int LOADER_CAT = 1;

    /**
     * The fields to query from the database
     */
    private static final String[] LIST_PROJECTION = new String[] {
            Tables.Entries._ID,
            Tables.Entries.CAT_ID,
            Tables.Entries.CAT,
            Tables.Entries.TITLE,
            Tables.Entries.MAKER,
            Tables.Entries.RATING,
            Tables.Entries.DATE
    };

    /**
     * Whether the Activity is in two-pane mode
     */
    private boolean mTwoPane;

    /**
     * The main list Toolbar
     */
    @Nullable
    private Toolbar mToolbar;

    /**
     * The Toolbar for export selection mode
     */
    @Nullable
    private Toolbar mExportToolbar;

    /**
     * Toolbar Animations
     */
    @Nullable
    private Animation mExportInAnimation;
    @Nullable
    private Animation mExportOutAnimation;

    /**
     * The current activated item if in two-pane mode
     */
    private long mActivatedItem = -1;

    /**
     * The where string to use in the database query
     */
    @Nullable
    private String mWhere;

    /**
     * The arguments for the where clause
     */
    @Nullable
    private String[] mWhereArgs;

    /**
     * The database column to sort by
     */
    @NonNull
    private String mSortField = Tables.Entries.TITLE;

    /**
     * Whether to sort entries in reverse order
     */
    private boolean mSortReversed = false;

    /**
     * The category ID
     */
    private long mCatId = 0;

    /**
     * The category name
     */
    @Nullable
    private String mCatName;

    /**
     * Whether the list is in export selection mode
     */
    private boolean mExportMode = false;

    /**
     * The Adapter for the ListView
     */
    private EntryListAdapter mAdapter;

    /**
     * Get an instance of this Fragment.
     *
     * @param catId        The category ID
     * @param twoPane      Whether the layout is in two-pane mode
     * @param selectedItem The selected item ID
     * @param exportMode   Whether to enable export mode
     * @param where        The where clause
     * @param whereArgs    The values for the parameters of the where clause
     * @return An instance of this Fragment
     */
    @NonNull
    public static EntryListFragment getInstance(long catId, boolean twoPane, long selectedItem,
                                                boolean exportMode, @Nullable String where,
                                                @Nullable String[] whereArgs) {
        final EntryListFragment fragment = new EntryListFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_CAT, catId);
        args.putBoolean(ARG_TWO_PANE, twoPane);
        args.putLong(ARG_SELECTED_ITEM, selectedItem);
        args.putBoolean(ARG_EXPORT_MODE, exportMode);
        args.putString(ARG_WHERE, where);
        args.putStringArray(ARG_WHERE_ARGS, whereArgs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        mCatId = args != null ? args.getLong(ARG_CAT, mCatId) : mCatId;
        mTwoPane = args != null && args.getBoolean(ARG_TWO_PANE, mTwoPane);
        mActivatedItem =
                args != null ? args.getLong(ARG_SELECTED_ITEM, mActivatedItem) : mActivatedItem;
        mWhere = args != null ? args.getString(ARG_WHERE) : null;
        mWhereArgs = args != null ? args.getStringArray(ARG_WHERE_ARGS) : null;
        mExportMode = args != null && args.getBoolean(ARG_EXPORT_MODE, mExportMode);

        if(savedInstanceState != null) {
            mActivatedItem = savedInstanceState.getLong(STATE_SELECTED_ITEM, mActivatedItem);
            mExportMode = savedInstanceState.getBoolean(STATE_EXPORT_MODE, mExportMode);
        }

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        mSortField = prefs.getString(FlavordexApp.PREF_LIST_SORT_FIELD, mSortField);
        mSortReversed = prefs.getBoolean(FlavordexApp.PREF_LIST_SORT_REVERSED, mSortReversed);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setActivateOnItemClick(mTwoPane);

        setupToolbar();
        registerForContextMenu(getListView());

        setEmptyText();

        final Context context = getContext();
        if(context != null) {
            mAdapter = new EntryListAdapter(context);
            setListShown(false);
            setListAdapter(mAdapter);
        }

        setExportMode(mExportMode, false);

        getLoaderManager().initLoader(LOADER_ENTRIES, null, this);
        if(mCatId > 0) {
            getLoaderManager().initLoader(LOADER_CAT, null, this);
        } else {
            setCatName(null);
        }

        if(context != null) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putLong(FlavordexApp.PREF_LIST_CAT_ID, mCatId).apply();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_entry_list, container, false);

        final FrameLayout list = root.findViewById(R.id.list);
        list.addView(super.onCreateView(inflater, container, savedInstanceState));

        return root;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if(enter) {
            return AnimationUtils.loadAnimation(getContext(), R.anim.fragment_in_from_right);
        } else {
            return AnimationUtils.loadAnimation(getContext(), R.anim.fragment_out_to_right);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        if(mExportMode) {
            invalidateExportMenu();
            return;
        }

        mActivatedItem = id;

        final EntryListActivity activity = (EntryListActivity)getActivity();
        if(activity != null) {
            final Cursor cursor = (Cursor)mAdapter.getItem(position);
            final String catName = cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT));
            final long catId = cursor.getLong(cursor.getColumnIndex(Tables.Entries.CAT_ID));
            activity.onItemSelected(id, catName, catId);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_ITEM, mActivatedItem);
        outState.putBoolean(STATE_EXPORT_MODE, mExportMode);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.entry_list_menu, menu);
        setupMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                final EntryListActivity activity = (EntryListActivity)getActivity();
                if(activity != null) {
                    activity.onCatSelected(-1, false);
                }
                return true;
            case R.id.menu_sort_name:
                item.setChecked(true);
                setSort(Tables.Entries.TITLE);
                return true;
            case R.id.menu_sort_date:
                item.setChecked(true);
                setSort(Tables.Entries.DATE);
                return true;
            case R.id.menu_sort_rating:
                item.setChecked(true);
                setSort(Tables.Entries.RATING);
                return true;
            case R.id.menu_add_entry:
                if(mCatName != null) {
                    addEntry(mCatId, mCatName);
                } else {
                    final FragmentManager fm = getFragmentManager();
                    if(fm != null) {
                        CatListDialog.showDialog(fm, this, REQUEST_SELECT_CAT);
                    }
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final Activity activity = getActivity();
        if(activity != null) {
            activity.getMenuInflater().inflate(R.menu.entry_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final Context context = getContext();

        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        final Cursor cursor = (Cursor)mAdapter.getItem(info.position);

        switch(item.getItemId()) {
            case R.id.menu_share:
                if(context != null && cursor != null) {
                    final String title =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                    final float rating =
                            cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
                    EntryUtils.share(context, title, rating);
                }
                return true;
            case R.id.menu_edit_entry:
                if(context != null && cursor != null) {
                    EditEntryActivity.startActivity(context, info.id,
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT)));
                }
                return true;
            case R.id.menu_delete_entry:
                if(cursor != null) {
                    final FragmentManager fm = getFragmentManager();
                    if(fm != null) {
                        final String title =
                                cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                        final Intent deleteIntent = new Intent();
                        deleteIntent.putExtra(EXTRA_ENTRY_ID, info.id);
                        ConfirmationDialog.showDialog(fm, this, REQUEST_DELETE_ENTRY,
                                getString(R.string.title_delete_entry),
                                getString(R.string.message_confirm_delete, title),
                                R.drawable.ic_delete, deleteIntent);
                    }
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_SELECT_CAT:
                    addEntry(data.getLongExtra(CatListDialog.EXTRA_CAT_ID, 0),
                            data.getStringExtra(CatListDialog.EXTRA_CAT_NAME));
                    break;
                case REQUEST_ADD_ENTRY:
                    final FragmentManager fm = getFragmentManager();
                    if(fm != null) {
                        CatListDialog.closeDialog(fm);
                    }

                    final EntryListActivity activity = (EntryListActivity)getActivity();
                    final long entryId = data.getLongExtra(AddEntryActivity.EXTRA_ENTRY_ID, 0);
                    if(activity != null && entryId > 0) {
                        mActivatedItem = entryId;
                        activity.onItemSelected(entryId,
                                data.getStringExtra(AddEntryActivity.EXTRA_ENTRY_CAT),
                                data.getLongExtra(AddEntryActivity.EXTRA_ENTRY_CAT_ID, 0));
                    }
                    break;
                case REQUEST_DELETE_ENTRY:
                    final Context context = getContext();
                    if(context != null) {
                        final long id = data.getLongExtra(EXTRA_ENTRY_ID, 0);
                        new EntryDeleter(context, id).execute();
                    }
                    break;
            }
        }
    }

    /**
     * Start the Add Entry Activity.
     *
     * @param catId   The category ID
     * @param catName The category name
     */
    private void addEntry(long catId, @Nullable String catName) {
        final Context context = getContext();
        if(context != null) {
            final Intent addIntent = AddEntryActivity.getIntent(context, catId, catName);
            startActivityForResult(addIntent, REQUEST_ADD_ENTRY);
        }
    }

    /**
     * Set up the list Toolbar if it exists.
     */
    @SuppressLint("PrivateResource")
    private void setupToolbar() {
        final EntryListActivity activity = (EntryListActivity)getActivity();
        if(activity == null) {
            return;
        }

        mToolbar = activity.findViewById(R.id.list_toolbar);
        if(mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            mToolbar.setNavigationContentDescription(R.string.abc_action_bar_up_description);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onCatSelected(-1, false);
                }
            });
            final Menu menu = mToolbar.getMenu();
            menu.clear();
            mToolbar.inflateMenu(R.menu.entry_list_menu);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            setupMenu(menu);
        } else {
            setHasOptionsMenu(true);
            final ActionBar actionBar = activity.getSupportActionBar();
            if(actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    /**
     * Set up the list menu.
     *
     * @param menu A Menu from the ActionBar or Toolbar
     */
    private void setupMenu(@Nullable Menu menu) {
        if(menu == null) {
            return;
        }

        if(mSortField.equals(Tables.Entries.TITLE)) {
            menu.findItem(R.id.menu_sort_name).setChecked(true);
        }
        if(mSortField.equals(Tables.Entries.DATE)) {
            menu.findItem(R.id.menu_sort_date).setChecked(true);
        }
        if(mSortField.equals(Tables.Entries.RATING)) {
            menu.findItem(R.id.menu_sort_rating).setChecked(true);
        }

    }

    /**
     * Set the field to sort the list by. If the field is the same as the current field, the order
     * is reversed.
     *
     * @param field The name of the database column to sort by
     */
    private void setSort(@NonNull String field) {
        if(mSortField.equals(field)) {
            mSortReversed = !mSortReversed;
        }
        mSortField = field;
        getLoaderManager().restartLoader(0, null, this);

        final SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getContext()).edit();
        editor.putString(FlavordexApp.PREF_LIST_SORT_FIELD, mSortField);
        editor.putBoolean(FlavordexApp.PREF_LIST_SORT_REVERSED, mSortReversed);
        editor.apply();
    }

    /**
     * Set the text shown when the list is empty based on any filters that are set.
     */
    private void setEmptyText() {
        CharSequence emptyText;
        if(mWhere == null) {
            emptyText = getText(R.string.message_no_data);
        } else {
            emptyText = getString(R.string.message_no_data_filter);
        }
        setEmptyText(emptyText);
    }

    /**
     * Set the subtitle. This will display the category name as the title of the list Toolbar or as
     * the ActionBar subtitle.
     *
     * @param catName The category name
     */
    private void setCatName(@Nullable String catName) {
        final String subtitle;
        if(mWhere != null) {
            subtitle = getString(R.string.title_search_results);
        } else if(catName == null) {
            subtitle = getString(R.string.title_all_entries);
        } else {
            mCatName = catName;
            subtitle = getString(R.string.title_cat_entries,
                    FlavordexApp.getRealCatName(getContext(), mCatName));
        }
        if(mToolbar != null) {
            mToolbar.setTitle(subtitle);
        } else {
            final AppCompatActivity activity = (AppCompatActivity)getActivity();
            if(activity != null) {
                final ActionBar actionBar = activity.getSupportActionBar();
                if(actionBar != null) {
                    actionBar.setSubtitle(subtitle);
                }
            }
        }
    }

    /**
     * Turn on activate-on-click mode. When this mode is on, list items will be given the activated
     * state when touched.
     */
    private void setActivateOnItemClick(boolean activateOnItemClick) {
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
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
        final Activity activity = getActivity();
        if(activity != null && mExportToolbar == null) {
            mExportToolbar = activity.findViewById(R.id.export_toolbar);
            mExportToolbar.inflateMenu(R.menu.export_menu);
            mExportToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_export_selected:
                            final FragmentManager fm = getFragmentManager();
                            if(fm != null) {
                                ExportDialog.showDialog(fm, getListView().getCheckedItemIds());
                                setExportMode(false, true);
                            }
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

    /**
     * Set the selected list item.
     *
     * @param position The index of the item to activate
     */
    private void setActivatedPosition(int position) {
        if(!mExportMode && position != ListView.INVALID_POSITION) {
            getListView().setItemChecked(position, true);
        }
    }

    /**
     * Clear the selected list item.
     */
    public void clearSelection() {
        final EntryListActivity activity = (EntryListActivity)getActivity();
        if(activity == null) {
            return;
        }

        final ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setSubtitle(null);
        }
        getListView().setItemChecked(mAdapter.getItemIndex(mActivatedItem), false);
        mActivatedItem = -1;
        activity.onItemSelected(mActivatedItem, null, 0);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Context context = getContext();
        if(context == null) {
            return null;
        }

        switch(id) {
            case LOADER_ENTRIES:
                Uri uri;
                if(mCatId > 0) {
                    uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_CAT_URI_BASE, mCatId);
                } else {
                    uri = Tables.Entries.CONTENT_URI;
                }
                final String sort = mSortField + (mSortReversed ? " DESC" : " ASC");
                return new CursorLoader(context, uri, LIST_PROJECTION, mWhere, mWhereArgs, sort);
            case LOADER_CAT:
                return new CursorLoader(context,
                        ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId),
                        new String[] {Tables.Cats.NAME}, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
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
                } else {
                    mAdapter.swapCursor(data);
                    setActivatedPosition(mAdapter.getItemIndex(mActivatedItem));
                }
                setListShown(true);
                break;
            case LOADER_CAT:
                if(data.moveToFirst()) {
                    setCatName(data.getString(data.getColumnIndex(Tables.Cats.NAME)));
                } else {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            final FragmentManager fm = getFragmentManager();
                            if(fm != null) {
                                fm.popBackStack();
                            }
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch(loader.getId()) {
            case LOADER_ENTRIES:
                setActivatedPosition(ListView.INVALID_POSITION);
                mAdapter.swapCursor(null);
                break;
        }
    }
}
