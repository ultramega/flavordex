package com.ultramegasoft.flavordex2.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;

/**
 * Dialog for confirming the deletion of a type. This also handles the deleting of the type.
 *
 * @author Steve Guidetti
 */
public class DeleteTypeDialog extends DialogFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Tag to identify the fragment
     */
    private static final String TAG = "DeleteTypeDialog";

    /**
     * Arguments for the fragment
     */
    public static final String ARG_TYPE_ID = "type_id";

    /**
     * Loader ids
     */
    private static final int LOADER_TYPE = 0;
    private static final int LOADER_COUNT = 1;

    /**
     * Views from the layout
     */
    private LinearLayout mCheckLayout;
    private TextView mTxtMessage;
    private TextView mTxtCheckEntries;

    /**
     * The database id for the type
     */
    private long mTypeId;

    /**
     * Show the confirmation dialog to delete a type.
     *
     * @param fm          The FragmentManager to use
     * @param target      The fragment to send the result to
     * @param requestCode The code to identify the request
     * @param typeId      The database id for the type
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode,
                                  long typeId) {
        if(typeId > 0) {
            final DialogFragment fragment = new DeleteTypeDialog();
            fragment.setTargetFragment(target, requestCode);

            final Bundle args = new Bundle();
            args.putLong(ARG_TYPE_ID, typeId);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTypeId = getArguments().getLong(ARG_TYPE_ID);
        return new AlertDialog.Builder(getContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.menu_delete_type)
                .setView(getLayout(savedInstanceState))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Fragment target = getTargetFragment();
                        if(target != null) {
                            target.onActivityResult(getTargetRequestCode(),
                                    Activity.RESULT_OK, null);
                        }
                        new TypeDeleteTask(getContext().getContentResolver(), mTypeId).execute();
                        dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                })
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        setButtonEnabled(false);
    }

    /**
     * Set the enabled status of the positive button.
     *
     * @param enabled Whether to enable the button
     */
    final void setButtonEnabled(boolean enabled) {
        ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
    }

    /**
     * Get the inner layout of the dialog.
     *
     * @param savedInstanceState The saved state
     * @return The view to place in the dialog
     */
    private View getLayout(Bundle savedInstanceState) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View root = inflater.inflate(R.layout.dialog_delete_type, null);

        mCheckLayout = (LinearLayout)root.findViewById(R.id.check_entries);
        mTxtMessage = (TextView)root.findViewById(R.id.message);

        final CheckBox checkBox = (CheckBox)root.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setButtonEnabled(isChecked);
            }
        });

        mTxtCheckEntries = (TextView)root.findViewById(R.id.check_message);
        mTxtCheckEntries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBox.toggle();
            }
        });

        if(savedInstanceState == null) {
            getLoaderManager().initLoader(LOADER_TYPE, null, this);
            getLoaderManager().initLoader(LOADER_COUNT, null, this);
        }

        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_TYPE:
                final Uri uri = ContentUris.withAppendedId(Tables.Types.CONTENT_ID_URI_BASE, mTypeId);
                return new CursorLoader(getContext(), uri, null, null, null, null);
            case LOADER_COUNT:
                final String where = Tables.Entries.TYPE_ID + " = " + mTypeId;
                return new CursorLoader(getContext(), Tables.Entries.CONTENT_URI, null, where, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_TYPE:
                if(data.moveToFirst()) {
                    final String name = data.getString(data.getColumnIndex(Tables.Types.NAME));
                    mTxtMessage.setText(getString(R.string.message_confirm_delete_type, name));
                }
                break;
            case LOADER_COUNT:
                final int count = data.getCount();
                if(count > 0) {
                    final String entries =
                            getResources().getQuantityString(R.plurals.entries, count);
                    mTxtCheckEntries.setText(Html.fromHtml(
                            getString(R.string.message_delete_type_entries, count, entries)));
                } else {
                    mCheckLayout.setVisibility(View.GONE);
                    setButtonEnabled(true);
                }
                break;
        }

        getLoaderManager().destroyLoader(loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Task for deleted the type in the background.
     */
    private static class TypeDeleteTask extends AsyncTask<Void, Void, Void> {
        /**
         * The ContentResolver to use
         */
        private final ContentResolver mResolver;

        /**
         * The type database id
         */
        private final long mTypeId;

        /**
         * @param cr     The ContentResolver to use
         * @param typeId The type database id
         */
        public TypeDeleteTask(ContentResolver cr, long typeId) {
            mResolver = cr;
            mTypeId = typeId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Uri uri = ContentUris.withAppendedId(Tables.Types.CONTENT_ID_URI_BASE, mTypeId);
            mResolver.delete(uri, null, null);
            mResolver.notifyChange(Tables.Entries.CONTENT_URI, null);
            return null;
        }
    }
}
