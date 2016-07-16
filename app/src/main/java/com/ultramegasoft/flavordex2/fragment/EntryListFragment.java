package com.ultramegasoft.flavordex2.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
    protected static final String ARG_EXPORT_MODE = "export_mode";

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
    protected static final int LOADER_ENTRIES = 0;
    protected static final int LOADER_CAT = 1;

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
            Tables.Entries.DATE,
            Tables.Entries.SHARED,
            Tables.Entries.LINK
    };

    /**
     * Whether the Activity is in two-pane mode
     */
    protected boolean mTwoPane;

    /**
     * The main list Toolbar
     */
    private Toolbar mToolbar;

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
     * The current activated item if in two-pane mode
     */
    protected long mActivatedItem = -1;

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
     * The category ID
     */
    private long mCatId = 0;

    /**
     * The category name
     */
    private String mCatName;

    /**
     * Whether the list is in export selection mode
     */
    private boolean mExportMode = false;

    /**
     * The Adapter for the ListView
     */
    protected EntryListAdapter mAdapter;

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
    public static EntryListFragment getInstance(long catId, boolean twoPane, long selectedItem,
                                                boolean exportMode, String where,
                                                String[] whereArgs) {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        mCatId = args.getLong(ARG_CAT, mCatId);
        mTwoPane = args.getBoolean(ARG_TWO_PANE, mTwoPane);
        mActivatedItem = args.getLong(ARG_SELECTED_ITEM, mActivatedItem);
        mWhere = args.getString(ARG_WHERE);
        mWhereArgs = args.getStringArray(ARG_WHERE_ARGS);
        mExportMode = args.getBoolean(ARG_EXPORT_MODE, mExportMode);

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setActivateOnItemClick(mTwoPane);

        setupToolbar();
        registerForContextMenu(getListView());

        setEmptyText();

        mAdapter = new EntryListAdapter(getContext());
        setListShown(false);
        setListAdapter(mAdapter);

        setExportMode(mExportMode, false);

        getLoaderManager().initLoader(LOADER_ENTRIES, null, this);
        if(mCatId > 0) {
            getLoaderManager().initLoader(LOADER_CAT, null, this);
        } else {
            setCatName(null);
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putLong(FlavordexApp.PREF_LIST_CAT_ID, mCatId).apply();
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
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if(enter) {
            return AnimationUtils.loadAnimation(getContext(), R.anim.fragment_in_from_right);
        } else {
            return AnimationUtils.loadAnimation(getContext(), R.anim.fragment_out_to_right);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if(mExportMode) {
            invalidateExportMenu();
            return;
        }
        super.onListItemClick(listView, view, position, id);
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
                ((EntryListActivity)getActivity()).onCatSelected(-1, false);
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
                    CatListDialog.showDialog(getFragmentManager(), this, REQUEST_SELECT_CAT);
                }
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
                    final long id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
                    final String title =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                    final float rating =
                            cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
                    final String link =
                            cursor.getString(cursor.getColumnIndex(Tables.Entries.LINK));
                    EntryUtils.share(getContext(), title, rating, link);
                    if(cursor.getInt(cursor.getColumnIndex(Tables.Entries.SHARED)) == 0) {
                        EntryUtils.setShareStatus(getContext(), id, true);
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
                case REQUEST_SELECT_CAT:
                    addEntry(data.getLongExtra(CatListDialog.EXTRA_CAT_ID, 0),
                            data.getStringExtra(CatListDialog.EXTRA_CAT_NAME));
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
                case REQUEST_DELETE_ENTRY:
                    final long id = data.getLongExtra(EXTRA_ENTRY_ID, 0);
                    new EntryDeleter(getContext(), id).execute();
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
    private void addEntry(long catId, String catName) {
        final Intent addIntent = AddEntryActivity.getIntent(getContext(), catId, catName);
        startActivityForResult(addIntent, REQUEST_ADD_ENTRY);
    }

    /**
     * Set up the list Toolbar if it exists.
     */
    @SuppressLint("PrivateResource")
    private void setupToolbar() {
        mToolbar = (Toolbar)getActivity().findViewById(R.id.list_toolbar);
        if(mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            mToolbar.setNavigationContentDescription(R.string.abc_action_bar_up_description);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((EntryListActivity)getActivity()).onCatSelected(-1, false);
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
            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
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
    private void setupMenu(Menu menu) {
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
    private void setCatName(String catName) {
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
            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.setSubtitle(subtitle);
            }
        }
    }

    /**
     * Turn on activate-on-click mode. When this mode is on, list items will be given the activated
     * state when touched.
     */
    protected void setActivateOnItemClick(boolean activateOnItemClick) {
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

    /**
     * Set the selected list item.
     *
     * @param position The index of the item to activate
     */
    protected void setActivatedPosition(int position) {
        if(!mExportMode && position != ListView.INVALID_POSITION) {
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
        switch(id) {
            case LOADER_ENTRIES:
                Uri uri;
                if(mCatId > 0) {
                    uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_CAT_URI_BASE, mCatId);
                } else {
                    uri = Tables.Entries.CONTENT_URI;
                }
                final String sort = mSortField + (mSortReversed ? " DESC" : " ASC");
                return new CursorLoader(getContext(), uri, LIST_PROJECTION, mWhere, mWhereArgs,
                        sort);
            case LOADER_CAT:
                return new CursorLoader(getContext(),
                        ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId),
                        new String[] {Tables.Cats.NAME}, null, null, null);
        }
        return null;
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
                            getFragmentManager().popBackStack();
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch(loader.getId()) {
            case LOADER_ENTRIES:
                setActivatedPosition(ListView.INVALID_POSITION);
                mAdapter.swapCursor(null);
                break;
        }
    }
}
