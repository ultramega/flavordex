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

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.CSVUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.util.FileUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;
import com.ultramegasoft.flavordex2.widget.CSVListAdapter;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Dialog for importing journal entries from CSV files.
 *
 * @author Steve Guidetti
 */
public class FileImportDialog extends ImportDialog
        implements LoaderManager.LoaderCallbacks<CSVUtils.CSVHolder> {
    private static final String TAG = "FileImportDialog";

    /**
     * Keys for the Fragment arguments
     */
    private static final String ARG_FILE_PATH = "file_path";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_DUPLICATES = 1000;
    private static final int REQUEST_SET_CATEGORY = 1001;

    /**
     * Keys for the saved state
     */
    private static final String STATE_DATA = "data";

    /**
     * The data loaded from the CSV file
     */
    @Nullable
    private CSVUtils.CSVHolder mData;

    /**
     * The path to the selected file
     */
    private String mFilePath;

    /**
     * Whether the import is a Zip file
     */
    private boolean mIsZipFile;

    /**
     * Show the dialog.
     *
     * @param fm       The FragmentManager to use
     * @param filePath The path to the selected file
     */
    public static void showDialog(@NonNull FragmentManager fm, @NonNull String filePath) {
        final DialogFragment fragment = new FileImportDialog();

        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Bundle args = getArguments();
        if(args != null) {
            mFilePath = args.getString(ARG_FILE_PATH);
            mIsZipFile = mFilePath != null && mFilePath.endsWith(FileUtils.EXT_ZIP);
        }
        if(savedInstanceState != null) {
            mData = savedInstanceState.getParcelable(STATE_DATA);
        }

        if(mData != null) {
            final Context context = getContext();
            if(context != null) {
                setListAdapter(new CSVListAdapter(context, mData));
            }
        } else if(mFilePath != null) {
            getLoaderManager().initLoader(0, null, this).forceLoad();
        }

        if(mIsZipFile) {
            mIncludeImages.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DATA, mData);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_DUPLICATES:
                if(resultCode == Activity.RESULT_OK) {
                    uncheckDuplicates();
                }
                break;
            case REQUEST_SET_CATEGORY:
                final FragmentManager fm = getFragmentManager();
                if(fm != null) {
                    CatListDialog.closeDialog(fm);
                }

                if(resultCode == Activity.RESULT_OK && data != null && mData != null) {
                    final long catId = data.getLongExtra(CatListDialog.EXTRA_CAT_ID, 0);
                    for(EntryHolder entry : mData.entries) {
                        entry.catId = catId;
                    }
                    mData.hasCategory = true;
                    validateData();
                } else {
                    dismiss();
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Uncheck duplicate entries.
     */
    private void uncheckDuplicates() {
        if(mData == null) {
            return;
        }
        final ListView listView = getListView();
        for(int i = 0; i < mData.entries.size(); i++) {
            listView.setItemChecked(i, !mData.duplicates.contains(mData.entries.get(i)));
        }

        final int numDuplicates = mData.duplicates.size();
        if(numDuplicates > 0) {
            final String duplicates =
                    getResources().getQuantityString(R.plurals.duplicates, numDuplicates);
            final String were = getResources().getQuantityString(R.plurals.were, numDuplicates);
            final String message = getString(R.string.message_duplicates_unchecked,
                    numDuplicates, duplicates, were);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void insertSelected() {
        final FragmentManager fm = getFragmentManager();
        final CSVListAdapter adapter = (CSVListAdapter)getListAdapter();
        if(fm != null && adapter != null) {
            final ArrayList<EntryHolder> entries = new ArrayList<>();
            for(long i : getListView().getCheckedItemIds()) {
                entries.add(adapter.getItem((int)i));
            }

            if(mIsZipFile && mIncludeImages.isChecked()) {
                DataSaverFragment.init(fm, entries, mFilePath);
            } else {
                DataSaverFragment.init(fm, entries);
            }
        }
    }

    @NonNull
    @Override
    public Loader<CSVUtils.CSVHolder> onCreateLoader(int id, Bundle args) {
        final Context context = getContext();
        if(context == null) {
            return null;
        }

        setListShown(false);
        return new CsvLoader(context, mFilePath);
    }

    /**
     * Check the data and prompt the user for input as needed.
     */
    private void validateData() {
        invalidateButtons();

        final FragmentManager fm = getFragmentManager();
        if(fm == null || mData == null) {
            return;
        }

        if(!mData.hasCategory) {
            CatListDialog.showDialog(fm, this, REQUEST_SET_CATEGORY);
        } else if(!mData.duplicates.isEmpty()) {
            DuplicatesDialog.showDialog(fm, this, REQUEST_DUPLICATES, mData.duplicates.size());
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<CSVUtils.CSVHolder> loader,
                               CSVUtils.CSVHolder data) {
        final Context context = getContext();
        if(context != null && data != null) {
            setListShown(true);
            setListAdapter(new CSVListAdapter(context, data));

            final ListView listView = getListView();
            for(int i = 0; i < data.entries.size(); i++) {
                listView.setItemChecked(i, true);
            }

            mData = data;
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    validateData();
                }
            });
        } else {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    final FragmentManager fm = getFragmentManager();
                    if(fm != null) {
                        MessageDialog.showDialog(fm, getString(R.string.title_error),
                                getString(R.string.error_csv_parse), R.drawable.ic_warning);
                    }
                    dismiss();
                }
            });
        }

        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<CSVUtils.CSVHolder> loader) {
    }

    /**
     * Asynchronously loads CSV files.
     */
    private static class CsvLoader extends AsyncTaskLoader<CSVUtils.CSVHolder> {
        /**
         * The path to the selected file
         */
        @NonNull
        private final String mFilePath;

        /**
         * @param context  The Context
         * @param filePath The path to the selected file
         */
        CsvLoader(@NonNull Context context, @NonNull String filePath) {
            super(context);
            mFilePath = filePath;
        }

        @Override
        public CSVUtils.CSVHolder loadInBackground() {
            if(mFilePath.substring(mFilePath.lastIndexOf('.')).toLowerCase()
                    .equals(FileUtils.EXT_ZIP)) {
                final File file = getCsvFromZip();
                if(file == null) {
                    return null;
                }
                final CSVUtils.CSVHolder holder = CSVUtils.importCSV(getContext(), file);
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                return holder;
            } else {
                return CSVUtils.importCSV(getContext(), new File(mFilePath));
            }
        }

        /**
         * Extract the CSV file from the Zip archive to a temp file.
         *
         * @return The CSV file
         */
        @Nullable
        private File getCsvFromZip() {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(mFilePath);
                final String entryName = mFilePath.substring(0, mFilePath.lastIndexOf('.'))
                        .substring(mFilePath.lastIndexOf('/') + 1) + FileUtils.EXT_CSV;
                final ZipEntry zipEntry = zipFile.getEntry(entryName);
                if(zipEntry != null) {
                    final File file = File.createTempFile("import_", entryName);
                    FileUtils.dumpStream(zipFile.getInputStream(zipEntry), file);

                    return file;
                }
            } catch(IOException e) {
                Log.e(TAG, "Unable to extract file", e);
            } finally {
                if(zipFile != null) {
                    try {
                        zipFile.close();
                    } catch(IOException ignored) {
                    }
                }
            }

            return null;
        }
    }

    /**
     * Dialog to show the user when duplicate entries are detected.
     */
    @SuppressWarnings("SameParameterValue")
    public static class DuplicatesDialog extends DialogFragment {
        private static final String TAG = "DuplicatesDialog";

        /**
         * Keys for the Fragment arguments
         */
        private static final String ARG_NUM = "num";

        /**
         * Show the dialog.
         *
         * @param fm          The FragmentManager to use
         * @param target      The Fragment to notify of the result
         * @param requestCode A number to identify this request
         * @param num         The number of duplicates
         */
        static void showDialog(@NonNull FragmentManager fm, @Nullable Fragment target,
                               int requestCode, int num) {
            final DialogFragment fragment = new DuplicatesDialog();
            fragment.setTargetFragment(target, requestCode);

            final Bundle args = new Bundle();
            args.putInt(ARG_NUM, num);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @NonNull
        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            final int num = args != null ? args.getInt(ARG_NUM) : 0;

            final Resources res = getResources();
            final String duplicates = res.getQuantityString(R.plurals.duplicates, num);
            final String duplicatesCapitalized =
                    Character.toUpperCase(duplicates.charAt(0)) + duplicates.substring(1);
            final String were = res.getQuantityString(R.plurals.were, num);

            final String message =
                    getString(R.string.message_duplicates_detected, num, duplicates, were);
            final String button =
                    getString(R.string.button_uncheck_duplicates, duplicatesCapitalized);
            return new android.app.AlertDialog.Builder(getContext())
                    .setTitle(R.string.title_duplicates)
                    .setIcon(R.drawable.ic_info)
                    .setMessage(message)
                    .setPositiveButton(button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Fragment fragment = getTargetFragment();
                            if(fragment != null) {
                                fragment.onActivityResult(getTargetRequestCode(),
                                        Activity.RESULT_OK, null);
                            }
                        }
                    })
                    .setNegativeButton(R.string.button_ignore, null)
                    .create();
        }
    }

    /**
     * Fragment for saving the selected entries in the background.
     */
    public static class DataSaverFragment extends BackgroundProgressDialog {
        private static final String TAG = "DataSaverFragment";

        /**
         * Keys for the Fragment arguments
         */
        private static final String ARG_ENTRIES = "entries";
        private static final String ARG_ZIP_FILE = "zip_file";

        /**
         * The list of entries
         */
        private ArrayList<EntryHolder> mEntries;

        /**
         * The path to the Zip file containing images
         */
        @Nullable
        private String mImageZip;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm      The FragmentManager to use
         * @param entries The list of entries
         */
        static void init(@NonNull FragmentManager fm, @NonNull ArrayList<EntryHolder> entries) {
            init(fm, entries, null);
        }

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm       The FragmentManager to use
         * @param entries  The list of entries
         * @param imageZip The path to the Zip file containing images
         */
        static void init(@NonNull FragmentManager fm, @NonNull ArrayList<EntryHolder> entries,
                         @Nullable String imageZip) {
            final DialogFragment fragment = new DataSaverFragment();

            final Bundle args = new Bundle();
            args.putParcelableArrayList(ARG_ENTRIES, entries);
            args.putString(ARG_ZIP_FILE, imageZip);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final Bundle args = getArguments();
            if(args != null) {
                mEntries = args.getParcelableArrayList(ARG_ENTRIES);
                mImageZip = args.getString(ARG_ZIP_FILE);
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
            dialog.setMax(mEntries.size());

            return dialog;
        }

        @Override
        protected void startTask() {
            final Context context = getContext();
            if(context != null) {
                new SaveTask(context, this, mEntries, mImageZip).execute();
            }
        }

        /**
         * Task for saving entries in the background.
         */
        private static class SaveTask extends AsyncTask<Void, Integer, Void> {
            /**
             * The Context reference
             */
            @NonNull
            private final WeakReference<Context> mContext;

            /**
             * The Fragment
             */
            @NonNull
            private final DataSaverFragment mFragment;

            /**
             * The list of entries
             */
            @NonNull
            private final ArrayList<EntryHolder> mEntries;

            /**
             * The path to the Zip file containing images
             */
            @Nullable
            private String mImageZip;

            /**
             * The Zip file containing the files being imported
             */
            @Nullable
            private ZipFile mZipFile;

            /**
             * Map of directories in the Zip file
             */
            private HashMap<String, HashMap<String, ZipEntry>> mZipDirs;

            /**
             * @param context  The Context
             * @param fragment The Fragment
             * @param entries  The list of entries
             * @param imageZip The path to the Zip file containing images
             */
            SaveTask(@NonNull Context context, @NonNull DataSaverFragment fragment,
                     @NonNull ArrayList<EntryHolder> entries, @Nullable String imageZip) {
                mContext = new WeakReference<>(context.getApplicationContext());
                mFragment = fragment;
                mEntries = entries;
                mImageZip = imageZip;
            }

            @Override
            protected Void doInBackground(Void... params) {
                final Context context = mContext.get();
                if(context == null) {
                    return null;
                }

                if(mImageZip != null) {
                    try {
                        mZipFile = new ZipFile(mImageZip);
                        mZipDirs = new HashMap<>();
                        ZipEntry entry;
                        String[] parts;
                        for(Enumeration<? extends ZipEntry> e = mZipFile.entries(); e.hasMoreElements(); ) {
                            entry = e.nextElement();
                            parts = TextUtils.split(entry.getName(), "/");
                            if(parts.length != 2) {
                                continue;
                            }

                            if(!mZipDirs.containsKey(parts[0])) {
                                mZipDirs.put(parts[0], new HashMap<String, ZipEntry>());
                            }

                            mZipDirs.get(parts[0]).put(parts[1], entry);
                        }
                    } catch(IOException e) {
                        Log.w(TAG, "Failed to open Zip file", e);
                    }
                }

                int i = 0;
                for(EntryHolder entry : mEntries) {
                    try {
                        importImages(entry);
                        EntryUtils.insertEntry(context, entry);
                    } catch(SQLiteException e) {
                        Log.e(TAG, "Failed to insert entry: " + entry.title, e);
                    }
                    publishProgress(++i);
                }

                if(mZipFile != null) {
                    try {
                        mZipFile.close();
                    } catch(IOException ignored) {
                    }
                }

                return null;
            }

            /**
             * Import images from the Zip file.
             *
             * @param entry The entry
             */
            private void importImages(@NonNull EntryHolder entry) {
                if(mZipFile == null) {
                    return;
                }

                final HashMap<String, ZipEntry> zipEntries = mZipDirs.get(entry.uuid);
                if(zipEntries == null) {
                    return;
                }

                InputStream inputStream = null;
                for(PhotoHolder photoHolder : entry.getPhotos()) {
                    String fileName = photoHolder.uri.getLastPathSegment();
                    if(!zipEntries.containsKey(fileName)) {
                        continue;
                    }

                    try {
                        inputStream = mZipFile.getInputStream(zipEntries.get(fileName));
                        final File file = PhotoUtils.savePhotoFromStream(inputStream,
                                fileName.substring(fileName.indexOf('_') + 1));
                        photoHolder.uri = Uri.fromFile(file);
                    } catch(IOException e) {
                        Log.w(TAG, "Failed to save image file", e);
                    } finally {
                        if(inputStream != null) {
                            try {
                                inputStream.close();
                            } catch(IOException ignored) {
                            }
                        }
                    }
                }
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
