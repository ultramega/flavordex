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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.CatListAdapter;

/**
 * Base class for the Dialog to select a category.
 *
 * @author Steve Guidetti
 */
public class BaseCatListDialog extends DialogFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CatListDialog";

    /**
     * Keys for the result data Intent
     */
    public static final String EXTRA_CAT_ID = "cat_id";
    public static final String EXTRA_CAT_NAME = "cat_name";

    /**
     * The Adapter backing the list
     */
    protected CatListAdapter mAdapter;

    /**
     * Show the dialog.
     *
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to notify of the result
     * @param requestCode A number to identify this request
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode) {
        final DialogFragment fragment = new CatListDialog();
        fragment.setTargetFragment(target, requestCode);

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
        final ListView listView = getLayout();
        listView.setAdapter(mAdapter);
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_select_cat)
                .setIcon(R.drawable.ic_list)
                .setView(listView)
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
    protected ListView getLayout() {
        final ListView listView = new ListView(getContext());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onCatSelected(position, id);
            }
        });

        return listView;
    }

    /**
     * Called when a list item is selected.
     *
     * @param position The position index of the item
     * @param id       The ID of the item
     */
    protected void onCatSelected(int position, long id) {
        final Fragment target = getTargetFragment();
        if(target != null) {
            final Intent intent = new Intent();
            intent.putExtra(EXTRA_CAT_ID, id);
            intent.putExtra(EXTRA_CAT_NAME, mAdapter.getItem(position).name);
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
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
