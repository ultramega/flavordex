package com.ultramegasoft.flavordex2.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.ultramegasoft.flavordex2.R;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
    private static void showDialog(FragmentManager fm, Fragment target, int requestCode,
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
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String[] fileList;
        if(!mPath.equals(mRootPath)) {
            final String[] tempList = getFileList(new File(mPath));
            fileList = new String[tempList.length + 1];
            fileList[0] = "^ ..";
            System.arraycopy(tempList, 0, fileList, 1, tempList.length);
        } else {
            fileList = getFileList(new File(mPath));
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.title_select_file)
                .setItems(fileList, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        selectFile(new File(mPath, fileList[whichButton].substring(2)));
                    }
                })
                .setNegativeButton(R.string.button_cancel, null);

        if(mAllowDirectories) {
            builder.setPositiveButton(R.string.button_select_dir,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            selectCurrentDirectory();
                        }
                    });
        }

        return builder.create();
    }

    /**
     * Select a file. If a directory is selected this will open a new dialog at the selected path,
     * otherwise the selected path will be sent to the target Fragment.
     *
     * @param file The selected File
     */
    private void selectFile(File file) {
        if(file.isDirectory()) {
            try {
                showDialog(getFragmentManager(), getTargetFragment(), getTargetRequestCode(),
                        mRootPath, mAllowDirectories, mNameFilter, file.getCanonicalPath());
            } catch(IOException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            final Fragment target = getTargetFragment();
            if(target != null) {
                final Intent data = new Intent();
                data.putExtra(EXTRA_PATH, file.getPath());
                target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
            }
        }
    }

    /**
     * Send the current path to the target Fragment.
     */
    private void selectCurrentDirectory() {
        final Fragment target = getTargetFragment();
        if(target != null) {
            final Intent data = new Intent();
            data.putExtra(EXTRA_PATH, mPath);
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
        }
    }

    /**
     * Get the list of files in the given path that match the current name filter.
     *
     * @param path The path to a directory to list files from
     * @return An array of file names
     */
    private String[] getFileList(File path) {
        if(path == null || !path.canRead() || !path.isDirectory()) {
            return new String[0];
        }

        final FilenameFilter dirFilter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                final File file = new File(dir, filename);
                return file.canRead() && file.isDirectory() && !filename.startsWith(".");
            }
        };

        final String[] dirList = path.list(dirFilter);
        Arrays.sort(dirList, String.CASE_INSENSITIVE_ORDER);
        for(int i = 0; i < dirList.length; i++) {
            dirList[i] = "] " + dirList[i];
        }

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
        for(int i = 0; i < fileList.length; i++) {
            fileList[i] = "- " + fileList[i];
        }

        final String[] finalList = new String[dirList.length + fileList.length];
        System.arraycopy(dirList, 0, finalList, 0, dirList.length);
        System.arraycopy(fileList, 0, finalList, dirList.length, fileList.length);

        return finalList;
    }
}
