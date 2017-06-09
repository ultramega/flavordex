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
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dialog for browsing and selecting files from the filesystem.
 *
 * @author Steve Guidetti
 */
@SuppressWarnings("SameParameterValue")
public class FileSelectorDialog extends DialogFragment {
    private static final String TAG = "FileSelectorDialog";

    /**
     * Arguments for the Fragment
     */
    private static final String ARG_PATH = "path";
    private static final String ARG_ROOT_PATH = "root_path";
    private static final String ARG_ALLOW_DIRECTORIES = "allow_directories";
    private static final String ARG_NAME_FILTER = "name_filter";

    /**
     * Keys for the result data Intent
     */
    private static final String EXTRA_PATH = "path";

    /**
     * Keys for the saved state
     */
    private static final String STATE_PATH = "path";

    /**
     * The current path
     */
    private String mPath;

    /**
     * The initial starting path
     */
    private String mRootPath;

    /**
     * Whether to allow directories to be selected
     */
    private boolean mAllowDirectories;

    /**
     * Filter out files that do not contain this string
     */
    @Nullable
    private String mNameFilter;

    /**
     * The ListView from the layout
     */
    private ListView mListView;

    /**
     * The header view used to show the current directory and allow traversing up the tree
     */
    private TextView mHeader;

    /**
     * The view to show when the current directory is empty
     */
    private TextView mEmpty;

    /**
     * The Adapter backing the list
     */
    private FileListAdapter mAdapter;

    /**
     * Callback interface for Activities to receive results.
     */
    public interface OnFileSelectedCallbacks {
        /**
         * Called when a file is selected.
         *
         * @param filePath The path to the selected file
         */
        void onFileSelected(String filePath);
    }

    /**
     * @param fm               The FragmentManager to use
     * @param target           The Fragment to send results to
     * @param requestCode      The request code
     * @param rootPath         The initial starting path
     * @param allowDirectories Whether to allow directories to be selected
     * @param nameFilter       Filter out files that do not contain this string
     */
    public static void showDialog(@NonNull FragmentManager fm, @Nullable Fragment target,
                                  int requestCode, @Nullable String rootPath,
                                  boolean allowDirectories, @Nullable String nameFilter) {
        showDialog(fm, target, requestCode, rootPath, allowDirectories, nameFilter, rootPath);
    }

    /**
     * @param fm               The FragmentManager to use
     * @param target           The Fragment to send results to
     * @param requestCode      The request code
     * @param rootPath         The initial starting path
     * @param allowDirectories Whether to allow directories to be selected
     * @param nameFilter       Filter out files that do not contain this string
     * @param path             The current path
     */
    private static void showDialog(@NonNull FragmentManager fm, @Nullable Fragment target,
                                   int requestCode, @Nullable String rootPath,
                                   boolean allowDirectories, @Nullable String nameFilter,
                                   @Nullable String path) {
        final DialogFragment fragment = new FileSelectorDialog();
        fragment.setTargetFragment(target, requestCode);

        final Bundle args = new Bundle();
        args.putString(ARG_ROOT_PATH, rootPath);
        args.putString(ARG_PATH, path);
        args.putBoolean(ARG_ALLOW_DIRECTORIES, allowDirectories);
        args.putString(ARG_NAME_FILTER, nameFilter);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        mPath = args.getString(ARG_PATH);
        mRootPath = args.getString(ARG_ROOT_PATH);
        mAllowDirectories = args.getBoolean(ARG_ALLOW_DIRECTORIES, false);
        mNameFilter = args.getString(ARG_NAME_FILTER);

        if(savedInstanceState != null) {
            mPath = savedInstanceState.getString(STATE_PATH, mPath);
        }

        if(mRootPath == null) {
            mRootPath = Environment.getExternalStorageDirectory().getPath();
        }

        if(mPath == null) {
            mPath = mRootPath;
        }

        if(!new File(mPath).canRead()) {
            Toast.makeText(getContext(), R.string.error_read_dir, Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        mAdapter = new FileListAdapter(getContext());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupAdapter();
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View root = LayoutInflater.from(getContext()).inflate(R.layout.list_dialog, null);
        mListView = root.findViewById(R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        mEmpty = root.findViewById(R.id.empty);
        if(mNameFilter == null) {
            mEmpty.setText(R.string.message_empty_dir);
        } else {
            mEmpty.setText(getString(R.string.message_empty_dir_filtered, mNameFilter));
        }
        mEmpty.setVisibility(View.VISIBLE);
        ((ViewGroup)root.findViewById(R.id.list_container)).removeView(mEmpty);
        mEmpty.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.title_select_file)
                .setView(root)
                .setNegativeButton(R.string.button_cancel, null);

        if(mAllowDirectories) {
            builder.setPositiveButton(R.string.button_select_dir,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            selectCurrentFile();
                        }
                    });
        }

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PATH, mPath);
    }

    /**
     * Called when an item is selected.
     *
     * @param index The item index
     */
    private void selectItem(int index) {
        final ListAdapter adapter = mListView.getAdapter();
        final int type = adapter.getItemViewType(index);
        final String item = (String)adapter.getItem(index);

        final File file = new File(mPath);
        if(type == AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            mPath = file.getParentFile().getPath();
        } else {
            mPath = new File(file, item).getPath();
        }

        if(type == FileListAdapter.FILE_TYPE) {
            selectCurrentFile();
        } else {
            setupAdapter();
        }
    }

    /**
     * Select a file and send the result back to the target Fragment.
     */
    private void selectCurrentFile() {
        final Fragment target = getTargetFragment();
        if(target != null) {
            final Intent data = new Intent();
            data.putExtra(EXTRA_PATH, mPath);
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
        } else if(getActivity() instanceof OnFileSelectedCallbacks) {
            ((OnFileSelectedCallbacks)getActivity()).onFileSelected(mPath);
        }
        dismiss();
    }

    /**
     * Set up the Adapter with the current list of files.
     */
    private void setupAdapter() {
        final File file = new File(mPath);
        if(!file.canRead() || !file.isDirectory()) {
            mAdapter.reset();
            return;
        }

        mListView.setAdapter(null);

        mAdapter.setData(getFileList(file), getDirList(file));
        setHeader(mPath.equals(mRootPath) ? null : file.getName());
        showEmpty(mAdapter.isEmpty());

        mListView.setAdapter(mAdapter);
    }

    /**
     * Set the header text.
     *
     * @param header The header text or null to remove the header
     */
    private void setHeader(@Nullable String header) {
        if(mHeader == null) {
            mHeader = (TextView)LayoutInflater.from(getContext())
                    .inflate(R.layout.file_list_item_dir_open, mListView, false);
        }
        mListView.removeHeaderView(mHeader);
        if(header != null) {
            mHeader.setText(header);
            mListView.addHeaderView(mHeader);
        }
    }

    /**
     * Show or hide the empty directory text.
     *
     * @param show Whether to show the empty directory text
     */
    private void showEmpty(boolean show) {
        mListView.removeFooterView(mEmpty);
        if(show) {
            mListView.addFooterView(mEmpty, null, false);
        }
    }

    /**
     * Get the list of files in the given path that match the current name filter.
     *
     * @param path The path to a directory to list files from
     * @return An array of file names
     */
    @NonNull
    private String[] getFileList(@NonNull File path) {
        final FilenameFilter filenameFilter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                final File file = new File(dir, filename);
                return !(!file.canRead() || file.isDirectory() || filename.startsWith("."))
                        && (mNameFilter == null
                        || filename.toLowerCase().contains(mNameFilter.toLowerCase()));
            }
        };

        final String[] fileList = path.list(filenameFilter);
        if(fileList == null) {
            return new String[0];
        }
        Arrays.sort(fileList, String.CASE_INSENSITIVE_ORDER);

        return fileList;
    }

    /**
     * Get the list of directories in the given path.
     *
     * @param path The path to a directory to list files from
     * @return An array of directory names
     */
    @NonNull
    private String[] getDirList(@NonNull File path) {
        final FilenameFilter dirFilter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                final File file = new File(dir, filename);
                return file.canRead() && file.isDirectory() && !filename.startsWith(".");
            }
        };

        final String[] dirList = path.list(dirFilter);
        if(dirList == null) {
            return new String[0];
        }
        Arrays.sort(dirList, String.CASE_INSENSITIVE_ORDER);

        return dirList;
    }

    /**
     * Custom Adapter for listing files.
     */
    private static class FileListAdapter extends BaseAdapter {
        /**
         * Item type IDs
         */
        static final int FILE_TYPE = 0;
        static final int DIR_TYPE = 1;

        /**
         * The Context
         */
        @NonNull
        private final Context mContext;

        /**
         * The list of file names
         */
        private final ArrayList<String> mData = new ArrayList<>();

        /**
         * The list of types corresponding to each file
         */
        private final ArrayList<Integer> mTypes = new ArrayList<>();

        /**
         * @param context The Context
         */
        FileListAdapter(@NonNull Context context) {
            mContext = context;
        }

        /**
         * Reset the data backing the Adapter.
         */
        void reset() {
            mData.clear();
            mTypes.clear();
            notifyDataSetChanged();
        }

        /**
         * Set the data backing the Adapter.
         *
         * @param files The list of file names
         * @param dirs  The list of directory names
         */
        void setData(@NonNull String[] files, @NonNull String[] dirs) {
            reset();
            addItems(dirs, DIR_TYPE);
            addItems(files, FILE_TYPE);
        }

        /**
         * Add items to the list.
         *
         * @param items The item names
         * @param type  The item type
         */
        void addItems(@NonNull String[] items, int type) {
            for(String item : items) {
                mData.add(item);
                mTypes.add(type);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public String getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return mTypes.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                final int id = getItemViewType(position) == DIR_TYPE
                        ? R.layout.file_list_item_dir
                        : R.layout.file_list_item;
                convertView = LayoutInflater.from(mContext).inflate(id, parent, false);
            }

            ((TextView)convertView).setText(mData.get(position));

            return convertView;
        }
    }
}
