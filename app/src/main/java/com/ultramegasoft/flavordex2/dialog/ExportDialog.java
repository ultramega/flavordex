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
import android.support.v7.widget.AppCompatCheckBox;
import android.text.Editable;
import android.text.InputFilter;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.CSVUtils;
import com.ultramegasoft.flavordex2.util.FileUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;
import com.ultramegasoft.flavordex2.util.csv.CSVWriter;
import com.ultramegasoft.flavordex2.widget.EntryHolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
     * Whether to include images in the export
     */
    private boolean mIncludeImages;

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
        final Context context = getContext();
        if(context == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        final Bundle args = getArguments();
        if(args != null) {
            mEntryIDs = args.getLongArray(ARG_ENTRY_IDS);
        }
        mBasePath = getBasePath();

        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_export, null);
        ((TextView)view.findViewById(R.id.file_path)).setText(mBasePath + "/");

        final TextView extension = view.findViewById(R.id.extension);
        extension.setText(FileUtils.EXT_CSV);
        ((AppCompatCheckBox)view.findViewById(R.id.include_images)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        extension.setText(isChecked ? FileUtils.EXT_ZIP : FileUtils.EXT_CSV);
                        mIncludeImages = isChecked;
                    }
                });

        setupFileField((EditText)view.findViewById(R.id.file_name));

        return new AlertDialog.Builder(context)
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

                    @Override
                    @SuppressWarnings({"SimplifiableIfStatement", "MethodDoesntCallSuperMethod"})
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

        final String fileName = baseName + "_" + dateString;
        final String extension = mIncludeImages ? FileUtils.EXT_ZIP : FileUtils.EXT_CSV;
        final String unique = FileUtils.getUniqueFileName(mBasePath, fileName, extension);

        return unique.substring(0, unique.lastIndexOf('.'));
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
        final FragmentManager fm = getFragmentManager();
        if(fm == null) {
            return;
        }

        final String extension = mIncludeImages ? FileUtils.EXT_ZIP : FileUtils.EXT_CSV;
        final String fileName = FileUtils.getUniqueFileName(mBasePath,
                mTxtFileName.getText().toString(), extension);
        final File file = new File(mBasePath, fileName.substring(0, fileName.lastIndexOf('.')));
        ExporterFragment.init(fm, mEntryIDs, file.getPath(), mIncludeImages);
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
        private static final String ARG_INCLUDE_IMAGES = "include_images";

        /**
         * The list of entry IDs to export
         */
        private long[] mEntryIds;

        /**
         * The path to the file to save to
         */
        private String mFilePath;

        /**
         * Whether to include images in the export
         */
        private boolean mIncludeImages;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm            The FragmentManager to use
         * @param entryIds      The list of entry IDs to export
         * @param filePath      The path to the CSV file to save to
         * @param includeImages Whether to include images in the export
         */
        static void init(@NonNull FragmentManager fm, @NonNull long[] entryIds,
                         @NonNull String filePath, boolean includeImages) {
            final DialogFragment fragment = new ExporterFragment();

            final Bundle args = new Bundle();
            args.putLongArray(ARG_ENTRY_IDS, entryIds);
            args.putString(ARG_FILE_PATH, filePath);
            args.putBoolean(ARG_INCLUDE_IMAGES, includeImages);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final Bundle args = getArguments();
            if(args != null) {
                mEntryIds = args.getLongArray(ARG_ENTRY_IDS);
                mFilePath = args.getString(ARG_FILE_PATH);
                mIncludeImages = args.getBoolean(ARG_INCLUDE_IMAGES);
            }
        }

        @NonNull
        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
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
                final Context context = getContext();
                if(context != null) {
                    final String fileName = mFilePath;
                    new DataExporter(context, this, mEntryIds, fileName, mIncludeImages).execute();
                }
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
            final FragmentManager fm = getFragmentManager();
            if(fm != null) {
                MessageDialog.showDialog(fm, getString(R.string.title_error),
                        getString(errorString), R.drawable.ic_warning);
            }
        }

        /**
         * Task for exporting the data in the background.
         */
        private static class DataExporter extends AsyncTask<Void, Integer, Boolean> {
            /**
             * The Context reference
             */
            @NonNull
            private final WeakReference<Context> mContext;

            /**
             * The Fragment
             */
            @NonNull
            private final ExporterFragment mFragment;

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
             * The list of entry IDs to export
             */
            @NonNull
            private final long[] mEntryIds;

            /**
             * The path to the output file without extension
             */
            @NonNull
            private final String mFileName;

            /**
             * The Uri for the current entry
             */
            @Nullable
            private Uri mEntryUri;

            /**
             * The OutputStream for writing to a Zip file
             */
            @Nullable
            private ZipOutputStream mZipOutputStream = null;

            /**
             * Buffer for reading and writing files
             */
            private final byte[] mBuffer = new byte[8192];

            /**
             * @param context       The Context
             * @param fragment      The Fragment
             * @param entryIds      The list of entry IDs to export
             * @param fileName      The path to the output file without extension
             * @param includeImages Whether to include images in the export
             */
            DataExporter(@NonNull Context context, @NonNull ExporterFragment fragment,
                         @NonNull long[] entryIds, @NonNull String fileName,
                         boolean includeImages) throws IOException {
                mContext = new WeakReference<>(context.getApplicationContext());
                mFragment = fragment;
                mEntryIds = entryIds;
                mFileName = fileName;
                mResolver = context.getContentResolver();
                mWriter = new CSVWriter(new FileWriter(fileName + FileUtils.EXT_CSV));

                if(includeImages) {
                    mZipOutputStream = new ZipOutputStream(new BufferedOutputStream(
                            new FileOutputStream(fileName + FileUtils.EXT_ZIP)));
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    CSVUtils.writeCSVHeader(mWriter);

                    Cursor cursor;
                    int i = 0;
                    for(long id : mEntryIds) {
                        mEntryUri =
                                ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, id);
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
                } catch(IOException e) {
                    Log.e(TAG, "Failed to write to file", e);
                    return false;
                } finally {
                    try {
                        mWriter.close();
                    } catch(IOException ignored) {
                    }
                }

                if(mZipOutputStream != null) {
                    try {
                        final String fileName = mFileName.substring(mFileName.lastIndexOf('/') + 1)
                                + FileUtils.EXT_CSV;
                        addToZipFile(mZipOutputStream, mFileName + FileUtils.EXT_CSV, fileName);

                        mZipOutputStream.close();
                        //noinspection ResultOfMethodCallIgnored
                        new File(mFileName + FileUtils.EXT_CSV).delete();
                    } catch(IOException e) {
                        Log.e(TAG, "Failed to write to file", e);
                        return false;
                    }
                }

                return true;
            }

            /**
             * Read the entry from the database.
             *
             * @param cursor The Cursor for the entry row
             * @return The entry
             */
            private EntryHolder readEntry(@NonNull Cursor cursor) throws IOException {
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
            private void loadPhotos(@NonNull EntryHolder entry) throws IOException {
                final Uri uri = Uri.withAppendedPath(mEntryUri, "photos");
                final Cursor cursor = mResolver.query(uri, null, null, null, Tables.Photos.POS);
                if(cursor != null) {
                    try {
                        while(cursor.moveToNext()) {
                            addPhoto(entry,
                                    cursor.getString(cursor.getColumnIndex(Tables.Photos.PATH)),
                                    cursor.getInt(cursor.getColumnIndex(Tables.Photos.POS)));
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }

            /**
             * Add a photo to an entry export.
             *
             * @param entry The entry
             * @param path  The path to the source photo file
             * @param sort  The sort position of this photo
             */
            private void addPhoto(@NonNull EntryHolder entry, @NonNull String path, int sort)
                    throws IOException {
                Uri photoUri = PhotoUtils.parsePath(path);
                if(photoUri != null) {
                    if(mZipOutputStream == null) {
                        entry.addPhoto(0, null, photoUri);
                    } else {
                        final String outName = String.format(Locale.US, "%s/%d_%s", entry.uuid,
                                sort, photoUri.getLastPathSegment());
                        addToZipFile(mZipOutputStream, photoUri.getPath(), outName);
                        entry.addPhoto(0, null,
                                Uri.parse(outName.substring(outName.lastIndexOf('/') + 1)));
                    }
                }
            }

            /**
             * Add a file to the Zip file.
             *
             * @param zipOutputStream The open ZipOutputStream
             * @param sourcePath      The path to the source file
             * @param destName        The destination file name
             */
            private void addToZipFile(@NonNull ZipOutputStream zipOutputStream,
                                      @NonNull String sourcePath, @NonNull String destName)
                    throws IOException {
                BufferedInputStream source = null;
                try {
                    source = new BufferedInputStream(new FileInputStream(sourcePath));
                    zipOutputStream.putNextEntry(new ZipEntry(destName));

                    int bytes;
                    while((bytes = source.read(mBuffer, 0, mBuffer.length)) != -1) {
                        zipOutputStream.write(mBuffer, 0, bytes);
                    }
                } finally {
                    if(source != null) {
                        source.close();
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
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if(result) {
                    final Context context = mContext.get();
                    if(context != null) {
                        Toast.makeText(context, R.string.message_export_complete, Toast.LENGTH_LONG)
                                .show();
                    }
                } else {
                    mFragment.showError(R.string.error_csv_export);
                }
                mFragment.dismiss();
            }
        }
    }
}
