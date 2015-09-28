package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
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
import android.widget.SearchView;

import com.ultramegasoft.flavordex2.dialog.AppChooserDialog;
import com.ultramegasoft.flavordex2.dialog.CatListDialog;
import com.ultramegasoft.flavordex2.dialog.ConfirmationDialog;
import com.ultramegasoft.flavordex2.dialog.EntryFilterDialog;
import com.ultramegasoft.flavordex2.dialog.ExportDialog;
import com.ultramegasoft.flavordex2.dialog.FileImportDialog;
import com.ultramegasoft.flavordex2.dialog.FileSelectorDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.AppImportUtils;
import com.ultramegasoft.flavordex2.util.EntryDeleter;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;
import com.ultramegasoft.flavordex2.widget.EntryListAdapter;

/**
 * The main entry list Fragment. Shows a list of all the journal entries.
 *
 * @author Steve Guidetti
 */
public class EntryListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_SET_FILTERS = 100;
    private static final int REQUEST_ADD_ENTRY = 200;
    private static final int REQUEST_IMPORT_FILE = 300;
    private static final int REQUEST_DELETE_ENTRY = 400;
    private static final int REQUEST_SELECT_CAT = 500;

    /**
     * Extras for Activity results
     */
    private static final String EXTRA_ENTRY_ID = "entry_id";

    /**
     * Keys for the saved state
     */
    private static final String STATE_SELECTED_ITEM = "selected_item";
    private static final String STATE_SEARCH = "search";
    private static final String STATE_FILTERS = "filters";
    private static final String STATE_FILTER_TEXT = "filter_text";
    private static final String STATE_WHERE = "where";
    private static final String STATE_WHERE_ARGS = "where_args";
    private static final String STATE_EXPORT_MODE = "export_mode";

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
    private Toolbar mToolbar;

    /**
     * The Toolbar for displaying filter settings
     */
    private Toolbar mFilterToolbar;

    /**
     * Whether the filter Toolbar is showing
     */
    private boolean mFilterToolbarShowing;

    /**
     * The Toolbar for export selection mode
     */
    private Toolbar mExportToolbar;

    /**
     * Toolbar Animations
     */
    private Animation mFilterInAnimation;
    private Animation mFilterOutAnimation;
    private Animation mExportInAnimation;
    private Animation mExportOutAnimation;

    /**
     * The current activated item if in two-pane mode
     */
    private long mActivatedItem = -1;

    /**
     * The string to search for in the list query
     */
    private String mSearchQuery;

    /**
     * Map of filters to populate the filter dialog
     */
    private ContentValues mFilters;

    /**
     * The list of filter parameters to show the user
     */
    private String mFilterText;

    /**
     * The where string to use in the database query
     */
    private String mWhere;

    /**
     * The arguments for the where clause
     */
    private String[] mWhereArgs;

    /**
     * The database column to sort by
     */
    private String mSortField = Tables.Entries.TITLE;

    /**
     * Whether to sort entries in reverse order
     */
    private boolean mSortReversed = false;

    /**
     * Whether the list is in export selection mode
     */
    private boolean mExportMode = false;

    /**
     * The Adapter for the ListView
     */
    private EntryListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mActivatedItem = savedInstanceState.getLong(STATE_SELECTED_ITEM, mActivatedItem);
            mSearchQuery = savedInstanceState.getString(STATE_SEARCH);
            mFilters = savedInstanceState.getParcelable(STATE_FILTERS);
            mFilterText = savedInstanceState.getString(STATE_FILTER_TEXT);
            mWhere = savedInstanceState.getString(STATE_WHERE);
            mWhereArgs = savedInstanceState.getStringArray(STATE_WHERE_ARGS);
            mExportMode = savedInstanceState.getBoolean(STATE_EXPORT_MODE, mExportMode);
        }

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        mSortField = prefs.getString(FlavordexApp.PREF_LIST_SORT_FIELD, mSortField);
        mSortReversed = prefs.getBoolean(FlavordexApp.PREF_LIST_SORT_REVERSED, mSortReversed);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setupToolbar();
        registerForContextMenu(getListView());

        updateFilterToolbar(false);
        updateEmptyText();

        mAdapter = new EntryListAdapter(getContext());
        setListShown(false);
        setListAdapter(mAdapter);

        setExportMode(mExportMode, false);

        getLoaderManager().initLoader(0, null, this);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_entry_list, container, false);

        final FrameLayout list = (FrameLayout)root.findViewById(R.id.list);
        list.addView(super.onCreateView(inflater, container, savedInstanceState));

        return root;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        if(mExportMode) {
            invalidateExportMenu();
            return;
        }
        mActivatedItem = id;
        final Cursor cursor = (Cursor)mAdapter.getItem(position);
        final String catName = cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT));
        final long catId = cursor.getLong(cursor.getColumnIndex(Tables.Entries.CAT_ID));
        ((EntryListActivity)getActivity()).onItemSelected(id, catName, catId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_ITEM, mActivatedItem);
        outState.putString(STATE_SEARCH, mSearchQuery);
        outState.putParcelable(STATE_FILTERS, mFilters);
        outState.putString(STATE_FILTER_TEXT, mFilterText);
        outState.putString(STATE_WHERE, mWhere);
        outState.putStringArray(STATE_WHERE_ARGS, mWhereArgs);
        outState.putBoolean(STATE_EXPORT_MODE, mExportMode);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
        if(mToolbar == null) {
            inflater.inflate(R.menu.entry_list_menu, menu);
            setupMenu(menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean showFileXport = Environment.getExternalStorageDirectory().canWrite()
                || PermissionUtils.shouldAskExternalStoragePerm(getActivity());
        final boolean showAppImport = AppImportUtils.isAnyAppInstalled(getContext());
        menu.findItem(R.id.menu_xport).setVisible(showFileXport || showAppImport);
        menu.findItem(R.id.menu_import).setVisible(showFileXport);
        menu.findItem(R.id.menu_export).setVisible(showFileXport);
        menu.findItem(R.id.menu_import_app).setVisible(showAppImport);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_filter:
                EntryFilterDialog.showDialog(getFragmentManager(), this, REQUEST_SET_FILTERS,
                        mFilters);
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
                CatListDialog.showDialog(getFragmentManager(), this, REQUEST_SELECT_CAT, true);
                return true;
            case R.id.menu_import:
                if(!PermissionUtils.checkExternalStoragePerm(getActivity(),
                        R.string.message_request_storage_xport)) {
                    return true;
                }
                final String rootPath = Environment.getExternalStorageDirectory().getPath();
                FileSelectorDialog.showDialog(getFragmentManager(), this, REQUEST_IMPORT_FILE,
                        rootPath, false, ".csv");
                return true;
            case R.id.menu_import_app:
                AppChooserDialog.showDialog(getFragmentManager(), false);
                return true;
            case R.id.menu_export:
                if(!PermissionUtils.checkExternalStoragePerm(getActivity(),
                        R.string.message_request_storage_xport)) {
                    return true;
                }
                if(!mExportMode) {
                    setExportMode(true, true);
                }
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.entry_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        final Cursor cursor = (Cursor)mAdapter.getItem(info.position);
        switch(item.getItemId()) {
            case R.id.menu_share:
                if(cursor != null) {
                    final String title =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                    final float rating =
                            cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
                    final Intent shareIntent =
                            EntryUtils.getShareIntent(getContext(), title, rating);
                    if(shareIntent != null) {
                        startActivity(shareIntent);
                    }
                }
                return true;
            case R.id.menu_edit_entry:
                EditEntryActivity.startActivity(getContext(), info.id,
                        cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT)));
                return true;
            case R.id.menu_delete_entry:
                if(cursor != null) {
                    final String title =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                    final Intent deleteIntent = new Intent();
                    deleteIntent.putExtra(EXTRA_ENTRY_ID, info.id);
                    ConfirmationDialog.showDialog(getFragmentManager(), this, REQUEST_DELETE_ENTRY,
                            getString(R.string.title_delete_entry),
                            getString(R.string.message_confirm_delete, title), R.drawable.ic_delete,
                            deleteIntent);
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_SET_FILTERS:
                    setFilters(data);
                    break;
                case REQUEST_SELECT_CAT:
                    final Intent addIntent = AddEntryActivity.getIntent(getContext(),
                            data.getLongExtra(CatListDialog.EXTRA_CAT_ID, 0),
                            data.getStringExtra(CatListDialog.EXTRA_CAT_NAME));
                    startActivityForResult(addIntent, REQUEST_ADD_ENTRY);
                    break;
                case REQUEST_ADD_ENTRY:
                    CatListDialog.closeDialog(getFragmentManager());
                    final long entryId = data.getLongExtra(AddEntryActivity.EXTRA_ENTRY_ID, 0);
                    if(entryId > 0) {
                        mActivatedItem = entryId;
                        ((EntryListActivity)getActivity()).onItemSelected(entryId,
                                data.getStringExtra(AddEntryActivity.EXTRA_ENTRY_CAT),
                                data.getLongExtra(AddEntryActivity.EXTRA_ENTRY_CAT_ID, 0));
                    }
                    break;
                case REQUEST_IMPORT_FILE:
                    FileImportDialog.showDialog(getFragmentManager(),
                            data.getStringExtra(FileSelectorDialog.EXTRA_PATH));
                    break;
                case REQUEST_DELETE_ENTRY:
                    final long id = data.getLongExtra(EXTRA_ENTRY_ID, 0);
                    if(mTwoPane && id == mActivatedItem) {
                        clearSelection();
                    }
                    new EntryDeleter(getContext(), id).execute();
                    break;
            }
        }
    }

    /**
     * Set up the list Toolbar if it exists.
     */
    private void setupToolbar() {
        mToolbar = (Toolbar)getActivity().findViewById(R.id.list_toolbar);
        if(mToolbar != null) {
            mToolbar.inflateMenu(R.menu.entry_list_menu);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            setupMenu(mToolbar.getMenu());
        }
    }

    /**
     * Set up the list menu.
     *
     * @param menu A Menu from the ActionBar or Toolbar
     */
    private void setupMenu(Menu menu) {
        if(menu == null) {
            return;
        }

        setupSearch(menu.findItem(R.id.menu_search));
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
     * Set up the search bar.
     *
     * @param searchItem The search action item
     */
    private void setupSearch(MenuItem searchItem) {
        if(searchItem == null) {
            return;
        }

        final SearchView searchView = (SearchView)searchItem.getActionView();
        searchView.setQueryHint(getText(R.string.menu_search));

        if(!TextUtils.isEmpty(mSearchQuery)) {
            searchItem.expandActionView();
            searchView.setQuery(mSearchQuery, false);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                setSearchQuery(newText);
                return false;
            }
        });
    }

    /**
     * Set the list searchQuery.
     *
     * @param searchQuery The searchQuery
     */
    private void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;
        updateEmptyText();
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Set the filter parameters from the result from the filter dialog.
     *
     * @param filterData The Intent returned by EntryFilterDialog
     */
    private void setFilters(Intent filterData) {
        if(filterData == null) {
            mFilters = null;
            mFilterText = null;
            mWhere = null;
            mWhereArgs = null;
        } else {
            mFilters = filterData.getParcelableExtra(EntryFilterDialog.EXTRA_FILTER_VALUES);
            mFilterText = filterData.getStringExtra(EntryFilterDialog.EXTRA_FIELDS_LIST);
            mWhere = filterData.getStringExtra(EntryFilterDialog.EXTRA_SQL_WHERE);
            mWhereArgs = filterData.getStringArrayExtra(EntryFilterDialog.EXTRA_SQL_ARGS);
        }
        updateFilterToolbar(true);
        updateEmptyText();
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Set the field to sort the list by. If the field is the same as the current field, the order
     * is reversed.
     *
     * @param field The name of the database column to sort by
     */
    private void setSort(String field) {
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
     * Enable or disable export mode.
     *
     * @param exportMode Whether to enable export mode
     */
    private void setExportMode(boolean exportMode, boolean animate) {
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
                    android.support.v7.appcompat.R.anim.abc_slide_in_bottom);
            mExportOutAnimation = AnimationUtils.loadAnimation(getContext(),
                    android.support.v7.appcompat.R.anim.abc_slide_out_bottom);
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
     * Update the state of the filter Toolbar.
     */
    private void updateFilterToolbar(boolean animate) {
        if(mFilters == null || mFilters.size() == 0) {
            showFilterToolbar(false, animate);
            setSubtitle(getString(R.string.title_all_entries));
        } else {
            if(mFilters.containsKey(Tables.Entries.CAT)) {
                setSubtitle(getString(R.string.title_cat_entries,
                        mFilters.get(Tables.Entries.CAT)));
            } else {
                setSubtitle(getString(R.string.title_all_entries));
            }
            showFilterToolbar(true, animate);
            mFilterToolbar.setTitle(getString(R.string.message_active_filters, mFilterText));
        }
    }

    /**
     * Show or hide the filter Toolbar.
     *
     * @param show Whether to show the filter Toolbar
     */
    private void showFilterToolbar(boolean show, boolean animate) {
        if(mFilterToolbarShowing == show) {
            return;
        }

        if(mFilterToolbar == null) {
            mFilterToolbar = (Toolbar)getActivity().findViewById(R.id.filter_toolbar);
            mFilterToolbar.inflateMenu(R.menu.filters_menu);
            mFilterToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_clear:
                            setFilters(null);
                            return true;
                    }
                    return false;
                }
            });

            mFilterInAnimation = AnimationUtils.loadAnimation(getContext(),
                    android.support.v7.appcompat.R.anim.abc_slide_in_top);
            mFilterOutAnimation = AnimationUtils.loadAnimation(getContext(),
                    android.support.v7.appcompat.R.anim.abc_slide_out_top);
        }

        mFilterToolbar.setVisibility(show ? View.VISIBLE : View.GONE);
        if(animate) {
            mFilterToolbar.startAnimation(show ? mFilterInAnimation : mFilterOutAnimation);
        }

        mFilterToolbarShowing = show;
    }

    /**
     * Set the text shown when the list is empty based on any filters that are set.
     */
    private void updateEmptyText() {
        CharSequence emptyText;
        if(TextUtils.isEmpty(mSearchQuery)) {
            emptyText = getText(R.string.message_no_data);
        } else {
            emptyText = Html.fromHtml(getString(R.string.message_no_data_filter, mSearchQuery));
        }
        setEmptyText(emptyText);
    }

    /**
     * Set the subtitle. This will will display as the title of the list Toolbar or as the ActionBar
     * subtitle.
     *
     * @param subtitle The subtitle string
     */
    private void setSubtitle(CharSequence subtitle) {
        if(mToolbar != null) {
            mToolbar.setTitle(subtitle);
        } else {
            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.setSubtitle(subtitle);
            }
        }
    }

    /**
     * Enable or disable two-pane mode.
     *
     * @param twoPane Whether the Activity is in two-pane mode
     */
    public void setTwoPane(boolean twoPane) {
        mTwoPane = twoPane;
        setActivateOnItemClick(twoPane);
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
     * Set the selected list item.
     *
     * @param position The index of the item to activate
     */
    private void setActivatedPosition(int position) {
        if(position != ListView.INVALID_POSITION && !mExportMode) {
            getListView().setItemChecked(position, true);
        }
    }

    /**
     * Clear the selected list item.
     */
    public void clearSelection() {
        final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setSubtitle(null);
        }
        getListView().setItemChecked(mAdapter.getItemIndex(mActivatedItem), false);
        mActivatedItem = -1;
        ((EntryListActivity)getActivity()).onItemSelected(mActivatedItem, null, 0);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        if(TextUtils.isEmpty(mSearchQuery)) {
            uri = Tables.Entries.CONTENT_URI;
        } else {
            uri = Uri.withAppendedPath(Tables.Entries.CONTENT_FILTER_URI_BASE, mSearchQuery);
        }
        final String sort = mSortField + (mSortReversed ? " DESC" : " ASC");
        return new CursorLoader(getContext(), uri, LIST_PROJECTION, mWhere, mWhereArgs, sort);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setActivatedPosition(ListView.INVALID_POSITION);
        mAdapter.swapCursor(null);
    }
}
