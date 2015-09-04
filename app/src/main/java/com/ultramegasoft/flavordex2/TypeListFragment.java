package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.dialog.DeleteTypeDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryTypeAdapter;

/**
 * Fragment for showing a list of type selections when adding a new entry
 *
 * @author Steve Guidetti
 */
public class TypeListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Request coded for external activities
     */
    private static final int REQUEST_ADD_TYPE = 100;

    /**
     * The adapter backing the list
     */
    private EntryTypeAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.title_add);
        actionBar.setSubtitle(R.string.title_select_type);
        setListShown(false);
        registerForContextMenu(getListView());
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectType(id);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.type_select_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_add_type:
                final Intent intent = new Intent(getActivity(), EditTypeActivity.class);
                startActivityForResult(intent, REQUEST_ADD_TYPE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.type_context_menu, menu);

        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)menuInfo;
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
                final Intent intent = new Intent(getContext(), EditTypeActivity.class);
                intent.putExtra(EditTypeActivity.EXTRA_TYPE_ID, info.id);
                startActivity(intent);
                return true;
            case R.id.menu_delete:
                DeleteTypeDialog.showDialog(getFragmentManager(), null, 0, info.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            final long typeId = data.getLongExtra(EditTypeActivity.EXTRA_TYPE_ID, 0);
            if(typeId > 0) {
                selectType(typeId);
            }
        }
    }

    /**
     * Launch the entry creation fragment with the selected type.
     *
     * @param id The selected type id
     */
    private void selectType(long id) {
        final Fragment fragment = new AddEntryFragment();
        final Bundle args = new Bundle();
        args.putLong(AddEntryFragment.ARG_TYPE_ID, id);
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Tables.Types.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter = new EntryTypeAdapter(getActivity(), data, android.R.layout.simple_list_item_1,
                android.R.id.text1);
        setListAdapter(mAdapter);
        setListShown(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
