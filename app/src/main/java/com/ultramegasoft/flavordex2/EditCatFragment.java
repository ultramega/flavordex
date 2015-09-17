package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
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

import com.ultramegasoft.flavordex2.dialog.CatDeleteDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.InputUtils;
import com.ultramegasoft.flavordex2.widget.RadarHolder;
import com.ultramegasoft.flavordex2.widget.RadarView;

import java.util.ArrayList;

/**
 * Fragment for editing or creating an entry category.
 *
 * @author Steve Guidetti
 */
public class EditCatFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keys for the Fragment arguments
     */
    public static final String ARG_CAT_ID = "cat_id";
    public static final String ARG_CAT_NAME = "cat_name";

    /**
     * Loader IDs
     */
    private static final int LOADER_CAT = 0;
    private static final int LOADER_EXTRAS = 1;
    private static final int LOADER_FLAVOR = 2;

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_DELETE_CAT = 100;

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
    private ArrayList<Field> mExtraFields = new ArrayList<>();

    /**
     * The status of the flavors
     */
    private ArrayList<Field> mFlavorFields = new ArrayList<>();

    /**
     * The category ID from the arguments
     */
    private long mCatId;

    /**
     * Interface for field listeners.
     */
    private interface CatFieldListener {
        /**
         * Should the undo delete function be allowed?
         *
         * @return Whether undo is allowed
         */
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCatId = getArguments().getLong(ARG_CAT_ID);
        setHasOptionsMenu(true);
        if(mCatId > 0) {
            getActivity().setTitle(getString(R.string.title_edit_cat));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState == null) {
            if(mCatId > 0) {
                getLoaderManager().initLoader(LOADER_CAT, null, this);
                getLoaderManager().initLoader(LOADER_EXTRAS, null, this);
                getLoaderManager().initLoader(LOADER_FLAVOR, null, this);
            } else {
                addFlavor(0, null);
                mRadarView.setVisibility(View.VISIBLE);
                if(mTxtTitle != null) {
                    mTxtTitle.requestFocus();
                }
            }
        } else {
            mExtraFields = savedInstanceState.getParcelableArrayList(STATE_EXTRA_FIELDS);
            mFlavorFields = savedInstanceState.getParcelableArrayList(STATE_FLAVOR_FIELDS);

            for(int i = 0; i < mExtraFields.size(); i++) {
                addExtraField(i);
            }

            for(int i = 0; i < mFlavorFields.size(); i++) {
                addFlavorField(i);
            }

            mRadarView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(getLayoutId(), container, false);

        mTxtTitle = (EditText)root.findViewById(R.id.cat_name);
        InputUtils.addFilter(mTxtTitle, InputUtils.NAME_FILTER);

        mTableExtras = (TableLayout)root.findViewById(R.id.cat_extras);
        mTableFlavors = (TableLayout)root.findViewById(R.id.cat_flavor);
        mRadarView = (RadarView)root.findViewById(R.id.radar);

        root.findViewById(R.id.button_add_extra).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addExtra(0, null, false);
            }
        });

        root.findViewById(R.id.button_add_flavor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFlavor(0, null);
            }
        });

        return root;
    }

    /**
     * Get the ID for the layout to use.
     *
     * @return An ID from R.layout
     */
    private int getLayoutId() {
        final String cat = getArguments().getString(ARG_CAT_NAME);

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
    public void onSaveInstanceState(Bundle outState) {
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
                    getActivity().finish();
                    break;
            }
        }
    }

    /**
     * Add an extra field.
     *
     * @param id      The database ID, if any
     * @param name    The name of the field
     * @param deleted Whether to show the field as deleted
     */
    private void addExtra(long id, String name, boolean deleted) {
        final int count = mExtraFields.size();
        if(count > 0 && TextUtils.isEmpty(mExtraFields.get(count - 1).name)) {
            return;
        }
        if(mExtraFields.add(new Field(id, name, deleted))) {
            addExtraField(count);
        }
    }

    /**
     * Add the Views associated with an extra field to the layout.
     *
     * @param i The array index of the extra field
     */
    private void addExtraField(int i) {
        final Field field = mExtraFields.get(i);
        if(field == null) {
            return;
        }

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
     * Add a flavor to the radar chart.
     *
     * @param id   The database ID of the flavor, if any
     * @param name The name of the flavor
     */
    private void addFlavor(long id, String name) {
        final int count = mFlavorFields.size();
        if(count > 0 && TextUtils.isEmpty(mFlavorFields.get(count - 1).name)) {
            return;
        }
        if(mFlavorFields.add(new Field(id, name))) {
            addFlavorField(count);
        }
    }

    /**
     * Add the Views associated with a flavor to the layout.
     *
     * @param i The array index of the flavor
     */
    private void addFlavorField(int i) {
        final Field field = mFlavorFields.get(i);
        if(field == null) {
            return;
        }

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
    private void addTableRow(final TableLayout tableLayout, String text, int maxLength, int hint,
                             final int deleteHint, final boolean deleted,
                             final CatFieldListener listener) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View root = inflater.inflate(R.layout.cat_edit_field, tableLayout, false);

        final InputFilter[] filters = new InputFilter[] {
                InputUtils.NAME_FILTER,
                new InputFilter.LengthFilter(maxLength)
        };
        final EditText editText = (EditText)root.findViewById(R.id.field_name);
        editText.setSaveEnabled(false);
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

        final ImageButton deleteButton = (ImageButton)root.findViewById(R.id.button_delete);
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
    private ArrayList<RadarHolder> getRadarData() {
        final ArrayList<RadarHolder> data = new ArrayList<>();
        for(Field field : mFlavorFields) {
            if(field.isEmpty() || field.delete) {
                continue;
            }
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
        return true;
    }

    /**
     * Save the category data and close the Activity.
     */
    private void saveData() {
        if(!validateForm()) {
            return;
        }

        final ContentValues info = new ContentValues();
        if(mTxtTitle != null) {
            info.put(Tables.Cats.NAME, mTxtTitle.getText().toString());
        }

        new DataSaver(getContext().getContentResolver(), info, mExtraFields, mFlavorFields, mCatId)
                .execute();

        getActivity().finish();
    }

    /**
     * Open the delete confirmation dialog.
     */
    private void confirmDeleteCat() {
        if(mCatId > 0) {
            CatDeleteDialog.showDialog(getFragmentManager(), this, REQUEST_DELETE_CAT, mCatId);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
        switch(id) {
            case LOADER_CAT:
                return new CursorLoader(getContext(), uri, null, null, null, null);
            case LOADER_EXTRAS:
                return new CursorLoader(getContext(), Uri.withAppendedPath(uri, "extras"), null,
                        Tables.Extras.PRESET + " = 0", null, Tables.Extras._ID + " ASC");
            case LOADER_FLAVOR:
                return new CursorLoader(getContext(), Uri.withAppendedPath(uri, "flavor"), null,
                        null, null, Tables.Flavors._ID + " ASC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_CAT:
                if(data.moveToFirst()) {
                    if(mTxtTitle != null) {
                        final String name = data.getString(data.getColumnIndex(Tables.Cats.NAME));
                        mTxtTitle.setText(FlavordexApp.getRealCatName(getContext(), name));
                    }
                }
                break;
            case LOADER_EXTRAS:
                while(data.moveToNext()) {
                    addExtra(data.getLong(data.getColumnIndex(Tables.Extras._ID)),
                            data.getString(data.getColumnIndex(Tables.Extras.NAME)),
                            data.getInt(data.getColumnIndex(Tables.Extras.DELETED)) == 1);
                }
                break;
            case LOADER_FLAVOR:
                while(data.moveToNext()) {
                    addFlavor(data.getLong(data.getColumnIndex(Tables.Flavors._ID)),
                            data.getString(data.getColumnIndex(Tables.Flavors.NAME)));
                }
                mRadarView.setData(getRadarData());
                mRadarView.setVisibility(View.VISIBLE);
                break;
        }

        getLoaderManager().destroyLoader(loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Task for saving category data in the background.
     */
    private static class DataSaver extends AsyncTask<Void, Void, Void> {
        /**
         * The ContentResolver to use
         */
        private final ContentResolver mResolver;

        /**
         * The basic information for the cats table
         */
        private final ContentValues mCatInfo;

        /**
         * The extra fields for the category
         */
        private final ArrayList<Field> mExtras;

        /**
         * The flavors for the category
         */
        private final ArrayList<Field> mFlavors;

        /**
         * The category database ID, if updating
         */
        private final long mCatId;

        /**
         * @param cr      The ContentResolver to use
         * @param catInfo The basic information for the cats table
         * @param extras  The extra fields for the category
         * @param flavors The flavors for the category
         * @param catId   The category database ID, if updating
         */
        public DataSaver(ContentResolver cr, ContentValues catInfo, ArrayList<Field> extras,
                         ArrayList<Field> flavors, long catId) {
            mResolver = cr;
            mCatInfo = catInfo;
            mExtras = extras;
            mFlavors = flavors;
            mCatId = catId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Uri catUri = insertCat();
            if(mExtras != null) {
                insertExtras(catUri);
            }
            if(mFlavors != null) {
                insertFlavors(catUri);
            }
            return null;
        }

        /**
         * Insert or update the basic information about the category.
         *
         * @return The base Uri for the category record
         */
        private Uri insertCat() {
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
        private void insertExtras(Uri catUri) {
            final Uri insertUri = Uri.withAppendedPath(catUri, "extras");
            Uri uri;
            final ContentValues values = new ContentValues();
            for(Field field : mExtras) {
                if(field.id > 0) {
                    uri = ContentUris.withAppendedId(Tables.Extras.CONTENT_ID_URI_BASE, field.id);
                    if(field.delete) {
                        mResolver.delete(uri, null, null);
                    } else {
                        values.put(Tables.Extras.NAME, field.name);
                        values.put(Tables.Extras.DELETED, false);
                        mResolver.update(uri, values, null, null);
                    }
                } else if(!field.isEmpty()) {
                    values.put(Tables.Extras.NAME, field.name);
                    mResolver.insert(insertUri, values);
                }
            }
        }

        /**
         * Insert, update, or delete the flavors for the category.
         *
         * @param catUri The base Uri for the category
         */
        private void insertFlavors(Uri catUri) {
            final Uri insertUri = Uri.withAppendedPath(catUri, "flavor");
            Uri uri;
            final ContentValues values = new ContentValues();
            for(Field field : mFlavors) {
                if(field.id > 0) {
                    uri = ContentUris.withAppendedId(Tables.Flavors.CONTENT_ID_URI_BASE, field.id);
                    if(field.delete) {
                        mResolver.delete(uri, null, null);
                    } else {
                        values.put(Tables.Flavors.NAME, field.name);
                        mResolver.update(uri, values, null, null);
                    }
                } else if(!field.isEmpty()) {
                    values.put(Tables.Flavors.NAME, field.name);
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
        public final long id;

        /**
         * The name of this field
         */
        public String name;

        /**
         * Whether this field is marked for deletion
         */
        public boolean delete;

        /**
         * @param id   The database ID for this field, or 0 if new
         * @param name The name of this field
         */
        public Field(long id, String name) {
            this(id, name, false);
        }

        /**
         * @param id     The database ID for this field, or 0 if new
         * @param name   The name of this field
         * @param delete The initial deleted status of the field
         */
        public Field(long id, String name, boolean delete) {
            this.id = id;
            this.name = name;
            this.delete = delete;
        }

        Field(Parcel in) {
            this.id = in.readLong();
            this.name = in.readString();
            final boolean[] booleans = new boolean[1];
            in.readBooleanArray(booleans);
            this.delete = booleans[0];
        }

        /**
         * Is this field empty?
         *
         * @return True if the ID is 0 and the name is blank
         */
        public boolean isEmpty() {
            return id == 0 && TextUtils.isEmpty(name);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            dest.writeString(name);
            dest.writeBooleanArray(new boolean[] {delete});
        }
    }
}
