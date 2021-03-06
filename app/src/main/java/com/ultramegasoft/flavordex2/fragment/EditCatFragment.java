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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.dialog.CatDeleteDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.radarchart.RadarHolder;
import com.ultramegasoft.radarchart.RadarView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Fragment for editing or creating an entry category.
 *
 * @author Steve Guidetti
 */
public class EditCatFragment extends LoadingProgressFragment
        implements LoaderManager.LoaderCallbacks<EditCatFragment.DataLoader.Holder> {
    /**
     * Keys for the Fragment arguments
     */
    public static final String ARG_CAT_ID = "cat_id";
    public static final String ARG_CAT_NAME = "cat_name";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_DELETE_CAT = 200;

    /**
     * Keys for the saved state
     */
    private static final String STATE_EXTRA_FIELDS = "extra_fields";
    private static final String STATE_FLAVOR_FIELDS = "flavor_fields";

    /**
     * Views from the layout
     */
    private EditText mTxtTitle;
    private TableLayout mTableExtras;
    private TableLayout mTableFlavors;
    private RadarView mRadarView;

    /**
     * The status of the extra fields
     */
    @NonNull
    private ArrayList<Field> mExtraFields = new ArrayList<>();

    /**
     * The status of the flavors
     */
    @NonNull
    private ArrayList<Field> mFlavorFields = new ArrayList<>();

    /**
     * The category ID from the arguments
     */
    private long mCatId;

    /**
     * True while data is loading
     */
    private boolean mIsLoading;

    /**
     * Interface for field listeners.
     */
    private interface CatFieldListener {
        /**
         * Should the undo delete function be allowed?
         *
         * @return Whether undo is allowed
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean allowUndo();

        /**
         * Called when the field is deleted.
         */
        void onDelete();

        /**
         * Called when the field is undeleted.
         */
        void onUndoDelete();

        /**
         * Called when the name of the field is changed.
         *
         * @param name The new name of the field
         */
        void onNameChange(String name);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if(activity == null) {
            return;
        }

        final Bundle args = getArguments();
        mCatId = args != null ? args.getLong(ARG_CAT_ID) : 0;
        setHasOptionsMenu(true);
        if(mCatId > 0) {
            activity.setTitle(getString(R.string.title_edit_cat));
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState == null) {
            if(mCatId > 0) {
                getLoaderManager().initLoader(0, null, this).forceLoad();
            } else {
                createFlavor();
                hideLoadingIndicator(false);
                if(mTxtTitle != null) {
                    mTxtTitle.requestFocus();
                }
            }
        } else {
            final ArrayList<Field> extraFields =
                    savedInstanceState.getParcelableArrayList(STATE_EXTRA_FIELDS);
            if(extraFields != null) {
                mExtraFields = extraFields;
            }
            final ArrayList<Field> flavorFields =
                    savedInstanceState.getParcelableArrayList(STATE_FLAVOR_FIELDS);
            if(flavorFields != null) {
                mFlavorFields = flavorFields;
            }
            populateFields();
            hideLoadingIndicator(false);
        }
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);

        mTxtTitle = root.findViewById(R.id.cat_name);

        mTableExtras = root.findViewById(R.id.cat_extras);
        mTableFlavors = root.findViewById(R.id.cat_flavor);
        mRadarView = root.findViewById(R.id.radar);

        root.findViewById(R.id.button_add_extra).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createExtra();
            }
        });

        root.findViewById(R.id.button_add_flavor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFlavor();
            }
        });

        return root;
    }

    @Override
    protected int getLayoutId() {
        final Bundle args = getArguments();
        final String cat = args != null ? args.getString(ARG_CAT_NAME) : null;

        if(FlavordexApp.CAT_BEER.equals(cat)) {
            return R.layout.fragment_edit_cat_beer;
        }
        if(FlavordexApp.CAT_WINE.equals(cat)) {
            return R.layout.fragment_edit_cat_wine;
        }
        if(FlavordexApp.CAT_WHISKEY.equals(cat)) {
            return R.layout.fragment_edit_cat_whiskey;
        }
        if(FlavordexApp.CAT_COFFEE.equals(cat)) {
            return R.layout.fragment_edit_cat_coffee;
        }

        return R.layout.fragment_edit_cat;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_EXTRA_FIELDS, mExtraFields);
        outState.putParcelableArrayList(STATE_FLAVOR_FIELDS, mFlavorFields);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.cat_edit_menu, menu);
        menu.findItem(R.id.menu_delete).setVisible(mCatId > 0);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_save).setEnabled(!mIsLoading);
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_save:
                saveData();
                return true;
            case R.id.menu_delete:
                confirmDeleteCat();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_DELETE_CAT:
                    final Activity activity = getActivity();
                    if(activity != null) {
                        activity.finish();
                    }
                    break;
            }
        }
    }

    /**
     * Add all the fields to the layout.
     */
    private void populateFields() {
        for(Field field : mExtraFields) {
            addExtraField(field);
        }
        for(Field field : mFlavorFields) {
            addFlavorField(field);
        }
    }

    /**
     * Create a new extra field.
     */
    private void createExtra() {
        final int count = mExtraFields.size();
        if(count > 0 && TextUtils.isEmpty(mExtraFields.get(count - 1).name)) {
            return;
        }

        final Field field = new Field(0, null);
        mExtraFields.add(field);
        addExtraField(field);
    }

    /**
     * Add the Views associated with an extra field to the layout.
     *
     * @param field The field
     */
    private void addExtraField(@NonNull final Field field) {
        final CatFieldListener listener = new CatFieldListener() {
            @Override
            public boolean allowUndo() {
                return !field.isEmpty();
            }

            @Override
            public void onDelete() {
                if(field.isEmpty()) {
                    mExtraFields.remove(field);
                } else {
                    field.delete = true;
                }
            }

            @Override
            public void onUndoDelete() {
                field.delete = false;
            }

            @Override
            public void onNameChange(String name) {
                field.name = name;
            }
        };

        addTableRow(mTableExtras, field.name, 20, R.string.hint_extra_name,
                R.string.button_remove_extra, field.delete, listener);
    }

    /**
     * Create a new flavor.
     */
    private void createFlavor() {
        final int count = mFlavorFields.size();
        if(count > 0 && TextUtils.isEmpty(mFlavorFields.get(count - 1).name)) {
            return;
        }

        final Field field = new Field(0, null);
        mFlavorFields.add(field);
        addFlavorField(field);
    }

    /**
     * Add the Views associated with a flavor to the layout.
     *
     * @param field The field
     */
    private void addFlavorField(@NonNull final Field field) {
        final CatFieldListener listener = new CatFieldListener() {
            @Override
            public boolean allowUndo() {
                return !field.isEmpty();
            }

            @Override
            public void onDelete() {
                if(field.isEmpty()) {
                    mFlavorFields.remove(field);
                } else {
                    field.delete = true;
                    mRadarView.setData(getRadarData());
                }
            }

            @Override
            public void onUndoDelete() {
                field.delete = false;
                mRadarView.setData(getRadarData());
            }

            @Override
            public void onNameChange(String name) {
                field.name = name;
                mRadarView.setData(getRadarData());
            }
        };

        addTableRow(mTableFlavors, field.name, 12, R.string.hint_flavor_name,
                R.string.button_remove_flavor, field.delete, listener);
    }

    /**
     * Add a row to the provided TableLayout. The row contains an EditText for the field name with a
     * delete button. The delete button becomes an undo button if the provided listener allows undo.
     * If undo is not allowed, the delete button will remove the row.
     *
     * @param tableLayout The TableLayout to add a row to
     * @param text        The text to fill the text field
     * @param maxLength   The maximum allowed length of the text field
     * @param hint        The hint for the EditText
     * @param deleteHint  The contentDescription for the delete Button
     * @param deleted     The initial deleted status of the field
     * @param listener    The event listener for the field
     */
    private void addTableRow(@NonNull final TableLayout tableLayout, @Nullable String text,
                             int maxLength, int hint, final int deleteHint, final boolean deleted,
                             @NonNull final CatFieldListener listener) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View root = inflater.inflate(R.layout.edit_cat_field, tableLayout, false);

        final InputFilter[] filters = new InputFilter[] {new InputFilter.LengthFilter(maxLength)};
        final EditText editText = root.findViewById(R.id.field_name);
        editText.setFilters(filters);
        editText.setHint(hint);
        editText.setText(text);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                listener.onNameChange(s.toString());
            }
        });

        final ImageButton deleteButton = root.findViewById(R.id.button_delete);
        deleteButton.setContentDescription(getString(deleteHint));
        deleteButton.setOnClickListener(new View.OnClickListener() {
            private boolean mDeleted = deleted;

            @Override
            public void onClick(View v) {
                if(mDeleted) {
                    setDeleted(false);
                    listener.onUndoDelete();
                } else {
                    if(!listener.allowUndo()) {
                        tableLayout.removeView(root);
                    } else {
                        setDeleted(true);
                    }
                    listener.onDelete();
                }
            }

            private void setDeleted(boolean deleted) {
                mDeleted = deleted;
                editText.setEnabled(!deleted);
                if(deleted) {
                    deleteButton.setImageResource(R.drawable.ic_undo);
                    deleteButton.setContentDescription(getString(R.string.button_undo));
                    editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    deleteButton.setImageResource(R.drawable.ic_clear);
                    deleteButton.setContentDescription(getString(deleteHint));
                    editText.setPaintFlags(editText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }
        });

        if(deleted) {
            editText.setEnabled(false);
            deleteButton.setImageResource(R.drawable.ic_undo);
            deleteButton.setContentDescription(getString(R.string.button_undo));
            editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        tableLayout.addView(root);
        if(TextUtils.isEmpty(text)) {
            editText.requestFocus();
        }
    }

    /**
     * Get the active flavor data for the RadarView.
     *
     * @return List of RadarHolders for the RadarView
     */
    @NonNull
    private ArrayList<RadarHolder> getRadarData() {
        final ArrayList<RadarHolder> data = new ArrayList<>();
        for(Field field : mFlavorFields) {
            if(field.isEmpty() || field.delete) {
                continue;
            }
            assert field.name != null;
            data.add(new RadarHolder(field.name, 0));
        }
        return data;
    }

    /**
     * Check if the form is properly filled out.
     *
     * @return Whether all the form fields are valid
     */
    private boolean validateForm() {
        if(mTxtTitle != null && TextUtils.isEmpty(mTxtTitle.getText().toString())) {
            mTxtTitle.setError(getString(R.string.error_required));
            mTxtTitle.requestFocus();
            return false;
        }
        return !mIsLoading;
    }

    /**
     * Save the category data and close the Activity.
     */
    private void saveData() {
        final Activity activity = getActivity();
        if(activity == null || !validateForm()) {
            return;
        }

        final ContentValues info = new ContentValues();
        if(mTxtTitle != null) {
            final String title = EntryUtils.filterName(mTxtTitle.getText().toString());
            info.put(Tables.Cats.NAME, title);
        }

        new DataSaver(activity, info, mExtraFields, mFlavorFields, mCatId).execute();

        activity.finish();
    }

    /**
     * Open the delete confirmation dialog.
     */
    private void confirmDeleteCat() {
        final FragmentManager fm = getFragmentManager();
        if(fm != null && mCatId > 0) {
            CatDeleteDialog.showDialog(fm, this, REQUEST_DELETE_CAT, mCatId);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Loader<DataLoader.Holder> onCreateLoader(int id, Bundle args) {
        final Activity activity = getActivity();
        if(activity == null) {
            return null;
        }

        mIsLoading = true;
        activity.invalidateOptionsMenu();
        return new DataLoader(activity, mCatId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<DataLoader.Holder> loader, DataLoader.Holder data) {
        mIsLoading = false;
        getLoaderManager().destroyLoader(0);
        hideLoadingIndicator(true);

        final Activity activity = getActivity();
        if(mTxtTitle != null) {
            mTxtTitle.setText(FlavordexApp.getRealCatName(activity, data.catName));
            mTxtTitle.setSelection(mTxtTitle.length());
        }
        mExtraFields = data.extras;
        mFlavorFields = data.flavors;
        populateFields();
        mRadarView.setData(getRadarData());

        if(activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onLoaderReset(@NonNull Loader<DataLoader.Holder> loader) {
    }

    /**
     * Custom Loader to load everything in one task
     */
    public static class DataLoader extends AsyncTaskLoader<DataLoader.Holder> {
        /**
         * The ContentResolver to use
         */
        @NonNull
        private final ContentResolver mResolver;

        /**
         * The category ID
         */
        private final long mCatId;

        /**
         * @param context The Context
         * @param catId   The category ID
         */
        DataLoader(@NonNull Context context, long catId) {
            super(context);
            mResolver = context.getContentResolver();
            mCatId = catId;
        }

        @Override
        public Holder loadInBackground() {
            final Holder holder = new Holder();
            final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
            loadCat(holder, uri);
            loadExtras(holder, uri);
            loadFlavors(holder, uri);
            return holder;
        }

        /**
         * Load data from the categories table.
         *
         * @param holder The Holder
         * @param catUri The Uri for the category
         */
        private void loadCat(@NonNull Holder holder, @NonNull Uri catUri) {
            final Cursor cursor = mResolver.query(catUri, null, null, null, null);
            if(cursor != null) {
                try {
                    if(cursor.moveToFirst()) {
                        holder.catName = cursor.getString(cursor.getColumnIndex(Tables.Cats.NAME));
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        /**
         * Load the custom extra fields for the category.
         *
         * @param holder The Holder
         * @param catUri The Uri for the category
         */
        private void loadExtras(@NonNull Holder holder, @NonNull Uri catUri) {
            final Uri uri = Uri.withAppendedPath(catUri, "extras");
            final String where = Tables.Extras.PRESET + " = 0";
            final String sort = Tables.Extras.POS + " ASC";
            final Cursor cursor = mResolver.query(uri, null, where, null, sort);
            if(cursor != null) {
                try {
                    long id;
                    String name;
                    boolean deleted;
                    while(cursor.moveToNext()) {
                        id = cursor.getLong(cursor.getColumnIndex(Tables.Extras._ID));
                        name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
                        deleted = cursor.getInt(cursor.getColumnIndex(Tables.Extras.DELETED)) == 1;
                        holder.extras.add(new Field(id, name, deleted));
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        /**
         * Load the flavors for the category.
         *
         * @param holder The Holder
         * @param catUri The Uri for the category
         */
        private void loadFlavors(@NonNull Holder holder, @NonNull Uri catUri) {
            final Uri uri = Uri.withAppendedPath(catUri, "flavor");
            final String sort = Tables.Flavors.POS + " ASC";
            final Cursor cursor = mResolver.query(uri, null, null, null, sort);
            if(cursor != null) {
                try {
                    long id;
                    String name;
                    while(cursor.moveToNext()) {
                        id = cursor.getLong(cursor.getColumnIndex(Tables.Flavors._ID));
                        name = cursor.getString(cursor.getColumnIndex(Tables.Flavors.NAME));
                        holder.flavors.add(new Field(id, name));
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        /**
         * The holder for return data
         */
        static class Holder {
            /**
             * The name of the category
             */
            @Nullable
            String catName;

            /**
             * The list of extra fields for the category
             */
            final ArrayList<Field> extras = new ArrayList<>();

            /**
             * The list of flavors for the category
             */
            final ArrayList<Field> flavors = new ArrayList<>();
        }
    }

    /**
     * Task for saving category data in the background.
     */
    private static class DataSaver extends AsyncTask<Void, Void, Void> {
        /**
         * The Context reference
         */
        @NonNull
        private final WeakReference<Context> mContext;

        /**
         * The ContentResolver to use
         */
        @NonNull
        private final ContentResolver mResolver;

        /**
         * The basic information for the cats table
         */
        @NonNull
        private final ContentValues mCatInfo;

        /**
         * The extra fields for the category
         */
        @NonNull
        private final ArrayList<Field> mExtras;

        /**
         * The flavors for the category
         */
        @NonNull
        private final ArrayList<Field> mFlavors;

        /**
         * The category database ID, if updating
         */
        private final long mCatId;

        /**
         * @param context The Context
         * @param catInfo The basic information for the cats table
         * @param extras  The extra fields for the category
         * @param flavors The flavors for the category
         * @param catId   The category database ID, if updating
         */
        DataSaver(@NonNull Context context, @NonNull ContentValues catInfo,
                  @NonNull ArrayList<Field> extras, @NonNull ArrayList<Field> flavors,
                  long catId) {
            mContext = new WeakReference<>(context.getApplicationContext());
            mResolver = context.getContentResolver();
            mCatInfo = catInfo;
            mExtras = extras;
            mFlavors = flavors;
            mCatId = catId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Context context = mContext.get();
            if(context == null) {
                return null;
            }

            final Uri catUri = updateCat();
            if(catUri != null) {
                updateExtras(catUri);
                updateFlavors(catUri);
            }

            return null;
        }

        /**
         * Insert or update the basic information about the category.
         *
         * @return The base Uri for the category record
         */
        @Nullable
        private Uri updateCat() {
            final Uri uri;
            if(mCatId > 0) {
                uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
                if(mCatInfo.size() > 0) {
                    mResolver.update(uri, mCatInfo, null, null);
                }
            } else {
                uri = mResolver.insert(Tables.Cats.CONTENT_URI, mCatInfo);
            }
            return uri;
        }

        /**
         * Insert, update, or delete the extra fields for the category.
         *
         * @param catUri The base Uri for the category
         */
        private void updateExtras(@NonNull Uri catUri) {
            final Uri insertUri = Uri.withAppendedPath(catUri, "extras");
            Uri uri;
            final ContentValues values = new ContentValues();
            int pos = 0;
            for(Field field : mExtras) {
                if(field.id > 0) {
                    uri = ContentUris.withAppendedId(Tables.Extras.CONTENT_ID_URI_BASE, field.id);
                    if(field.delete) {
                        mResolver.delete(uri, null, null);
                    } else {
                        values.put(Tables.Extras.NAME, EntryUtils.filterName(field.name));
                        values.put(Tables.Extras.POS, pos++);
                        values.put(Tables.Extras.DELETED, false);
                        mResolver.update(uri, values, null, null);
                    }
                } else if(!field.isEmpty()) {
                    values.put(Tables.Extras.NAME, EntryUtils.filterName(field.name));
                    values.put(Tables.Extras.POS, pos++);
                    mResolver.insert(insertUri, values);
                }
            }
        }

        /**
         * Insert, update, or delete the flavors for the category.
         *
         * @param catUri The base Uri for the category
         */
        private void updateFlavors(@NonNull Uri catUri) {
            final Uri insertUri = Uri.withAppendedPath(catUri, "flavor");
            Uri uri;
            final ContentValues values = new ContentValues();
            int pos = 0;
            for(Field field : mFlavors) {
                if(field.id > 0) {
                    uri = ContentUris.withAppendedId(Tables.Flavors.CONTENT_ID_URI_BASE, field.id);
                    if(field.delete) {
                        mResolver.delete(uri, null, null);
                    } else {
                        values.put(Tables.Flavors.NAME, field.name);
                        values.put(Tables.Flavors.POS, pos++);
                        mResolver.update(uri, values, null, null);
                    }
                } else if(!field.isEmpty()) {
                    values.put(Tables.Flavors.NAME, field.name);
                    values.put(Tables.Flavors.POS, pos++);
                    mResolver.insert(insertUri, values);
                }
            }
        }
    }

    /**
     * Holder for field data.
     */
    private static class Field implements Parcelable {
        public static final Creator<Field> CREATOR = new Creator<Field>() {
            @Override
            public Field createFromParcel(Parcel in) {
                return new Field(in);
            }

            @Override
            public Field[] newArray(int size) {
                return new Field[size];
            }
        };

        /**
         * The database ID for this field
         */
        final long id;

        /**
         * The name of this field
         */
        @Nullable
        String name;

        /**
         * Whether this field is marked for deletion
         */
        boolean delete;

        /**
         * @param id   The database ID for this field, or 0 if new
         * @param name The name of this field
         */
        Field(long id, @Nullable String name) {
            this(id, name, false);
        }

        /**
         * @param id     The database ID for this field, or 0 if new
         * @param name   The name of this field
         * @param delete The initial deleted status of the field
         */
        Field(long id, @Nullable String name, boolean delete) {
            this.id = id;
            this.name = name;
            this.delete = delete;
        }

        Field(Parcel in) {
            this.id = in.readLong();
            this.name = in.readString();
            this.delete = in.readInt() == 1;
        }

        /**
         * Is this field empty?
         *
         * @return True if the ID is 0 and the name is blank
         */
        boolean isEmpty() {
            return id == 0 && TextUtils.isEmpty(name);
        }

        @SuppressWarnings("SameReturnValue")
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            dest.writeString(name);
            dest.writeInt(delete ? 1 : 0);
        }
    }
}
