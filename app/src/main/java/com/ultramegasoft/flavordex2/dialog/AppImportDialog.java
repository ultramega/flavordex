package com.ultramegasoft.flavordex2.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.AppImportUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.EntryListAdapter;

/**
 * Dialog for importing journal entries from the original Flavordex apps.
 *
 * @author Steve Guidetti
 */
public class AppImportDialog extends ImportDialog implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Tag to identify the Fragment
     */
    private static final String TAG = "AppImportDialog";

    /**
     * Keys for the Fragment arguments
     */
    private static final String ARG_APP = "app";

    /**
     * The fields to query from the source database
     */
    private static final String[] LIST_PROJECTION = new String[] {
            AppImportUtils.EntriesColumns._ID,
            AppImportUtils.EntriesColumns.TITLE,
            AppImportUtils.EntriesColumns.MAKER,
            AppImportUtils.EntriesColumns.RATING,
            AppImportUtils.EntriesColumns.DATE
    };

    /**
     * The source app
     */
    private int mApp;

    /**
     * Show the dialog.
     *
     * @param fm  The FragmentManager to use
     * @param app The source app
     */
    public static void showDialog(FragmentManager fm, int app) {
        final DialogFragment fragment = new AppImportDialog();

        final Bundle args = new Bundle();
        args.putInt(ARG_APP, app);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Bundle args = getArguments();
        mApp = args.getInt(ARG_APP);

        if(!AppImportUtils.isAppInstalled(getContext(), mApp, true)) {
            dismiss();
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void insertSelected() {
        ImporterFragment.init(getFragmentManager(), mApp, getListView().getCheckedItemIds());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setListShown(false);
        final Uri uri = AppImportUtils.getEntriesUri(mApp);
        return new CursorLoader(getContext(), uri, LIST_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setListShown(true);
        final EntryListAdapter adapter = new EntryListAdapter(getContext());
        adapter.setMultiChoiceMode(true);
        adapter.swapCursor(data);
        setListAdapter(adapter);

        final ListView listView = getListView();
        for(int i = 0; i < adapter.getCount(); i++) {
            listView.setItemChecked(i, true);
        }

        invalidateButtons();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        final CursorAdapter adapter = (CursorAdapter)getListAdapter();
        if(adapter != null) {
            adapter.swapCursor(null);
        }
    }

    /**
     * Fragment for importing the selected entries in the background.
     */
    public static class ImporterFragment extends BackgroundProgressDialog {
        /**
         * The tag to identify this Fragment
         */
        private static final String TAG = "ImporterFragment";

        /**
         * Keys for the Fragment arguments
         */
        private static final String ARG_APP = "app";
        private static final String ARG_ENTRY_IDS = "entry_ids";

        /**
         * The source app
         */
        private int mApp;

        /**
         * The list of source entry IDs to import
         */
        private long[] mEntryIds;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm       The FragmentManager to use
         * @param app      The source app
         * @param entryIds The list of source entry IDs to import
         */
        public static void init(FragmentManager fm, int app, long[] entryIds) {
            final DialogFragment fragment = new ImporterFragment();

            final Bundle args = new Bundle();
            args.putInt(ARG_APP, app);
            args.putLongArray(ARG_ENTRY_IDS, entryIds);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            mApp = args.getInt(ARG_APP);
            mEntryIds = args.getLongArray(ARG_ENTRY_IDS);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = new ProgressDialog(getContext());

            dialog.setIcon(R.drawable.ic_import);
            dialog.setTitle(R.string.title_importing);
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(mEntryIds.length);

            return dialog;
        }

        @Override
        protected void startTask() {
            new ImportTask().execute();
        }

        /**
         * Task for importing entries in the background.
         */
        private class ImportTask extends AsyncTask<Void, Integer, Void> {
            /**
             * The Context
             */
            private final Context mContext;

            public ImportTask() {
                mContext = getContext().getApplicationContext();
            }

            @Override
            protected Void doInBackground(Void... params) {
                EntryHolder entry;
                int i = 0;
                for(long id : mEntryIds) {
                    entry = AppImportUtils.importEntry(mContext, mApp, id);
                    try {
                        EntryUtils.insertEntry(mContext, entry);
                    } catch(SQLiteException e) {
                        Log.e(getClass().getSimpleName(), e.getMessage());
                    }
                    publishProgress(++i);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                final ProgressDialog dialog = (ProgressDialog)getDialog();
                if(dialog != null) {
                    dialog.setProgress(values[0]);
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                Toast.makeText(mContext, R.string.message_import_complete, Toast.LENGTH_LONG)
                        .show();
                dismiss();
            }
        }
    }
}
