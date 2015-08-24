package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.dialog.EntryFilterDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryListAdapter;

/**
 * The main entry list fragment. Shows a list of all the journal entries.
 *
 * @author Steve Guidetti
 */
public class EntryListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Request codes
     */
    private static final int REQUEST_SET_FILTERS = 100;

    /**
     * Keys for the saved state
     */
    private static final String STATE_SELECTED_ITEM = "selected_item";
    private static final String STATE_SEARCH = "search";
    private static final String STATE_FILTERS = "filters";
    private static final String STATE_FILTER_TEXT = "filter_text";
    private static final String STATE_WHERE = "where";
    private static final String STATE_WHERE_ARGS = "where_args";

    /**
     * The fields to query from the database
     */
    private static final String[] LIST_PROJECTION = new String[] {
            Tables.Entries._ID,
            Tables.Entries.TYPE,
            Tables.Entries.TITLE,
            Tables.Entries.MAKER,
            Tables.Entries.RATING,
            Tables.Entries.DATE
    };

    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The main list toolbar
     */
    private Toolbar mToolbar;

    /**
     * The toolbar for displaying filter settings
     */
    private Toolbar mFilterToolbar;

    /**
     * The current activated item. Only used on tablets.
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
     * The adapter for the list
     */
    private EntryListAdapter mAdapter;

    /**
     * A callback interface that all activities containing this fragment must implement. This
     * mechanism allows activities to be notified of item selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         *
         * @param id   The row id of the selected item
         * @param type The type of item selected
         */
        void onItemSelected(long id, String type);
    }

    /**
     * A dummy implementation of the Callbacks interface that does nothing. Used only when this
     * fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id, String type) {
        }
    };

    public EntryListFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        registerForContextMenu(getListView());

        setListShown(false);

        mAdapter = new EntryListAdapter(getActivity());
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_entry_list, container, false);

        final FrameLayout list = (FrameLayout)root.findViewById(R.id.list);
        list.addView(super.onCreateView(inflater, container, savedInstanceState));

        mToolbar = (Toolbar)root.findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.list_menu);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });

        setupSearch(mToolbar.getMenu().findItem(R.id.menu_search));

        mFilterToolbar = (Toolbar)root.findViewById(R.id.toolbar2);
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

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(savedInstanceState != null) {
            mActivatedItem = savedInstanceState.getLong(STATE_SELECTED_ITEM, mActivatedItem);
            mSearchQuery = savedInstanceState.getString(STATE_SEARCH);
            mFilters = savedInstanceState.getParcelable(STATE_FILTERS);
            mFilterText = savedInstanceState.getString(STATE_FILTER_TEXT);
            mWhere = savedInstanceState.getString(STATE_WHERE);
            mWhereArgs = savedInstanceState.getStringArray(STATE_WHERE_ARGS);
        }

        updateFilterToolbar();
        updateEmptyText();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        mActivatedItem = id;
        mCallbacks.onItemSelected(id, mAdapter.getItemType(id));
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_xport).setVisible(Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_filter:
                EntryFilterDialog.showDialog(getFragmentManager(), this, REQUEST_SET_FILTERS,
                        mFilters);
                return true;
            case R.id.menu_add_entry:
                // TODO: 8/14/2015 Create activity for adding entries
                return true;
            case R.id.menu_xport:
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    // TODO: 8/14/2015 Add import/export
                } else {
                    Toast.makeText(getActivity(), R.string.message_no_media, Toast.LENGTH_LONG)
                            .show();
                }
                return true;
            case R.id.menu_settings:
                // TODO: 8/14/2015 Add preferences
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_SET_FILTERS:
                    setFilters(data);
                    break;
            }
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
        searchView.setQueryHint(getText(R.string.menu_filter));

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
     * Set the list query searchQuery.
     *
     * @param searchQuery The query searchQuery
     */
    private void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;
        updateEmptyText();
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Set the filter parameters from the result from the filter dialog.
     *
     * @param filterData The intent returned by EntryFilterDialog
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
        updateFilterToolbar();
        updateEmptyText();
        getLoaderManager().restartLoader(0, null, this);
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
     * Update the state of the filter toolbar
     */
    private void updateFilterToolbar() {
        if(mFilters == null || mFilters.size() == 0) {
            mToolbar.setTitle(R.string.title_all_entries);
            mFilterToolbar.setVisibility(View.GONE);
        } else {
            if(mFilters.containsKey(Tables.Entries.TYPE)) {
                mToolbar.setTitle(getString(R.string.title_type_entries, mFilters.get(Tables.Entries.TYPE)));
            } else {
                mToolbar.setTitle(R.string.title_all_entries);
            }
            mFilterToolbar.setTitle(getString(R.string.message_active_filters, mFilterText));
            mFilterToolbar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the
     * 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    /**
     * Set the select list item.
     *
     * @param position The index of the item to activate
     */
    public void setActivatedPosition(int position) {
        if(position == ListView.INVALID_POSITION) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        } else {
            getListView().setItemChecked(position, true);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        if(TextUtils.isEmpty(mSearchQuery)) {
            uri = Tables.Entries.CONTENT_URI;
        } else {
            uri = Uri.withAppendedPath(Tables.Entries.CONTENT_FILTER_URI_BASE, mSearchQuery);
        }
        return new CursorLoader(getActivity(), uri, LIST_PROJECTION, mWhere, mWhereArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final EntryListAdapter adapter = (EntryListAdapter)getListAdapter();
        adapter.changeCursor(data);
        setActivatedPosition(adapter.getItemIndex(mActivatedItem));
        setListShown(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setActivatedPosition(ListView.INVALID_POSITION);
        mAdapter.changeCursor(null);
    }
}
