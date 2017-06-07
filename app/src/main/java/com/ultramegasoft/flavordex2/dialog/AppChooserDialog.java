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
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.AppImportUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.widget.EntryHolder;

import java.util.ArrayList;

/**
 * Dialog for choosing which app to import journal entries from.
 *
 * @author Steve Guidetti
 */
public class AppChooserDialog extends DialogFragment {
    private static final String TAG = "AppChooserDialog";

    /**
     * Keys for the Fragment arguments
     */
    private static final String ARG_MULTI_CHOICE = "multi_choice";

    /**
     * The ListView from the layout
     */
    private ListView mListView;

    /**
     * Show the dialog.
     *
     * @param fm          The FragmentManager to use
     * @param multiChoice Whether to allow multiple selections
     */
    public static void showDialog(FragmentManager fm, boolean multiChoice) {
        final DialogFragment fragment = new AppChooserDialog();

        final Bundle args = new Bundle();
        args.putBoolean(ARG_MULTI_CHOICE, multiChoice);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final boolean multiChoice = getArguments().getBoolean(ARG_MULTI_CHOICE);
        final View root =
                LayoutInflater.from(getContext()).inflate(R.layout.dialog_app_chooser, null);

        mListView = (ListView)root.findViewById(R.id.list);
        mListView.setAdapter(new AppListAdapter(getContext(), multiChoice));

        final int count;
        if(multiChoice) {
            count = mListView.getCount();
            mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    invalidateButtons();
                }
            });

            final ListAdapter adapter = mListView.getAdapter();
            for(int i = 0; i < count; i++) {
                mListView.setItemChecked(i, adapter.isEnabled(i));
            }
        } else {
            count = 1;
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectItem(id);
                }
            });
        }

        final TextView header = (TextView)root.findViewById(R.id.header);
        final String appsString = getResources().getQuantityString(R.plurals.app, count);
        header.setText(getString(R.string.header_select_app, appsString));

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_import_app)
                .setIcon(R.drawable.ic_import)
                .setView(root)
                .setNegativeButton(R.string.button_cancel, null);

        if(multiChoice) {
            builder.setPositiveButton(R.string.button_import,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            importSelected();
                        }
                    });
        }

        return builder.create();
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
     * Select a single app to import entries from.
     *
     * @param id The selected item position
     */
    private void selectItem(long id) {
        AppImportDialog.showDialog(getFragmentManager(), (int)id);
        dismiss();
    }

    /**
     * Import all of the entries from the selected apps.
     */
    private void importSelected() {
        final AppListAdapter adapter = (AppListAdapter)mListView.getAdapter();

        final int[] appIds = new int[mListView.getCheckedItemCount()];
        final CharSequence[] appNames = new String[mListView.getCheckedItemCount()];

        AppImportUtils.AppHolder appHolder;
        for(int i = 0, j = 0; i < mListView.getCount(); i++) {
            if(mListView.isItemChecked(i)) {
                appHolder = adapter.getItem(i);
                appIds[j] = appHolder.app;
                appNames[j] = appHolder.title;
                j++;
            }
        }

        ImporterFragment.init(getFragmentManager(), appIds, appNames);
    }

    /**
     * Custom Adapter for listing installed apps.
     */
    private static class AppListAdapter extends BaseAdapter {
        /**
         * The Context
         */
        private final Context mContext;

        /**
         * Whether to allow multiple selections
         */
        private final boolean mMultiChoice;

        /**
         * The list of installed apps
         */
        private final ArrayList<AppImportUtils.AppHolder> mData;

        /**
         * @param context     The Context
         * @param multiChoice Whether to allow multiple selections
         */
        AppListAdapter(Context context, boolean multiChoice) {
            mContext = context;
            mMultiChoice = multiChoice;
            mData = AppImportUtils.getInstalledApps(context.getPackageManager());
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public AppImportUtils.AppHolder getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).app;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return isEnabled(position) ? 0 : 1;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).supported;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final boolean enabled = isEnabled(position);
            if(convertView == null) {
                final int id = enabled ? R.layout.app_list_item : R.layout.app_list_item_disabled;
                convertView = LayoutInflater.from(mContext).inflate(id, parent, false);

                final Holder holder = new Holder();
                holder.icon = (ImageView)convertView.findViewById(R.id.icon);
                holder.title = (TextView)convertView.findViewById(R.id.title);
                convertView.setTag(holder);

                convertView.findViewById(R.id.checkbox)
                        .setVisibility(mMultiChoice ? View.VISIBLE : View.GONE);
            }

            final AppImportUtils.AppHolder appHolder = getItem(position);

            final Holder holder = (Holder)convertView.getTag();
            holder.icon.setImageDrawable(appHolder.icon);
            if(enabled) {
                holder.title.setText(appHolder.title);
            } else {
                holder.title.setText(mContext.getString(R.string.message_app_requires_update,
                        appHolder.title));
            }

            return convertView;
        }

        /**
         * Holder for View references
         */
        private static class Holder {
            public ImageView icon;
            public TextView title;
        }
    }

    /**
     * Fragment for importing all entries from the selected apps in the background.
     */
    public static class ImporterFragment extends BackgroundProgressDialog {
        private static final String TAG = "ImporterFragment";

        /**
         * Keys for the Fragment arguments
         */
        private static final String ARG_APP_IDS = "app_ids";
        private static final String ARG_APP_NAMES = "app_names";

        /**
         * The list of source apps to import from
         */
        private int[] mApps;

        /**
         * The names of the apps
         */
        private CharSequence[] mAppNames;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm       The FragmentManager to use
         * @param appIds   The source app IDs
         * @param appNames The names of the apps
         */
        public static void init(FragmentManager fm, int[] appIds, CharSequence[] appNames) {
            final DialogFragment fragment = new ImporterFragment();

            final Bundle args = new Bundle();
            args.putIntArray(ARG_APP_IDS, appIds);
            args.putCharSequenceArray(ARG_APP_NAMES, appNames);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            mApps = args.getIntArray(ARG_APP_IDS);
            mAppNames = args.getCharSequenceArray(ARG_APP_NAMES);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = new ProgressDialog(getContext());

            dialog.setIcon(R.drawable.ic_import);
            dialog.setTitle(R.string.title_importing);
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("");

            return dialog;
        }

        @Override
        protected void startTask() {
            new ImportTask().execute();
        }

        /**
         * Task for importing entries in the background.
         */
        private class ImportTask extends AsyncTask<Void, Integer, Void> {
            /**
             * The Context
             */
            private final Context mContext;

            ImportTask() {
                mContext = getContext().getApplicationContext();
            }

            @Override
            protected Void doInBackground(Void... params) {
                final ContentResolver cr = mContext.getContentResolver();

                int appId;
                for(int i = 0; i < mApps.length; i++) {
                    appId = mApps[i];

                    final Uri uri = AppImportUtils.getEntriesUri(appId);
                    final String[] projection = {AppImportUtils.EntriesColumns._ID};
                    final Cursor cursor = cr.query(uri, projection, null, null, null);
                    int count;
                    try {
                        if(cursor == null) {
                            continue;
                        }
                        EntryHolder entry;
                        count = cursor.getCount();
                        int j = 0;
                        while(cursor.moveToNext()) {
                            entry = AppImportUtils.importEntry(mContext, appId, cursor.getLong(0));
                            try {
                                EntryUtils.insertEntry(mContext, entry);
                            } catch(SQLiteException e) {
                                Log.e(TAG, "Failed to insert entry: " + entry.title, e);
                            }
                            publishProgress(i, ++j, count);
                        }
                    } finally {
                        cursor.close();
                    }
                    publishProgress(i, count, count);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                final ProgressDialog dialog = (ProgressDialog)getDialog();
                if(dialog != null) {
                    dialog.setMessage(mAppNames[values[0]]);
                    dialog.setMax(values[2]);
                    dialog.setProgress(values[1]);
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                Toast.makeText(mContext, R.string.message_import_complete, Toast.LENGTH_LONG)
                        .show();
                dismiss();
            }
        }
    }
}
