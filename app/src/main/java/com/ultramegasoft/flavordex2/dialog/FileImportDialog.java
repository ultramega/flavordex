package com.ultramegasoft.flavordex2.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.widget.ListView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.CSVUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.widget.CSVListAdapter;
import com.ultramegasoft.flavordex2.widget.EntryHolder;

import java.io.File;
import java.util.ArrayList;

/**
 * Dialog for importing journal entries from CSV files.
 *
 * @author Steve Guidetti
 */
public class FileImportDialog extends ImportDialog
        implements LoaderManager.LoaderCallbacks<CSVUtils.CSVHolder> {
    /**
     * Tag to identify the Fragment
     */
    private static final String TAG = "FileImportDialog";

    /**
     * Keys for the Fragment arguments
     */
    private static final String ARG_FILE_PATH = "file_path";

    /**
     * Keys for the saved state
     */
    private static final String STATE_DATA = "data";

    /**
     * The data loaded from the CSV file
     */
    private CSVUtils.CSVHolder mData;

    /**
     * The path to the selected file
     */
    private String mFilePath;

    /**
     * Show the dialog.
     *
     * @param fm       The FragmentManager to use
     * @param filePath The path to the selected file
     */
    public static void showDialog(FragmentManager fm, String filePath) {
        final DialogFragment fragment = new FileImportDialog();

        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFilePath = getArguments().getString(ARG_FILE_PATH);
        if(savedInstanceState != null) {
            mData = savedInstanceState.getParcelable(STATE_DATA);
        }

        if(mData != null) {
            setListAdapter(new CSVListAdapter(getContext(), mData));
        } else if(mFilePath != null) {
            getLoaderManager().initLoader(0, null, this).forceLoad();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DATA, mData);
    }

    @Override
    protected void insertSelected() {
        final CSVListAdapter adapter = (CSVListAdapter)getListAdapter();
        final ArrayList<EntryHolder> entries = new ArrayList<>();
        for(long i : getListView().getCheckedItemIds()) {
            entries.add(adapter.getItem((int)i));
        }
        DataSaverFragment.init(getFragmentManager(), entries);
    }

    @Override
    public Loader<CSVUtils.CSVHolder> onCreateLoader(int id, Bundle args) {
        setListShown(false);
        return new AsyncTaskLoader<CSVUtils.CSVHolder>(getContext()) {
            @Override
            public CSVUtils.CSVHolder loadInBackground() {
                return CSVUtils.importCSV(getContext(), new File(mFilePath));
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<CSVUtils.CSVHolder> loader, CSVUtils.CSVHolder data) {
        if(data != null) {
            setListShown(true);
            setListAdapter(new CSVListAdapter(getContext(), data));

            final ListView listView = getListView();
            for(int i = 0; i < data.entries.size(); i++) {
                listView.setItemChecked(i, !data.duplicates.contains(data.entries.get(i)));
            }

            final int numDuplicates = data.duplicates.size();
            if(numDuplicates > 0) {
                final String duplicates =
                        getResources().getQuantityString(R.plurals.duplicates, numDuplicates);
                final String were = getResources().getQuantityString(R.plurals.were, numDuplicates);
                final String message = getString(R.string.message_duplicates_unchecked,
                        numDuplicates, duplicates, were);
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }

            mData = data;
            invalidateButtons();
        } else {
            MessageDialog.showDialog(getFragmentManager(), getString(R.string.title_error),
                    getString(R.string.error_csv_import), R.drawable.ic_warning);
            dismiss();
        }

        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<CSVUtils.CSVHolder> loader) {
    }

    /**
     * Fragment for saving the selected entries in the background.
     */
    public static class DataSaverFragment extends DialogFragment {
        /**
         * The tag to identify this Fragment
         */
        private static final String TAG = "DataSaverFragment";

        /**
         * Keys for the Fragment arguments
         */
        private static final String ARG_ENTRIES = "entries";

        /**
         * The list of entries
         */
        private ArrayList<EntryHolder> mEntries;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm      The FragmentManager to use
         * @param entries The list of entries
         */
        public static void init(FragmentManager fm, ArrayList<EntryHolder> entries) {
            final DialogFragment fragment = new DataSaverFragment();

            final Bundle args = new Bundle();
            args.putParcelableArrayList(ARG_ENTRIES, entries);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);
            setCancelable(false);

            final Bundle args = getArguments();
            mEntries = args.getParcelableArrayList(ARG_ENTRIES);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if(savedInstanceState == null) {
                new SaveTask().execute();
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = new ProgressDialog(getContext());

            dialog.setIcon(R.drawable.ic_import);
            dialog.setTitle(R.string.title_importing);
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(mEntries.size());

            return dialog;
        }

        @Override
        public void onDestroyView() {
            final Dialog dialog = getDialog();
            if(dialog != null) {
                getDialog().setDismissMessage(null);
            }
            super.onDestroyView();
        }

        /**
         * Task for saving entries in the background.
         */
        private class SaveTask extends AsyncTask<Void, Integer, Void> {
            /**
             * The Context
             */
            private final Context mContext;

            public SaveTask() {
                mContext = getContext().getApplicationContext();
            }

            @Override
            protected Void doInBackground(Void... params) {
                final ContentResolver cr = mContext.getContentResolver();

                int i = 0;
                for(EntryHolder entry : mEntries) {
                    EntryUtils.insertEntry(cr, entry);
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
