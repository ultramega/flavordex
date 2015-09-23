package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
public class ImportDialog extends DialogFragment
        implements LoaderManager.LoaderCallbacks<CSVUtils.CSVHolder> {
    /**
     * Tag to identify the Fragment
     */
    private static final String TAG = "ImportDialog";

    /**
     * Keys for the Fragment arguments
     */
    private static final String ARG_FILE_PATH = "file_path";

    /**
     * Keys for the saved state
     */
    private static final String STATE_DATA = "data";

    /**
     * Views from the layout
     */
    private FrameLayout mListContainer;
    private ListView mListView;
    private ProgressBar mProgressBar;

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
        final DialogFragment fragment = new ImportDialog();

        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFilePath = getArguments().getString(ARG_FILE_PATH);
        if(savedInstanceState != null) {
            mData = savedInstanceState.getParcelable(STATE_DATA);
        }
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View root = LayoutInflater.from(getContext()).inflate(R.layout.list_dialog, null);

        mListContainer = (FrameLayout)root.findViewById(R.id.list_container);

        mListView = (ListView)root.findViewById(R.id.list);
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                invalidateButtons();
            }
        });

        final TextView emptyView = (TextView)root.findViewById(R.id.empty);
        emptyView.setText(R.string.message_import_no_data);
        mListView.setEmptyView(emptyView);

        mProgressBar = (ProgressBar)root.findViewById(R.id.progress);

        if(mData != null) {
            mListView.setAdapter(new CSVListAdapter(getContext(), mData));
        } else if(mFilePath != null) {
            getLoaderManager().initLoader(0, null, this).forceLoad();
        }

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_import)
                .setIcon(R.drawable.ic_import)
                .setView(root)
                .setPositiveButton(R.string.button_import, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        insertSelected();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        invalidateButtons();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DATA, mData);
    }

    /**
     * Update the status of the dialog buttons.
     */
    private void invalidateButtons() {
        final AlertDialog dialog = (AlertDialog)getDialog();
        final boolean itemSelected = mListView.getCheckedItemCount() > 0;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(itemSelected);
    }

    /**
     * Insert the selected entries into the database.
     */
    private void insertSelected() {
        final CSVListAdapter adapter = (CSVListAdapter)mListView.getAdapter();
        final ArrayList<EntryHolder> entries = new ArrayList<>();
        for(long i : mListView.getCheckedItemIds()) {
            entries.add(adapter.getItem((int)i));
        }
        DataSaverFragment.init(getFragmentManager(), entries);
    }

    @Override
    public Loader<CSVUtils.CSVHolder> onCreateLoader(int id, Bundle args) {
        mListContainer.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
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
            mProgressBar.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
            mListView.setAdapter(new CSVListAdapter(getContext(), data));

            for(int i = 0; i < data.entries.size(); i++) {
                mListView.setItemChecked(i, !data.duplicates.contains(data.entries.get(i)));
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
        public void onStart() {
            super.onStart();
            new SaveTask().execute();
        }

        /**
         * Task for saving entries in the background.
         */
        private class SaveTask extends AsyncTask<Void, Integer, Void> {
            /**
             * The ContentResolver to use for inserting data
             */
            private final ContentResolver mResolver;

            public SaveTask() {
                mResolver = getContext().getContentResolver();
            }

            @Override
            protected Void doInBackground(Void... params) {
                int i = 0;
                for(EntryHolder entry : mEntries) {
                    EntryUtils.insertEntry(mResolver, entry);
                    publishProgress(++i);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                ((ProgressDialog)getDialog()).setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Void result) {
                Toast.makeText(getContext(), R.string.message_import_complete, Toast.LENGTH_LONG)
                        .show();
                dismiss();
            }
        }
    }
}
