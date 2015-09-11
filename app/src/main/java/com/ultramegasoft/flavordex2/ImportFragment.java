package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.dialog.FileSelectorDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.CSVUtils;
import com.ultramegasoft.flavordex2.widget.CSVListAdapter;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;
import com.ultramegasoft.flavordex2.widget.RadarHolder;

import java.io.File;
import java.util.ArrayList;

/**
 * Fragment for importing journal entries from CSV files.
 *
 * @author Steve Guidetti
 */
public class ImportFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<CSVUtils.CSVHolder> {
    /**
     * Request codes for external Activities
     */
    private final int REQUEST_SELECT_FILE = 100;
    private final int REQUEST_INSERT = 200;

    /**
     * Keys for the saved state
     */
    private static final String STATE_DATA = "date";
    private static final String STATE_FILE_PATH = "file_path";
    private static final String STATE_IN_PROGRESS = "in_progress";

    /**
     * Views from the layout
     */
    private Button mBtnFile;

    /**
     * The data loaded from the CSV file
     */
    private CSVUtils.CSVHolder mData;

    /**
     * The path to the selected file
     */
    private String mFilePath;

    /**
     * Whether the insert operation is in progress
     */
    private boolean mInProgress;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ActivityCompat.invalidateOptionsMenu(getActivity());
            }
        });

        setEmptyText(getString(R.string.message_select_import_file));
        setListShown(true);

        if(savedInstanceState != null) {
            mData = savedInstanceState.getParcelable(STATE_DATA);
            mFilePath = savedInstanceState.getString(STATE_FILE_PATH);
            mInProgress = savedInstanceState.getBoolean(STATE_IN_PROGRESS, mInProgress);
        }

        if(mData != null) {
            setEmptyText(getString(R.string.error_csv_parse));
            setListAdapter(new CSVListAdapter(getContext(), mData));
        } else if(mFilePath != null) {
            getLoaderManager().initLoader(0, null, this).forceLoad();
        }

        if(mFilePath != null) {
            mBtnFile.setText(mFilePath);
        }

        setUiEnabled(!mInProgress);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_import, container, false);

        final FrameLayout list = (FrameLayout)root.findViewById(R.id.list);
        //noinspection ConstantConditions
        list.addView(super.onCreateView(inflater, container, savedInstanceState));

        mBtnFile = (Button)root.findViewById(R.id.file_path);
        mBtnFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileSelector();
            }
        });

        return root;
    }

    /**
     * Open the file selection dialog.
     */
    private void openFileSelector() {
        FileSelectorDialog.showDialog(getFragmentManager(), this, REQUEST_SELECT_FILE,
                Environment.getExternalStorageDirectory().getPath(), false, ".csv");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DATA, mData);
        outState.putString(STATE_FILE_PATH, mFilePath);
        outState.putBoolean(STATE_IN_PROGRESS, mInProgress);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.import_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_import).setVisible(mData != null)
                .setEnabled(!mInProgress && anyItemSelected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_import:
                insertSelected();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_SELECT_FILE:
                    mData = null;
                    mFilePath = data.getStringExtra(FileSelectorDialog.EXTRA_PATH);
                    mBtnFile.setText(mFilePath);

                    getLoaderManager().initLoader(0, null, this).forceLoad();
                    break;
                case REQUEST_INSERT:
                    mInProgress = false;
                    mData = null;
                    mFilePath = null;

                    setListAdapter(null);

                    resetUi();
                    Toast.makeText(getContext(), R.string.message_import_complete,
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    /**
     * Set the enabled state of the interface.
     *
     * @param enabled Whether to enable the interface
     */
    private void setUiEnabled(boolean enabled) {
        mBtnFile.setEnabled(enabled);
        getListView().setEnabled(enabled);
        ActivityCompat.invalidateOptionsMenu(getActivity());
    }

    /**
     * Reset the interface to its initial state.
     */
    private void resetUi() {
        setEmptyText(getString(R.string.message_select_import_file));
        mBtnFile.setText(R.string.button_select_file);
        setUiEnabled(true);
    }

    /**
     * Are any list items selected?
     *
     * @return Whether any list items are selected
     */
    private boolean anyItemSelected() {
        return getListView().getCheckedItemCount() > 0;
    }

    /**
     * Insert the selected entries into the database.
     */
    private void insertSelected() {
        mInProgress = true;
        setUiEnabled(false);

        final CSVListAdapter adapter = (CSVListAdapter)getListAdapter();
        final ArrayList<EntryHolder> entries = new ArrayList<>();
        for(long i : getListView().getCheckedItemIds()) {
            entries.add(adapter.getItem((int)i));
        }
        DataSaverFragment.init(getFragmentManager(), this, REQUEST_INSERT, entries);
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
            setListAdapter(new CSVListAdapter(getContext(), data));

            for(int i = 0; i < data.entries.size(); i++) {
                getListView().setItemChecked(i, !data.duplicates.contains(data.entries.get(i)));
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
        } else {
            setEmptyText(getString(R.string.error_csv_parse));
        }

        setListShown(true);
        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<CSVUtils.CSVHolder> loader) {
        setListAdapter(null);
        mFilePath = null;
        mBtnFile.setText(R.string.button_select_file);
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
         * Start a new instance of this Fragment.
         *
         * @param fm          The FragmentManager to use
         * @param target      The Fragment to notify of the result
         * @param requestCode A number to identify this request
         * @param entries     The list of entries
         */
        public static void init(FragmentManager fm, Fragment target, int requestCode,
                                ArrayList<EntryHolder> entries) {
            final DialogFragment fragment = new DataSaverFragment();
            fragment.setTargetFragment(target, requestCode);

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
            final ArrayList<EntryHolder> entries = args.getParcelableArrayList(ARG_ENTRIES);

            new SaveTask(entries).execute();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = new ProgressDialog(getContext());

            dialog.setIcon(R.drawable.ic_import);
            dialog.setTitle(R.string.title_importing);
            dialog.setIndeterminate(false);

            return dialog;
        }

        /**
         * Set the progress bar progress.
         *
         * @param progress The progress on a scale of 0 to 10000
         */
        private void setProgress(int progress) {
            ((ProgressDialog)getDialog()).setProgress(progress);
        }

        /**
         * Notify the target fragment that the task is complete.
         */
        private void onComplete() {
            setProgress(10000);
            final Fragment target = getTargetFragment();
            if(target != null) {
                target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            }
            dismiss();
        }

        /**
         * Task for saving entries in the background.
         */
        private class SaveTask extends AsyncTask<Void, Integer, Void> {
            /**
             * The ContentResolver to use for inserting data
             */
            private final ContentResolver mResolver;

            /**
             * The list of entries
             */
            private final ArrayList<EntryHolder> mEntries;

            /**
             * The Uri for the entry category
             */
            private Uri mCatUri;

            /**
             * Whether this is a new category
             */
            private boolean mIsCatNew;

            /**
             * @param entries The list of entries
             */
            public SaveTask(ArrayList<EntryHolder> entries) {
                mResolver = getContext().getContentResolver();
                mEntries = entries;
            }

            @Override
            protected Void doInBackground(Void... params) {
                Uri entryUri;
                int i = 0;
                for(EntryHolder entry : mEntries) {
                    entry.catId = getCatId(entry.catName);
                    mCatUri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE,
                            entry.catId);

                    entryUri = insertEntry(entry);
                    insertExtras(entryUri, entry);
                    insertFlavors(entryUri, entry);
                    insertPhotos(entryUri, entry);
                    publishProgress(++i * 10000 / mEntries.size());
                }
                return null;
            }

            /**
             * Insert the row into the entries table.
             *
             * @param entry The entry
             * @return The Uri for the new entry
             */
            private Uri insertEntry(EntryHolder entry) {
                final ContentValues values = new ContentValues();
                values.put(Tables.Entries.TITLE, entry.title);
                values.put(Tables.Entries.CAT, entry.catId);
                values.put(Tables.Entries.MAKER, entry.maker);
                values.put(Tables.Entries.ORIGIN, entry.origin);
                values.put(Tables.Entries.LOCATION, entry.location);
                values.put(Tables.Entries.DATE, entry.date);
                values.put(Tables.Entries.PRICE, entry.price);
                values.put(Tables.Entries.RATING, entry.rating);
                values.put(Tables.Entries.NOTES, entry.notes);

                return mResolver.insert(Tables.Entries.CONTENT_URI, values);
            }

            /**
             * Find the ID for a category, creating one if it doesn't exist.
             *
             * @param name The name of the category
             * @return The ID for the category
             */
            private long getCatId(String name) {
                final Uri uri = Tables.Cats.CONTENT_URI;
                final String[] projection = new String[] {Tables.Cats._ID};
                final String where = Tables.Cats.NAME + " = ?";
                final String[] whereArgs = new String[] {name};
                final Cursor cursor = mResolver.query(uri, projection, where, whereArgs, null);
                try {
                    if(cursor.moveToFirst()) {
                        mIsCatNew = false;
                        return cursor.getLong(cursor.getColumnIndex(Tables.Cats._ID));
                    }
                } finally {
                    cursor.close();
                }

                mIsCatNew = true;
                final ContentValues values = new ContentValues();
                values.put(Tables.Cats.NAME, name);
                return Long.valueOf(mResolver.insert(uri, values).getLastPathSegment());
            }

            /**
             * Insert the extra fields for the new entry.
             *
             * @param entryUri The Uri for the new entry
             * @param entry    The entry
             */
            private void insertExtras(Uri entryUri, EntryHolder entry) {
                final Uri uri = Uri.withAppendedPath(entryUri, "extras");
                final ContentValues values = new ContentValues();
                for(ExtraFieldHolder extra : entry.getExtras()) {
                    values.put(Tables.EntriesExtras.EXTRA, getExtraId(extra.name));
                    values.put(Tables.EntriesExtras.VALUE, extra.value);
                    mResolver.insert(uri, values);
                }
            }

            /**
             * Find the ID of an extra field, creating one if it doesn't exist.
             *
             * @param name The name of the field
             * @return The ID for the extra field
             */
            private long getExtraId(String name) {
                final Uri uri = Uri.withAppendedPath(mCatUri, "extras");
                final String[] projection = new String[] {Tables.Extras._ID};
                final String where = Tables.Extras.NAME + " = ?";
                final String[] whereArgs = new String[] {name};
                final Cursor cursor = mResolver.query(uri, projection, where, whereArgs, null);
                try {
                    if(cursor.moveToFirst()) {
                        return cursor.getLong(cursor.getColumnIndex(Tables.Extras._ID));
                    }
                } finally {
                    cursor.close();
                }

                final ContentValues values = new ContentValues();
                values.put(Tables.Extras.NAME, name);
                return Long.valueOf(mResolver.insert(uri, values).getLastPathSegment());
            }

            /**
             * Insert the flavors for the new entry.
             *
             * @param entryUri The Uri for the new entry
             * @param entry    The entry
             */
            private void insertFlavors(Uri entryUri, EntryHolder entry) {
                final Uri uri = Uri.withAppendedPath(entryUri, "flavor");
                final ContentValues values = new ContentValues();
                for(RadarHolder flavor : entry.getFlavors()) {
                    values.put(Tables.EntriesFlavors.FLAVOR, getFlavorId(flavor.name));
                    values.put(Tables.EntriesFlavors.VALUE, flavor.value);
                    mResolver.insert(uri, values);
                }
            }

            /**
             * Find the ID of a flavor, creating one if it doesn't exist.
             *
             * @param name The name of the flavor
             * @return The ID for the flavor
             */
            private long getFlavorId(String name) {
                final Uri uri = Uri.withAppendedPath(mCatUri, "flavor");
                final String[] projection = new String[] {Tables.Flavors._ID};
                final String where = Tables.Flavors.NAME + " = ?";
                final String[] whereArgs = new String[] {name};
                final Cursor cursor = mResolver.query(uri, projection, where, whereArgs, null);
                try {
                    if(cursor.moveToFirst()) {
                        return cursor.getLong(cursor.getColumnIndex(Tables.Flavors._ID));
                    }
                } finally {
                    cursor.close();
                }

                final ContentValues values = new ContentValues();
                values.put(Tables.Flavors.NAME, name);
                values.put(Tables.Flavors.DELETED, !mIsCatNew);
                return Long.valueOf(mResolver.insert(uri, values).getLastPathSegment());
            }

            /**
             * Insert the photos for the new entry.
             *
             * @param entryUri The Uri for the new entry
             * @param entry    The entry
             */
            private void insertPhotos(Uri entryUri, EntryHolder entry) {
                final Uri uri = Uri.withAppendedPath(entryUri, "photos");
                final ContentValues values = new ContentValues();
                for(PhotoHolder photo : entry.getPhotos()) {
                    values.put(Tables.Photos.PATH, photo.path);
                    mResolver.insert(uri, values);
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Void result) {
                onComplete();
            }
        }
    }
}
