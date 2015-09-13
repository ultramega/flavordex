package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
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
        mListView = (ListView)root.findViewById(R.id.list);
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                invalidateButtons();
            }
        });

        mProgressBar = (ProgressBar)root.findViewById(R.id.progress);

        if(mData != null) {
            mListView.setAdapter(new CSVListAdapter(getContext(), mData));
        } else if(mFilePath != null) {
            mProgressBar.setVisibility(View.VISIBLE);
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
        } else {
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

            /**
             * The Uri for the entry category
             */
            private Uri mCatUri;

            /**
             * Whether this is a new category
             */
            private boolean mIsCatNew;

            public SaveTask() {
                mResolver = getContext().getContentResolver();
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
                    publishProgress(++i);
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
                values.put(Tables.Entries.PRICE, entry.price);
                values.put(Tables.Entries.LOCATION, entry.location);
                values.put(Tables.Entries.DATE, entry.date);
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
