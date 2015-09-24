package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
    /**
     * Tag to identify the Fragment
     */
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
            for(int i = 0; i < count; i++) {
                mListView.setItemChecked(i, true);
            }
        } else {
            count = 1;
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectItem(position);
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
    protected final void invalidateButtons() {
        final AlertDialog dialog = (AlertDialog)getDialog();
        final boolean itemSelected = mListView.getCheckedItemCount() > 0;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(itemSelected);
    }

    /**
     * Select a single app to import entries from.
     *
     * @param position The selected item position
     */
    private void selectItem(int position) {
        AppImportDialog.showDialog(getFragmentManager(), position);
        dismiss();
    }

    /**
     * Import all of the entries from the selected apps.
     */
    private void importSelected() {
        ImporterFragment.init(getFragmentManager(), mListView.getCheckedItemIds());
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
        public AppListAdapter(Context context, boolean multiChoice) {
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
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.app_list_item, parent,
                        false);

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
            holder.title.setText(appHolder.title);

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
    public static class ImporterFragment extends DialogFragment {
        /**
         * The tag to identify this Fragment
         */
        private static final String TAG = "ImporterFragment";

        /**
         * Keys for the Fragment arguments
         */
        private static final String ARG_APPS = "apps";

        /**
         * The list of source apps to import from
         */
        private long[] mApps;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm   The FragmentManager to use
         * @param apps The source apps
         */
        public static void init(FragmentManager fm, long[] apps) {
            final DialogFragment fragment = new ImporterFragment();

            final Bundle args = new Bundle();
            args.putLongArray(ARG_APPS, apps);
            fragment.setArguments(args);

            fragment.show(fm, TAG);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);
            setCancelable(false);

            final Bundle args = getArguments();
            mApps = args.getLongArray(ARG_APPS);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = new ProgressDialog(getContext());

            dialog.setIcon(R.drawable.ic_import);
            dialog.setTitle(R.string.title_importing);
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(mApps.length);

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            new ImportTask().execute();
        }

        /**
         * Task for importing entries in the background.
         */
        private class ImportTask extends AsyncTask<Void, Integer, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                final Context context = getContext();
                final ContentResolver cr = context.getContentResolver();

                int i = 0;
                for(long appId : mApps) {
                    final int app = (int)appId;
                    final Uri uri = AppImportUtils.getEntriesUri(app);
                    final String[] projection = {AppImportUtils.EntriesColumns._ID};
                    final Cursor cursor = cr.query(uri, projection, null, null, null);
                    try {
                        EntryHolder entry;
                        final int count = cursor.getCount();
                        int j = 0;
                        while(cursor.moveToNext()) {
                            entry = AppImportUtils.importEntry(context, app, cursor.getLong(0));
                            EntryUtils.insertEntry(cr, entry);
                            publishProgress(i, ++j * 10000 / count);
                        }
                    } finally {
                        cursor.close();
                    }
                    publishProgress(++i, 0);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                final ProgressDialog dialog = (ProgressDialog)getDialog();
                dialog.setProgress(values[0]);
                dialog.setSecondaryProgress(values[1]);
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
