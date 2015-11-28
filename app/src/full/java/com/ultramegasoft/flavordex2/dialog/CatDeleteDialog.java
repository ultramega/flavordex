package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

/**
 * Dialog for confirming the deletion of a category. This also handles the deleting of the
 * category.
 *
 * @author Steve Guidetti
 */
public class CatDeleteDialog extends DialogFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CatDeleteDialog";

    /**
     * Arguments for the Fragment
     */
    private static final String ARG_CAT_ID = "cat_id";

    /**
     * Keys for the saved state
     */
    private static final String STATE_SHOW_CHECK = "show_check";

    /**
     * Views from the layout
     */
    private LinearLayout mCheckLayout;
    private TextView mTxtMessage;
    private TextView mTxtCheckEntries;
    private CheckBox mCheckBox;

    /**
     * Whether to show the check layout
     */
    private boolean mShowCheckLayout = true;

    /**
     * The database ID for the category
     */
    private long mCatId;

    /**
     * Show the confirmation dialog to delete a category.
     *
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to send the result to
     * @param requestCode The code to identify the request
     * @param catId       The database ID for the category
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode,
                                  long catId) {
        if(catId > 0) {
            final DialogFragment fragment = new CatDeleteDialog();
            fragment.setTargetFragment(target, requestCode);

            final Bundle args = new Bundle();
            args.putLong(ARG_CAT_ID, catId);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCatId = getArguments().getLong(ARG_CAT_ID);
        return new AlertDialog.Builder(getContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.title_delete_cat)
                .setView(getLayout(savedInstanceState))
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Fragment target = getTargetFragment();
                        if(target != null) {
                            target.onActivityResult(getTargetRequestCode(),
                                    Activity.RESULT_OK, null);
                        }
                        new CatDeleteTask(getContext(), mCatId).execute();
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                })
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        setButtonEnabled(mCheckBox.isChecked() || !mShowCheckLayout);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHOW_CHECK, mShowCheckLayout);
    }

    /**
     * Set the enabled status of the positive button.
     *
     * @param enabled Whether to enable the button
     */
    private void setButtonEnabled(boolean enabled) {
        ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
    }

    /**
     * Set the display of the check layout.
     *
     * @param showCheckLayout Whether to show the check layout
     */
    private void setShowCheckLayout(boolean showCheckLayout) {
        mCheckLayout.setVisibility(showCheckLayout ? View.VISIBLE : View.GONE);
        mShowCheckLayout = showCheckLayout;
    }

    /**
     * Get the inner layout of the dialog.
     *
     * @param savedInstanceState The saved state
     * @return The View to place in the dialog
     */
    @SuppressLint("InflateParams")
    private View getLayout(Bundle savedInstanceState) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View root = inflater.inflate(R.layout.dialog_delete_cat, null);

        mCheckLayout = (LinearLayout)root.findViewById(R.id.check_entries);
        mTxtMessage = (TextView)root.findViewById(R.id.message);
        mTxtMessage.setFreezesText(true);

        mCheckBox = (CheckBox)root.findViewById(R.id.checkbox);
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setButtonEnabled(isChecked);
            }
        });

        mTxtCheckEntries = (TextView)root.findViewById(R.id.check_message);
        mTxtCheckEntries.setFreezesText(true);
        mTxtCheckEntries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCheckBox.toggle();
            }
        });

        if(savedInstanceState == null) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            setShowCheckLayout(savedInstanceState.getBoolean(STATE_SHOW_CHECK));
        }

        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
        return new CursorLoader(getContext(), uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()) {
            final String name = data.getString(data.getColumnIndex(Tables.Cats.NAME));
            final String message = getString(R.string.message_confirm_delete_cat, name);
            final int count = data.getInt(data.getColumnIndex(Tables.Cats.NUM_ENTRIES));

            mTxtMessage.setText(Html.fromHtml(message));
            if(count > 0) {
                final String entries = getResources().getQuantityString(R.plurals.entries, count);
                mTxtCheckEntries.setText(Html.fromHtml(
                        getString(R.string.message_delete_cat_entries, count, entries)));
                setButtonEnabled(false);
            } else {
                setShowCheckLayout(false);
                setButtonEnabled(true);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Task for deleting the category in the background.
     */
    private static class CatDeleteTask extends AsyncTask<Void, Void, Void> {
        /**
         * The Context
         */
        private final Context mContext;

        /**
         * The category database ID
         */
        private final long mCatId;

        /**
         * @param context The Context
         * @param catId   The category database ID
         */
        public CatDeleteTask(Context context, long catId) {
            mContext = context.getApplicationContext();
            mCatId = catId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final ContentResolver cr = mContext.getContentResolver();

            Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_CAT_URI_BASE, mCatId);
            final Cursor cursor = cr.query(uri, new String[] {Tables.Entries._ID}, null, null, null);
            if(cursor != null) {
                try {
                    while(cursor.moveToNext()) {
                        PhotoUtils.deleteThumb(mContext, cursor.getLong(0));
                    }
                } finally {
                    cursor.close();
                }
            }

            uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
            cr.delete(uri, null, null);
            cr.notifyChange(Tables.Entries.CONTENT_URI, null);
            BackendUtils.requestDataSync(mContext);
            return null;
        }
    }
}
