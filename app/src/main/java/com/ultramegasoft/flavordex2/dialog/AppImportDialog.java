/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

import java.lang.ref.WeakReference;

/**
 * Dialog for importing journal entries from the original Flavordex apps.
 *
 * @author Steve Guidetti
 */
public class AppImportDialog extends ImportDialog implements LoaderManager.LoaderCallbacks<Cursor> {
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
    public static void showDialog(@NonNull FragmentManager fm, int app) {
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
        if(args != null) {
            mApp = args.getInt(ARG_APP);
        }

        final Context context = getContext();
        if(context == null || !AppImportUtils.isAppInstalled(context, mApp, true)) {
            dismiss();
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void insertSelected() {
        final FragmentManager fm = getFragmentManager();
        if(fm != null) {
            ImporterFragment.init(fm, mApp, getListView().getCheckedItemIds());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Context context = getContext();
        if(context == null) {
            return null;
        }

        setListShown(false);
        final Uri uri = AppImportUtils.getEntriesUri(mApp);
        return new CursorLoader(context, uri, LIST_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final Context context = getContext();
        if(context == null) {
            return;
        }

        setListShown(true);
        final EntryListAdapter adapter = new EntryListAdapter(context);
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
        public static void init(@NonNull FragmentManager fm, int app, @NonNull long[] entryIds) {
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
            if(args != null) {
                mApp = args.getInt(ARG_APP);
                mEntryIds = args.getLongArray(ARG_ENTRY_IDS);
            }
        }

        @NonNull
        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
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
            final Context context = getContext();
            if(context != null) {
                new ImportTask(context, this, mApp, mEntryIds).execute();
            }
        }

        /**
         * Task for importing entries in the background.
         */
        private static class ImportTask extends AsyncTask<Void, Integer, Void> {
            /**
             * The Context reference
             */
            @NonNull
            private final WeakReference<Context> mContext;

            /**
             * The Fragment
             */
            @NonNull
            private final ImporterFragment mFragment;

            /**
             * The source app
             */
            private final int mApp;

            /**
             * The list of source entry IDs to import
             */
            @NonNull
            private final long[] mEntryIds;

            /**
             * @param context  The Context
             * @param fragment The Fragment
             * @param app      The source app
             * @param entryIds The list of source entry IDs to import
             */
            ImportTask(@NonNull Context context, @NonNull ImporterFragment fragment, int app,
                       @NonNull long[] entryIds) {
                mContext = new WeakReference<>(context.getApplicationContext());
                mFragment = fragment;
                mApp = app;
                mEntryIds = entryIds;
            }

            @Override
            protected Void doInBackground(Void... params) {
                final Context context = mContext.get();
                if(context == null) {
                    return null;
                }

                EntryHolder entry;
                int i = 0;
                for(long id : mEntryIds) {
                    entry = AppImportUtils.importEntry(context, mApp, id);
                    try {
                        EntryUtils.insertEntry(context, entry);
                    } catch(SQLiteException e) {
                        Log.e(TAG, "Failed to insert entry: " + entry.title, e);
                    }
                    publishProgress(++i);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);

                final ProgressDialog dialog = (ProgressDialog)mFragment.getDialog();
                if(dialog != null) {
                    dialog.setProgress(values[0]);
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                final Context context = mContext.get();
                if(context != null) {
                    Toast.makeText(context, R.string.message_import_complete, Toast.LENGTH_LONG)
                            .show();
                }

                mFragment.dismiss();
            }
        }
    }
}
