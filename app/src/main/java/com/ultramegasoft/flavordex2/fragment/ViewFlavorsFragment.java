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
package com.ultramegasoft.flavordex2.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.dialog.ConfirmationDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.radarchart.RadarEditWidget;
import com.ultramegasoft.radarchart.RadarHolder;
import com.ultramegasoft.radarchart.RadarView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Fragment to display and edit the flavor radar chart of a journal entry.
 *
 * @author Steve Guidetti
 */
public class ViewFlavorsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Loader IDs
     */
    private static final int LOADER_FLAVOR = 0;
    private static final int LOADER_DEFAULT_FLAVOR = 1;
    private static final int LOADER_RESET_FLAVOR = 2;

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_RESET = 600;

    /**
     * Keys for the saved state of this Fragment
     */
    private static final String STATE_EDIT_MODE = "edit_mode";
    private static final String STATE_DATA = "flavor_data";

    /**
     * The Views from the layout
     */
    private RadarView mRadarView;
    private RadarEditWidget mEditWidget;

    /**
     * Animations for the editing layout
     */
    @Nullable
    private Animation mInAnimation;
    @Nullable
    private Animation mOutAnimation;

    /**
     * The entry's flavor data
     */
    @Nullable
    private ArrayList<RadarHolder> mData;

    /**
     * Whether the radar chart is in edit mode
     */
    private boolean mEditMode;

    /**
     * The database ID for this entry
     */
    private long mEntryId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if(args != null) {
            mEntryId = args.getLong(ViewEntryFragment.ARG_ENTRY_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(savedInstanceState != null) {
            mData = savedInstanceState.getParcelableArrayList(STATE_DATA);
            mEditMode = savedInstanceState.getBoolean(STATE_EDIT_MODE, false);
            mRadarView.setVisibility(View.VISIBLE);
        } else if(mData == null) {
            getLoaderManager().initLoader(LOADER_FLAVOR, null, this);
        } else {
            mRadarView.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_view_flavors, container, false);

        mRadarView = root.findViewById(R.id.radar);
        mRadarView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if(!mRadarView.isInteractive()) {
                    setEditMode(true, true);
                    return true;
                }
                return false;
            }
        });

        mEditWidget = root.findViewById(R.id.edit_widget);
        mEditWidget.setTarget(mRadarView);
        mEditWidget.setOnButtonClickListener(new RadarEditWidget.OnButtonClickListener() {
            @Override
            public void onSave() {
                saveData();
            }

            @Override
            public void onCancel() {
                cancelEdit();
            }
        });

        return root;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        setEditMode(mEditMode, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRadarView = null;
        mEditWidget = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_EDIT_MODE, mEditMode);
        outState.putParcelableArrayList(STATE_DATA, mData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setHasOptionsMenu(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.view_flavor_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_edit_flavor).setEnabled(!mEditMode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_edit_flavor:
                setEditMode(true, true);
                return true;
            case R.id.menu_reset_flavor:
                final FragmentManager fm = getFragmentManager();
                if(fm != null) {
                    ConfirmationDialog.showDialog(fm, this, REQUEST_RESET,
                            getString(R.string.title_reset_flavor),
                            getString(R.string.message_reset_flavor));
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_RESET:
                    getLoaderManager().initLoader(LOADER_RESET_FLAVOR, null, this);
                    break;
            }
        }
    }

    /**
     * Enable or disable the RadarView's interactive mode and show or hide the editing layout.
     *
     * @param editMode Whether to enable edit mode
     * @param animate  Whether to animate the edit interface sliding in
     */
    private void setEditMode(boolean editMode, boolean animate) {
        if(animate && mInAnimation == null) {
            mInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.flavor_edit_in);
            mOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.flavor_edit_out);
        }

        if(editMode) {
            mEditWidget.setVisibility(View.VISIBLE);
            if(animate) {
                mEditWidget.startAnimation(mInAnimation);
            }
        } else {
            if(mEditWidget != null) {
                if(animate) {
                    mEditWidget.startAnimation(mOutAnimation);
                }
                mEditWidget.setVisibility(View.INVISIBLE);
            }
        }

        mRadarView.setInteractive(editMode);
        mEditMode = editMode;

        final Activity activity = getActivity();
        if(activity != null) {
            ActivityCompat.invalidateOptionsMenu(activity);
        }
    }

    /**
     * Save the current flavor data to the database.
     */
    private void saveData() {
        final Context context = getContext();
        if(context == null) {
            cancelEdit();
            return;
        }

        setEditMode(false, true);
        mData = mRadarView.getData();
        if(mData != null) {
            new DataSaver(context, mEntryId, mData).execute();
        }
    }

    /**
     * Reset the radar chart to the original data and disable edit mode.
     */
    private void cancelEdit() {
        setEditMode(false, true);
        mRadarView.setData(mData);
    }

    /**
     * Called when the back button is pressed.
     *
     * @return Whether the back button press was intercepted
     */
    public boolean onBackButtonPressed() {
        if(mEditMode) {
            cancelEdit();
            return true;
        }
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        final Context context = getContext();
        if(context == null) {
            return null;
        }

        Uri uri;
        switch(id) {
            case LOADER_FLAVOR:
                uri = Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE,
                        mEntryId + "/flavor");
                return new CursorLoader(context, uri, null, null, null,
                        Tables.EntriesFlavors.POS + " ASC");
            case LOADER_DEFAULT_FLAVOR:
            case LOADER_RESET_FLAVOR:
                final Bundle args = getArguments();
                final long catId =
                        args != null ? args.getLong(ViewEntryFragment.ARG_ENTRY_CAT_ID) : 0;
                uri = Uri.withAppendedPath(Tables.Cats.CONTENT_ID_URI_BASE, catId + "/flavor");
                return new CursorLoader(context, uri, null, null, null,
                        Tables.Flavors.POS + " ASC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null) {
            return;
        }
        data.moveToPosition(-1);
        final ArrayList<RadarHolder> flavorValues = new ArrayList<>();

        String name;
        int value;
        switch(loader.getId()) {
            case LOADER_FLAVOR:
                while(data.moveToNext()) {
                    name = data.getString(data.getColumnIndex(Tables.EntriesFlavors.FLAVOR));
                    value = data.getInt(data.getColumnIndex(Tables.EntriesFlavors.VALUE));
                    flavorValues.add(new RadarHolder(name, value));
                }
                if(flavorValues.isEmpty()) {
                    getLoaderManager().initLoader(LOADER_DEFAULT_FLAVOR, null, this);
                    break;
                }
                mData = flavorValues;
                if(mRadarView.getVisibility() != View.VISIBLE) {
                    mRadarView.startAnimation(AnimationUtils.loadAnimation(getContext(),
                            android.R.anim.fade_in));
                }
                if(!mEditMode) {
                    mRadarView.setData(flavorValues);
                }
                break;
            case LOADER_DEFAULT_FLAVOR:
            case LOADER_RESET_FLAVOR:
                while(data.moveToNext()) {
                    name = data.getString(data.getColumnIndex(Tables.Flavors.NAME));
                    flavorValues.add(new RadarHolder(name, 0));
                }

                mRadarView.setData(flavorValues);
                if(loader.getId() == LOADER_RESET_FLAVOR) {
                    if(!mEditMode) {
                        setEditMode(true, true);
                    } else {
                        mRadarView.turnTo(0);
                    }
                }
                break;
        }
        mRadarView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Task to save the data in the background.
     */
    private static class DataSaver extends AsyncTask<Void, Void, Void> {
        /**
         * The Context reference
         */
        @NonNull
        private final WeakReference<Context> mContext;

        /**
         * The entry ID to save the flavors to
         */
        private final long mEntryId;

        /**
         * The radar chart data to insert
         */
        @NonNull
        private final ArrayList<RadarHolder> mData;

        /**
         * @param context The Context
         * @param entryId The entry to save flavors to
         * @param data    The radar chart data to insert
         */
        DataSaver(@NonNull Context context, long entryId, @NonNull ArrayList<RadarHolder> data) {
            mContext = new WeakReference<>(context.getApplicationContext());
            mEntryId = entryId;
            mData = data;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            final Context context = mContext.get();
            if(context == null) {
                return null;
            }

            final ContentResolver cr = context.getContentResolver();
            Uri uri =
                    Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId + "/flavor");
            final ContentValues[] valuesArray = new ContentValues[mData.size()];
            ContentValues values;
            RadarHolder item;
            for(int i = 0; i < valuesArray.length; i++) {
                item = mData.get(i);
                values = new ContentValues();
                values.put(Tables.EntriesFlavors.FLAVOR, item.name);
                values.put(Tables.EntriesFlavors.VALUE, item.value);
                values.put(Tables.EntriesFlavors.POS, i);
                valuesArray[i] = values;
            }
            cr.bulkInsert(uri, valuesArray);

            EntryUtils.markChanged(cr, mEntryId);

            BackendUtils.requestDataSync(context);

            return null;
        }
    }
}
