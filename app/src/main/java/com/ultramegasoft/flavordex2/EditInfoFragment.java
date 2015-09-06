package com.ultramegasoft.flavordex2;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.RatingBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Fragment for editing details for a new or existing journal entry.
 *
 * @author Steve Guidetti
 */
public class EditInfoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keys for the fragment arguments
     */
    public static final String ARG_ENTRY_ID = "entry_id";

    /**
     * Loader ids
     */
    private static final int LOADER_ENTRY = 0;
    private static final int LOADER_EXTRAS = 1;

    /**
     * Keys for the saved state
     */
    private static final String STATE_EXTRAS = "extras";

    /**
     * The views for the form fields
     */
    private EditText mTxtTitle;
    private AutoCompleteTextView mTxtMaker;
    private EditText mTxtOrigin;
    private EditText mTxtPrice;
    private EditText mTxtLocation;
    private RatingBar mRatingBar;
    private EditText mTxtNotes;

    /**
     * The table layout for the main info
     */
    private TableLayout mInfoTable;

    /**
     * The category id for the entry being added
     */
    private long mCatId;

    /**
     * The entry id to edit
     */
    private long mEntryId;

    /**
     * Map of extra fields to their data
     */
    private HashMap<String, ExtraFieldHolder> mExtras = new HashMap<>();

    public EditInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCatId = getArguments().getLong(AddEntryFragment.ARG_CAT_ID);
        mEntryId = getArguments().getLong(ARG_ENTRY_ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState == null) {
            if(mEntryId > 0) {
                getLoaderManager().initLoader(LOADER_ENTRY, null, this);
            }
            getLoaderManager().initLoader(LOADER_EXTRAS, null, this);
        } else {
            mExtras = (HashMap<String, ExtraFieldHolder>)savedInstanceState.getSerializable(STATE_EXTRAS);
            populateExtras(mExtras);
        }
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(getLayoutId(), container, false);

        mTxtTitle = (EditText)root.findViewById(R.id.entry_title);
        mTxtMaker = (AutoCompleteTextView)root.findViewById(R.id.entry_maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.entry_origin);
        mTxtPrice = (EditText)root.findViewById(R.id.entry_price);
        mTxtLocation = (EditText)root.findViewById(R.id.entry_location);
        mRatingBar = (RatingBar)root.findViewById(R.id.entry_rating);
        mTxtNotes = (EditText)root.findViewById(R.id.entry_notes);

        mInfoTable = (TableLayout)root.findViewById(R.id.entry_info);

        mTxtLocation.setText(((FlavordexApp)getActivity().getApplication()).getLocationName());
        setupMakersAutoComplete();

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTxtTitle.requestFocus();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_EXTRAS, mExtras);
    }

    /**
     * Get the id for the layout to use.
     *
     * @return An id from R.layout
     */
    protected int getLayoutId() {
        return R.layout.fragment_add_info;
    }

    /**
     * Set up the autocomplete for the maker field.
     */
    private void setupMakersAutoComplete() {
        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.simple_dropdown_item_2line, null,
                new String[] {Tables.Makers.NAME, Tables.Makers.LOCATION},
                new int[] {android.R.id.text1, android.R.id.text2}, 0);

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                final String order = Tables.Makers.NAME + " ASC";
                final Uri uri;
                if(TextUtils.isEmpty(constraint)) {
                    uri = Tables.Makers.CONTENT_URI;
                } else {
                    uri = Uri.withAppendedPath(Tables.Makers.CONTENT_FILTER_URI_BASE,
                            constraint.toString());
                }
                return getActivity().getContentResolver().query(uri, null, null, null, order);
            }
        });

        mTxtMaker.setAdapter(adapter);

        // fill in maker and origin fields with a suggestion
        mTxtMaker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Cursor cursor = (Cursor)parent.getItemAtPosition(position);
                cursor.moveToPosition(position);

                final String name = cursor.getString(cursor.getColumnIndex(Tables.Makers.NAME));
                final String origin =
                        cursor.getString(cursor.getColumnIndex(Tables.Makers.LOCATION));
                mTxtMaker.setText(name);
                mTxtOrigin.setText(origin);

                // skip origin field
                mTxtOrigin.focusSearch(View.FOCUS_DOWN).requestFocus();
            }
        });
    }

    /**
     * Load the main details of the entry being edited.
     *
     * @param cursor The cursor returned from the database query
     */
    private void loadEntry(Cursor cursor) {
        if(cursor == null) {
            return;
        }

        if(cursor.moveToFirst()) {
            mTxtTitle.setText(cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE)));
            mTxtMaker.setText(cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER)));
            mTxtOrigin.setText(cursor.getString(cursor.getColumnIndex(Tables.Entries.ORIGIN)));
            mTxtPrice.setText(cursor.getString(cursor.getColumnIndex(Tables.Entries.PRICE)));
            mTxtLocation.setText(cursor.getString(cursor.getColumnIndex(Tables.Entries.LOCATION)));
            mRatingBar.setRating(cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING)));
            mTxtNotes.setText(cursor.getString(cursor.getColumnIndex(Tables.Entries.NOTES)));
        }
    }

    /**
     * Load the extra fields from the database.
     *
     * @param cursor The cursor returned from the database query
     */
    private void loadExtras(Cursor cursor) {
        if(cursor == null) {
            return;
        }

        long id;
        String name;
        boolean preset;
        while(cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex(
                    mEntryId > 0 ? Tables.EntriesExtras.EXTRA : Tables.Extras._ID));
            name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
            preset = cursor.getInt(cursor.getColumnIndex(Tables.Extras.PRESET)) == 1;

            final ExtraFieldHolder extra = new ExtraFieldHolder(id, name, preset);

            if(mEntryId > 0) {
                extra.value = cursor.getString(cursor.getColumnIndex(Tables.EntriesExtras.VALUE));
            }

            mExtras.put(extra.name, extra);
        }

        populateExtras(mExtras);
    }

    /**
     * Create and set up the extra field views.
     *
     * @param extras A map of extra names to the extra field
     */
    protected void populateExtras(HashMap<String, ExtraFieldHolder> extras) {
        for(ExtraFieldHolder extra : extras.values()) {
            if(!extra.preset) {
                addExtraRow(extra);
            }
        }
    }

    /**
     * Add an extra field view to the interface.
     *
     * @param extra The extra field
     */
    private void addExtraRow(ExtraFieldHolder extra) {
        final TableRow tableRow = new TableRow(getActivity());

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, metrics);

        final TextView label = new TextView(getActivity());
        label.setPadding(padding, 0, padding, 0);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        label.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        label.setText(extra.name + ": ");
        tableRow.addView(label);

        final EditText editText = new EditText(getActivity());

        editText.setSingleLine();
        editText.setEllipsize(TextUtils.TruncateAt.END);
        editText.setWidth(0);
        editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(256)});
        editText.setHint(extra.name);
        tableRow.addView(editText);

        initEditText(editText, extra);

        mInfoTable.addView(tableRow);
    }

    /**
     * Set up an EditText with an extra field.
     *
     * @param editText The EditText
     * @param extra    The extra field to associate with the view
     */
    protected static void initEditText(EditText editText, final ExtraFieldHolder extra) {
        editText.setText(extra.value);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                extra.value = s.toString();
            }
        });
    }

    /**
     * Set up a Spinner with an extra field.
     *
     * @param spinner The Spinner
     * @param extra   The extra field to associate with the view
     */
    protected static void initSpinner(Spinner spinner, final ExtraFieldHolder extra) {
        if(extra.value != null) {
            spinner.setSelection(Integer.valueOf(extra.value));
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                extra.value = position + "";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * Test if the required fields are properly filled out.
     *
     * @return Whether the form is valid
     */
    public boolean isValid() {
        return !TextUtils.isEmpty(mTxtTitle.getText().toString());
    }

    /**
     * Read the primary fields into a ContentValues object ready to be inserted into the entries
     * database table.
     *
     * @return ContentValues containing the data for the entries table
     */
    public final ContentValues getData() {
        final ContentValues values = new ContentValues();

        if(mEntryId == 0) {
            values.put(Tables.Entries.CAT, mCatId);
            values.put(Tables.Entries.DATE, System.currentTimeMillis());
        }

        values.put(Tables.Entries.TITLE, mTxtTitle.getText().toString());
        values.put(Tables.Entries.MAKER, mTxtMaker.getText().toString());
        values.put(Tables.Entries.ORIGIN, mTxtOrigin.getText().toString());
        values.put(Tables.Entries.LOCATION, mTxtLocation.getText().toString());
        values.put(Tables.Entries.PRICE, mTxtPrice.getText().toString());
        values.put(Tables.Entries.RATING, mRatingBar.getRating());
        values.put(Tables.Entries.NOTES, mTxtNotes.getText().toString());

        return values;
    }

    /**
     * Get the values of the extra fields as an array of ContentValues objects ready to be bulk
     * inserted into the entries_extras database table.
     *
     * @return Array of ContentValues containing data for the entries_extras table
     */
    public final ContentValues[] getExtras() {
        final ArrayList<ContentValues> values = new ArrayList<>();
        ContentValues rowValues;
        for(ExtraFieldHolder extra : mExtras.values()) {
            rowValues = new ContentValues();
            rowValues.put(Tables.EntriesExtras.EXTRA, extra.id);
            rowValues.put(Tables.EntriesExtras.VALUE, extra.value);

            values.add(rowValues);
        }

        return values.toArray(new ContentValues[values.size()]);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri baseUri;
        if(mCatId > 0) {
            baseUri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
        } else {
            baseUri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
        }
        switch(id) {
            case LOADER_ENTRY:
                return new CursorLoader(getActivity(), baseUri, null, null, null, null);
            case LOADER_EXTRAS:
                final String sort;
                if(mCatId > 0) {
                    sort = Tables.Extras._ID + " ASC";
                } else {
                    sort = Tables.EntriesExtras.EXTRA + " ASC";
                }
                return new CursorLoader(getActivity(), Uri.withAppendedPath(baseUri, "/extras"),
                        null, null, null, sort);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_ENTRY:
                loadEntry(data);
                break;
            case LOADER_EXTRAS:
                loadExtras(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
