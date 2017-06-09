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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.CSVUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;
import com.ultramegasoft.flavordex2.util.csv.CSVWriter;
import com.ultramegasoft.flavordex2.widget.EntryHolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog for exporting journal entries to CSV files.
 *
 * @author Steve Guidetti
 */
public class ExportDialog extends DialogFragment {
    private static final String TAG = "ExportDialog";

    /**
     * Keys for the Fragment arguments
     */
    private static final String ARG_ENTRY_IDS = "entry_ids";

    /**
     * The list of entry IDs to export
     */
    private long[] mEntryIDs;

    /**
     * Views from the layout
     */
    private EditText mTxtFileName;

    /**
     * The directory where the file will be saved
     */
    private String mBasePath;

    /**
     * Show the dialog.
     *
     * @param fm       The FragmentManager to use
     * @param entryIds The list of entry IDs to export
     */
    public static void showDialog(@NonNull FragmentManager fm, @NonNull long[] entryIds) {
        final DialogFragment fragment = new ExportDialog();

        final Bundle args = new Bundle();
        args.putLongArray(ARG_ENTRY_IDS, entryIds);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @NonNull
    @Override
    @SuppressLint({"InflateParams", "SetTextI18n"})
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mEntryIDs = getArguments().getLongArray(ARG_ENTRY_IDS);
        mBasePath = getBasePath();

        final View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_export, null);
        ((TextView)view.findViewById(R.id.file_path)).setText(mBasePath + "/");
        setupFileField((EditText)view.findViewById(R.id.file_name));

        return new AlertDialog.Builder(getContext())
                .setIcon(R.drawable.ic_export)
                .setTitle(R.string.title_export)
                .setView(view)
                .setPositiveButton(R.string.button_export, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        export();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .create();
    }

    /**
     * Get the path to the directory where we will save the file.
     *
     * @return The path to the directory where we will save the file
     */
    @NonNull
    private static String getBasePath() {
        final File file;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        } else {
            file = Environment.getExternalStorageDirectory();
        }
        //noinspection ResultOfMethodCallIgnored
        file.mkdirs();
        return file.getPath();
    }

    /**
     * Set up the file name input field.
     *
     * @param editText The EditText
     */
    private void setupFileField(@NonNull EditText editText) {
        editText.setText(getDefaultFileString());

        final InputFilter[] filters = new InputFilter[] {
                new InputFilter.LengthFilter(32),
                new LoginFilter.UsernameFilterGeneric() {
                    /**
                     * Additional allowed characters
                     */
                    private static final String mAllowed = "_-.";

                    @SuppressWarnings("SimplifiableIfStatement")
                    @Override
                    public boolean isAllowed(char c) {
                        if('0' <= c && c <= '9') {
                            return true;
                        }
                        if('a' <= c && c <= 'z') {
                            return true;
                        }
                        if('A' <= c && c <= 'Z') {
                            return true;
                        }
                        return mAllowed.indexOf(c) != -1;
                    }
                }
        };
        editText.setFilters(filters);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidateButtons();
            }
        });

        mTxtFileName = editText;
    }

    /**
     * Generate a default file name.
     *
     * @return The file name without the extension
     */
    @NonNull
    private String getDefaultFileString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
        final String dateString = dateFormat.format(new Date());

        final String baseName = getString(R.string.app_name).toLowerCase();

        String fileName = baseName + "_" + dateString;
        File file = new File(mBasePath, fileName + ".csv");

        int i = 1;
        while(file.exists()) {
            fileName = baseName + "_" + dateString + "_" + i++;
            file = new File(mBasePath, fileName + ".csv");
        }

        return fileName;
    }

    /**
     * Update the status of the dialog buttons.
     */
    private void invalidateButtons() {
        final AlertDialog dialog = (AlertDialog)getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setEnabled(!TextUtils.isEmpty(mTxtFileName.getText()));
    }

    /**
     * Export the entries to the specified file.
     */
    private void export() {
        final String fileName = mTxtFileName.getText().toString();
        final File file = new File(mBasePath, fileName + ".csv");
        ExporterFragment.init(getFragmentManager(), mEntryIDs, file.getPath());
    }

    /**
     * Fragment for exporting the selected entries in the background.
     */
    public static class ExporterFragment extends BackgroundProgressDialog {
        private static final String TAG = "ExporterFragment";

        /**
         * Keys for the Fragment arguments
         */
        private static final String ARG_ENTRY_IDS = "entry_ids";
        private static final String ARG_FILE_PATH = "file_path";

        /**
         * The list of entry IDs to export
         */
        private long[] mEntryIds;

        /**
         * The path to the file to save to
         */
        private String mFilePath;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm       The FragmentManager to use
         * @param entryIds The list of entry IDs to export
         * @param filePath The path to the CSV file to save to
         */
        public static void init(@NonNull FragmentManager fm, @NonNull long[] entryIds,
                                @NonNull String filePath) {
            final DialogFragment fragment = new ExporterFragment();

            final Bundle args = new Bundle();
            args.putLongArray(ARG_ENTRY_IDS, entryIds);
            args.putString(ARG_FILE_PATH, filePath);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            mEntryIds = args.getLongArray(ARG_ENTRY_IDS);
            mFilePath = args.getString(ARG_FILE_PATH);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = new ProgressDialog(getContext());

            dialog.setIcon(R.drawable.ic_export);
            dialog.setTitle(R.string.title_exporting);
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(mEntryIds.length);

            return dialog;
        }

        @Override
        protected void startTask() {
            try {
                new DataExporter(new CSVWriter(new FileWriter(mFilePath))).execute();
            } catch(IOException e) {
                Log.e(TAG, "Failed to open new file for writing", e);
                showError(R.string.error_csv_export_file);
                dismiss();
            }
        }

        /**
         * Show an error message.
         */
        private void showError(int errorString) {
            MessageDialog.showDialog(getFragmentManager(), getString(R.string.title_error),
                    getString(errorString), R.drawable.ic_warning);
        }

        /**
         * Task for exporting the data in the background.
         */
        private class DataExporter extends AsyncTask<Void, Integer, Boolean> {
            /**
             * The Context
             */
            @NonNull
            private final Context mContext;

            /**
             * The ContentResolver to load entries from the database
             */
            @NonNull
            private final ContentResolver mResolver;

            /**
             * The CSVWriter to use for writing
             */
            @NonNull
            private final CSVWriter mWriter;

            /**
             * The Uri for the current entry
             */
            @Nullable
            private Uri mEntryUri;

            /**
             * @param writer The CSVWriter to use for writing
             */
            DataExporter(@NonNull CSVWriter writer) {
                mContext = getContext().getApplicationContext();
                mResolver = mContext.getContentResolver();
                mWriter = writer;
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                CSVUtils.writeCSVHeader(mWriter);

                Cursor cursor;
                int i = 0;
                for(long id : mEntryIds) {
                    mEntryUri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, id);
                    cursor = mResolver.query(mEntryUri, null, null, null, null);
                    if(cursor != null) {
                        try {
                            if(cursor.moveToFirst()) {
                                CSVUtils.writeEntry(mWriter, readEntry(cursor));
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                    publishProgress(++i);
                }

                try {
                    mWriter.close();
                } catch(IOException e) {
                    Log.e(TAG, "Failed to write to file", e);
                    return false;
                }

                return true;
            }

            /**
             * Read the entry from the database.
             *
             * @param cursor The Cursor for the entry row
             * @return The entry
             */
            private EntryHolder readEntry(@NonNull Cursor cursor) {
                final EntryHolder entry = new EntryHolder();
                entry.uuid = cursor.getString(cursor.getColumnIndex(Tables.Entries.UUID));
                entry.title = cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
                entry.catName = cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT));
                entry.maker = cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER));
                entry.origin = cursor.getString(cursor.getColumnIndex(Tables.Entries.ORIGIN));
                entry.price = cursor.getString(cursor.getColumnIndex(Tables.Entries.PRICE));
                entry.location = cursor.getString(cursor.getColumnIndex(Tables.Entries.LOCATION));
                entry.date = cursor.getLong(cursor.getColumnIndex(Tables.Entries.DATE));
                entry.rating = cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
                entry.notes = cursor.getString(cursor.getColumnIndex(Tables.Entries.NOTES));

                loadExtras(entry);
                loadFlavors(entry);
                loadPhotos(entry);

                return entry;
            }

            /**
             * Load the extra fields for the entry.
             *
             * @param entry The entry
             */
            private void loadExtras(@NonNull EntryHolder entry) {
                final Uri uri = Uri.withAppendedPath(mEntryUri, "extras");
                final Cursor cursor = mResolver.query(uri, null, null, null, null);
                if(cursor != null) {
                    try {
                        String name;
                        String value;
                        boolean preset;
                        while(cursor.moveToNext()) {
                            name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
                            value = cursor.getString(
                                    cursor.getColumnIndex(Tables.EntriesExtras.VALUE));
                            preset = cursor.getInt(
                                    cursor.getColumnIndex(Tables.Extras.PRESET)) == 1;
                            entry.addExtra(0, name, preset, value);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }

            /**
             * Load the flavors for the entry.
             *
             * @param entry The entry
             */
            private void loadFlavors(@NonNull EntryHolder entry) {
                final Uri uri = Uri.withAppendedPath(mEntryUri, "flavor");
                final Cursor cursor = mResolver.query(uri, null, null, null, null);
                if(cursor != null) {
                    try {
                        String name;
                        int value;
                        while(cursor.moveToNext()) {
                            name = cursor.getString(cursor.getColumnIndex(
                                    Tables.EntriesFlavors.FLAVOR));
                            value = cursor.getInt(
                                    cursor.getColumnIndex(Tables.EntriesFlavors.VALUE));
                            entry.addFlavor(name, value);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }

            /**
             * Load the photos for the entry.
             *
             * @param entry The entry
             */
            private void loadPhotos(@NonNull EntryHolder entry) {
                final Uri uri = Uri.withAppendedPath(mEntryUri, "photos");
                final Cursor cursor = mResolver.query(uri, null, null, null, null);
                if(cursor != null) {
                    try {
                        String path;
                        while(cursor.moveToNext()) {
                            path = cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH));
                            entry.addPhoto(0, null, PhotoUtils.parsePath(path));
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                final ProgressDialog dialog = (ProgressDialog)getDialog();
                if(dialog != null) {
                    dialog.setProgress(values[0]);
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if(result) {
                    Toast.makeText(mContext, R.string.message_export_complete, Toast.LENGTH_LONG)
                            .show();
                } else {
                    showError(R.string.error_csv_export);
                }
                dismiss();
            }
        }
    }
}
