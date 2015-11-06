package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;

/**
 * Base for Dialogs for importing journal entries from external sources.
 *
 * @author Steve Guidetti
 */
public abstract class ImportDialog extends DialogFragment {
    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_DUPLICATES = 1000;

    /**
     * Views from the layout
     */
    private FrameLayout mListContainer;
    private ListView mListView;
    private ProgressBar mProgressBar;

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_DUPLICATES:
                if(resultCode == Activity.RESULT_OK) {
                    uncheckDuplicates();
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Insert the selected entries into the database.
     */
    protected abstract void insertSelected();

    /**
     * Uncheck duplicate entries.
     */
    protected void uncheckDuplicates() {
    }

    /**
     * Show the dialog asking the user whether to uncheck duplicate entries.
     *
     * @param num The number of duplicate entries
     */
    protected final void showDuplicatesDialog(int num) {
        DuplicatesDialog.showDialog(getFragmentManager(), this, REQUEST_DUPLICATES, num);
    }

    /**
     * Update the status of the dialog buttons.
     */
    protected final void invalidateButtons() {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            final boolean itemSelected = mListView.getCheckedItemCount() > 0;
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(itemSelected);
        }
    }

    /**
     * Set whether to show the list or the loading indicator.
     *
     * @param shown Whether to show the list
     */
    protected void setListShown(boolean shown) {
        if(shown) {
            mProgressBar.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            mListContainer.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Get the ListView from the dialog.
     *
     * @return The ListView
     */
    protected ListView getListView() {
        return mListView;
    }

    /**
     * Set the ListAdapter backing the list.
     *
     * @param adapter A ListAdapter
     */
    protected void setListAdapter(ListAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    /**
     * Get the ListAdapter backing the list.
     *
     * @return The ListAdapter
     */
    protected ListAdapter getListAdapter() {
        return mListView.getAdapter();
    }

    /**
     * Dialog to show the user when duplicate entries are detected.
     */
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
        public static void showDialog(FragmentManager fm, Fragment target, int requestCode, int num) {
            final DialogFragment fragment = new DuplicatesDialog();
            fragment.setTargetFragment(target, requestCode);

            final Bundle args = new Bundle();
            args.putInt(ARG_NUM, num);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int num = getArguments().getInt(ARG_NUM);

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
}
