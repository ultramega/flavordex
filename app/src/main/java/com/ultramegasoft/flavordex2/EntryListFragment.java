package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryListAdapter;

/**
 * The main entry list fragment. Shows a list of all the journal entries.
 *
 * @author Steve Guidetti
 */
public class EntryListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
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
     * Keys for the saved state
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String STATE_FILTER = "filter";

    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * The string to search for in the list query
     */
    private String mFilter;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        registerForContextMenu(getListView());

        setListShown(false);

        setListAdapter(new EntryListAdapter(getActivity()));

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(savedInstanceState != null) {
            mActivatedPosition = savedInstanceState.getInt(STATE_ACTIVATED_POSITION,
                    mActivatedPosition);
            mFilter = savedInstanceState.getString(STATE_FILTER);
        }

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
        mActivatedPosition = position;
        mCallbacks.onItemSelected(id, ((EntryListAdapter)getListAdapter()).getItemType(id));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mActivatedPosition != ListView.INVALID_POSITION) {
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
        outState.putString(STATE_FILTER, mFilter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);

        setupSearch(menu.findItem(R.id.menu_filter));
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

        if(!TextUtils.isEmpty(mFilter)) {
            searchItem.expandActionView();
            searchView.setQuery(mFilter, false);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                setFilter(newText);
                return false;
            }
        });
    }

    /**
     * Set the list query filter.
     *
     * @param filter The query filter
     */
    private void setFilter(String filter) {
        mFilter = filter;
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Set the text shown when the list is empty based on any filters that are set.
     */
    private void updateEmptyText() {
        CharSequence emptyText;
        if(TextUtils.isEmpty(mFilter)) {
            emptyText = getText(R.string.message_no_data);
        } else {
            emptyText = Html.fromHtml(getString(R.string.message_no_data_filter, mFilter));
        }
        setEmptyText(emptyText);
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

    public void setActivatedPosition(int position) {
        if(position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        if(TextUtils.isEmpty(mFilter)) {
            uri = Tables.Entries.CONTENT_URI;
        } else {
            uri = Uri.withAppendedPath(Tables.Entries.CONTENT_FILTER_URI_BASE, mFilter);
        }
        return new CursorLoader(getActivity(), uri, LIST_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        updateEmptyText();
        ((EntryListAdapter)getListAdapter()).changeCursor(data);
        setActivatedPosition(mActivatedPosition);
        setListShown(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        updateEmptyText();
        ((EntryListAdapter)getListAdapter()).changeCursor(null);
    }
}
