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
import android.text.InputFilter;
import android.text.TextUtils;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.provider.Tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Fragment for adding details for a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddInfoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Loader ids
     */
    private static final int LOADER_EXTRAS = 0;

    /**
     * The views for the form fields
     */
    private EditText mTxtTitle;
    private AutoCompleteTextView mTxtMaker;
    private EditText mTxtOrigin;
    private EditText mTxtLocation;
    private EditText mTxtPrice;
    private RatingBar mRatingBar;
    private EditText mTxtNotes;

    /**
     * The table layout for the main info
     */
    private TableLayout mInfoTable;

    /**
     * The type id for the entry being added
     */
    private long mTypeId;

    /**
     * Map of extra field names to their database ids
     */
    private HashMap<String, Long> mExtraIds = new HashMap<>();

    /**
     * List of extra fields
     */
    private ArrayList<EditText> mExtraFields = new ArrayList<>();

    public AddInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTypeId = getArguments().getLong(AddEntryFragment.ARG_TYPE_ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_EXTRAS, null, this);
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(getLayoutId(), container, false);

        mTxtTitle = (EditText)root.findViewById(R.id.entry_title);
        mTxtMaker = (AutoCompleteTextView)root.findViewById(R.id.entry_maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.entry_origin);
        mTxtLocation = (EditText)root.findViewById(R.id.entry_location);
        mTxtPrice = (EditText)root.findViewById(R.id.entry_price);
        mRatingBar = (RatingBar)root.findViewById(R.id.entry_rating);
        mTxtNotes = (EditText)root.findViewById(R.id.entry_notes);

        mInfoTable = (TableLayout)root.findViewById(R.id.entry_info);

        setupMakersAutoComplete();

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTxtTitle.requestFocus();
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
     * Load the extra fields from the database.
     *
     * @param cursor The cursor returned from the database query
     */
    private void loadExtras(Cursor cursor) {
        mExtraFields.clear();
        mExtraIds.clear();

        if(cursor == null) {
            return;
        }

        long id;
        String name;
        while(cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex(Tables.Extras._ID));
            name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
            mExtraIds.put(name, id);
            addExtraRow(name);
        }
    }

    /**
     * Add an extra field to the interface.
     *
     * @param name The name of the field
     */
    protected void addExtraRow(String name) {
        final TableRow tableRow = new TableRow(getActivity());

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, metrics);

        final TextView label = new TextView(getActivity());
        label.setPadding(padding, 0, padding, 0);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        label.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        label.setText(name + ": ");
        tableRow.addView(label);

        final EditText editText = new EditText(getActivity());

        editText.setSingleLine();
        editText.setEllipsize(TextUtils.TruncateAt.END);
        editText.setWidth(0);
        editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(256)});
        editText.setTag(name);
        editText.setHint(name);
        tableRow.addView(editText);

        mExtraFields.add(editText);

        mInfoTable.addView(tableRow);
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
    public ContentValues getData() {
        final ContentValues values = new ContentValues();

        values.put(Tables.Entries.TYPE, mTypeId);
        values.put(Tables.Entries.DATE, System.currentTimeMillis());

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
     * Read the values from the extra fields into the supplied HashMap, mapping field names to
     * values.
     *
     * @param values A HashMap to add values to
     */
    protected void readExtras(HashMap<String, String> values) {
        for(EditText field : mExtraFields) {
            values.put(field.getTag().toString(), field.getText().toString());
        }
    }

    /**
     * Get the values of the extra fields as an array of ContentValues objects ready to be bulk
     * inserted into the entries_extras database table.
     *
     * @return Array of ContentValues containing data for the entries_extras table
     */
    public ContentValues[] getExtras() {
        final HashMap<String, String> fieldValues = new HashMap<>();
        readExtras(fieldValues);

        final ArrayList<ContentValues> values = new ArrayList<>();
        ContentValues rowValues;
        for(Map.Entry<String, String> entry : fieldValues.entrySet()) {
            rowValues = new ContentValues();
            rowValues.put(Tables.EntriesExtras.EXTRA, mExtraIds.get(entry.getKey()));
            rowValues.put(Tables.EntriesExtras.VALUE, entry.getValue());

            values.add(rowValues);
        }

        return values.toArray(new ContentValues[values.size()]);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_EXTRAS:
                final Uri uri = ContentUris.withAppendedId(Tables.Types.CONTENT_ID_URI_BASE,
                        mTypeId);
                return new CursorLoader(getActivity(), Uri.withAppendedPath(uri, "/extras"), null,
                        null, null, Tables.Extras._ID + " ASC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_EXTRAS:
                loadExtras(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
