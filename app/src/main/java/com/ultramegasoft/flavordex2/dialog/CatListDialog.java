package com.ultramegasoft.flavordex2.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.EditCatActivity;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.CatListAdapter;

/**
 * Dialog to select a category.
 *
 * @author Steve Guidetti
 */
public class CatListDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CatListDialog";

    /**
     * Arguments for the Fragment
     */
    public static final String ARG_CONTEXT_MENU = "context_menu";

    /**
     * Keys for the result data Intent
     */
    public static final String EXTRA_CAT_ID = "cat_id";
    public static final String EXTRA_CAT_NAME = "cat_name";

    /**
     * The Adapter backing the list
     */
    private CatListAdapter mAdapter;

    /**
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to notify of the result
     * @param requestCode A number to identify this request
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode) {
        showDialog(fm, target, requestCode, false);
    }

    /**
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to notify of the result
     * @param requestCode A number to identify this request
     * @param contextMenu Whether to enable the context menu
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode,
                                  boolean contextMenu) {
        final DialogFragment fragment = new CatListDialog();
        fragment.setTargetFragment(target, requestCode);

        final Bundle args = new Bundle();
        args.putBoolean(ARG_CONTEXT_MENU, contextMenu);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    /**
     * Close the dialog.
     *
     * @param fm The FragmentManager to use
     */
    public static void closeDialog(FragmentManager fm) {
        final DialogFragment fragment = (DialogFragment)fm.findFragmentByTag(TAG);
        if(fragment != null) {
            fragment.dismiss();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new CatListAdapter(getContext(), null, android.R.layout.simple_list_item_1);
        getLoaderManager().initLoader(0, null, this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_select_cat)
                .setIcon(R.drawable.ic_list)
                .setView(getLayout())
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
    }

    /**
     * Get the layout for the Dialog.
     *
     * @return The View to place inside the Dialog
     */
    private View getLayout() {
        final ListView listView = new ListView(getContext());

        listView.addFooterView(LayoutInflater.from(getContext())
                .inflate(R.layout.cat_add_list_item, listView, false));
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == mAdapter.getCount()) {
                    startActivity(new Intent(getContext(), EditCatActivity.class));
                } else {
                    final Fragment target = getTargetFragment();
                    if(target != null) {
                        final Intent intent = new Intent();
                        intent.putExtra(EXTRA_CAT_ID, id);
                        intent.putExtra(EXTRA_CAT_NAME, mAdapter.getItem(position).name);
                        target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                    }
                }
            }
        });

        if(getArguments().getBoolean(ARG_CONTEXT_MENU, false)) {
            registerForContextMenu(listView);
        }

        return listView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)menuInfo;
        if(info.position == mAdapter.getCount()) {
            return;
        }

        getActivity().getMenuInflater().inflate(R.menu.cat_context_menu, menu);

        final MenuItem edit = menu.findItem(R.id.menu_edit);
        final MenuItem delete = menu.findItem(R.id.menu_delete);

        final MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onContextItemSelected(item);
            }
        };
        edit.setOnMenuItemClickListener(listener);
        delete.setOnMenuItemClickListener(listener);

        if(mAdapter.getItem(info.position).preset) {
            delete.setEnabled(false).setVisible(false);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        final Fragment target = getTargetFragment();
        if(target != null) {
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
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
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
