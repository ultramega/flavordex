package com.ultramegasoft.flavordex2.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
public class FileSelectorDialog extends DialogFragment {
    /**
     * Tag to identify the Fragment
     */
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
    public static final String EXTRA_PATH = "path";

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
    private String mNameFilter;

    /**
     * The Adapter backing the list
     */
    private FileListAdapter mAdapter;

    /**
     * @param fm               The FragmentManager to use
     * @param target           The Fragment to send results to
     * @param requestCode      The request code
     * @param rootPath         The initial starting path
     * @param allowDirectories Whether to allow directories to be selected
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode,
                                  String rootPath, boolean allowDirectories) {
        showDialog(fm, target, requestCode, rootPath, allowDirectories, null, rootPath);
    }

    /**
     * @param fm               The FragmentManager to use
     * @param target           The Fragment to send results to
     * @param requestCode      The request code
     * @param rootPath         The initial starting path
     * @param allowDirectories Whether to allow directories to be selected
     * @param nameFilter       Filter out files that do not contain this string
     */
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode,
                                  String rootPath, boolean allowDirectories, String nameFilter) {
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
    public static void showDialog(FragmentManager fm, Fragment target, int requestCode,
                                   String rootPath, boolean allowDirectories, String nameFilter,
                                   String path) {
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

        if(mRootPath == null) {
            mRootPath = Environment.getExternalStorageDirectory().getPath();
        }

        if(mPath == null) {
            mPath = mRootPath;
        }

        mAdapter = new FileListAdapter(getContext());
        setupAdapter();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int padding = getResources().getDimensionPixelSize(R.dimen.file_list_padding);
        final ListView listView = new ListView(getContext());
        listView.setPadding(padding, padding, padding, padding);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            listView.setPaddingRelative(padding, padding, padding, padding);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        listView.setAdapter(mAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.title_select_file)
                .setView(listView)
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

    /**
     * Called when an item is selected.
     *
     * @param index The item index
     */
    private void selectItem(int index) {
        final File file = new File(mPath);
        final int type = mAdapter.getItemViewType(index);
        if(type == FileListAdapter.CURRENT_DIR_TYPE) {
            mPath = file.getParentFile().getPath();
        } else {
            mPath = new File(file, mAdapter.getItem(index)).getPath();
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

        final String currentDir = mPath.equals(mRootPath) ? null : file.getName();
        mAdapter.setData(getFileList(file), getDirList(file), currentDir);
    }

    /**
     * Get the list of files in the given path that match the current name filter.
     *
     * @param path The path to a directory to list files from
     * @return An array of file names
     */
    private String[] getFileList(File path) {
        final FilenameFilter filenameFilter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                final File file = new File(dir, filename);
                return !(!file.canRead() || file.isDirectory() || filename.startsWith("."))
                        && (mNameFilter == null
                        || filename.toLowerCase().contains(mNameFilter.toLowerCase()));
            }
        };

        final String[] fileList = path.list(filenameFilter);
        Arrays.sort(fileList, String.CASE_INSENSITIVE_ORDER);

        return fileList;
    }

    /**
     * Get the list of directories in the given path.
     *
     * @param path The path to a directory to list files from
     * @return An array of directory names
     */
    private String[] getDirList(File path) {
        final FilenameFilter dirFilter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                final File file = new File(dir, filename);
                return file.canRead() && file.isDirectory() && !filename.startsWith(".");
            }
        };

        final String[] dirList = path.list(dirFilter);
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
        public static final int FILE_TYPE = 0;
        public static final int DIR_TYPE = 1;
        public static final int CURRENT_DIR_TYPE = 2;

        /**
         * The Context
         */
        private final Context mContext;

        /**
         * View padding in pixels
         */
        private final int mPadding;
        private final int mPaddingIndent;

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
        public FileListAdapter(Context context) {
            mContext = context;
            final Resources res = context.getResources();
            mPadding = res.getDimensionPixelSize(R.dimen.file_padding);
            mPaddingIndent = res.getDimensionPixelSize(R.dimen.file_padding_indent);
        }

        /**
         * Reset the data backing the Adapter.
         */
        public void reset() {
            mData.clear();
            mTypes.clear();
            notifyDataSetChanged();
        }

        /**
         * Set the data backing the Adapter.
         *
         * @param files      The list of file names
         * @param dirs       The list of directory names
         * @param currentDir The current directory name
         */
        public void setData(String[] files, String[] dirs, String currentDir) {
            reset();
            addItem(currentDir, CURRENT_DIR_TYPE);
            addItems(dirs, DIR_TYPE);
            addItems(files, FILE_TYPE);
        }

        /**
         * Add an item to the list.
         *
         * @param item The item name
         * @param type The item type
         */
        public void addItem(String item, int type) {
            if(item == null) {
                return;
            }
            mData.add(item);
            mTypes.add(type);
            notifyDataSetChanged();
        }

        /**
         * Add items to the list.
         *
         * @param items The item names
         * @param type  The item type
         */
        public void addItems(String[] items, int type) {
            if(items == null) {
                return;
            }
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
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return mTypes.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                final int id;
                final int padding;
                switch(getItemViewType(position)) {
                    case CURRENT_DIR_TYPE:
                        id = R.layout.file_list_item_dir_open;
                        padding = mPadding;
                        break;
                    case DIR_TYPE:
                        id = R.layout.file_list_item_dir;
                        padding = mPaddingIndent;
                        break;
                    default:
                        id = R.layout.file_list_item;
                        padding = mPaddingIndent;
                }
                convertView = LayoutInflater.from(mContext).inflate(id, parent, false);
                convertView.setPadding(padding, mPadding, mPadding, mPadding);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    convertView.setPaddingRelative(padding, mPadding, mPadding, mPadding);
                }
            }

            ((TextView)convertView).setText(mData.get(position));

            return convertView;
        }
    }
}
