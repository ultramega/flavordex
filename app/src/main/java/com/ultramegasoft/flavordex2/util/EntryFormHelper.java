package com.ultramegasoft.flavordex2.util;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper containing common functionality for entry input forms.
 *
 * @author Steve Guidetti
 */
public class EntryFormHelper implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Loader IDs
     */
    private static final int LOADER_MAKERS = 100;

    /**
     * The Fragment using the helper object.
     */
    protected final Fragment mFragment;

    /**
     * Views from the layout
     */
    public EditText mTxtTitle;
    public AutoCompleteTextView mTxtMaker;
    public EditText mTxtOrigin;
    public EditText mTxtPrice;
    public EditText mTxtLocation;

    /**
     * The TableLayout for the main info
     */
    private TableLayout mInfoTable;

    /**
     * Map of extra field names to their data
     */
    private final LinkedHashMap<String, ExtraFieldHolder> mExtras = new LinkedHashMap<>();

    /**
     * Map of extra fields to the input views
     */
    private final HashMap<ExtraFieldHolder, EditText> mExtraViews = new HashMap<>();

    /**
     * @param fragment   The Fragment using this helper object
     * @param layoutRoot The root of the layout
     */
    public EntryFormHelper(Fragment fragment, View layoutRoot) {
        mFragment = fragment;
        loadLayout(layoutRoot);
        setupMakersAutoComplete();
    }

    /**
     * Load the elements from the layout.
     *
     * @param root The root of the layout
     */
    protected void loadLayout(View root) {
        mInfoTable = (TableLayout)root.findViewById(R.id.entry_info);

        mTxtTitle = (EditText)root.findViewById(R.id.entry_title);
        mTxtMaker = (AutoCompleteTextView)root.findViewById(R.id.entry_maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.entry_origin);
        mTxtPrice = (EditText)root.findViewById(R.id.entry_price);
        mTxtLocation = (EditText)root.findViewById(R.id.entry_location);
    }

    /**
     * Set up the extra fields in the form.
     *
     * @param extras The list of extras
     */
    public void setExtras(LinkedHashMap<String, ExtraFieldHolder> extras) {
        final LayoutInflater inflater = LayoutInflater.from(mFragment.getContext());
        for(Map.Entry<String, ExtraFieldHolder> extra : extras.entrySet()) {
            if(!extra.getValue().preset) {
                final View root = inflater.inflate(R.layout.edit_info_extra, mInfoTable, false);
                final TextView label = (TextView)root.findViewById(R.id.label);
                final EditText value = (EditText)root.findViewById(R.id.value);
                label.setText(mFragment.getString(R.string.label_field, extra.getValue().name));
                initEditText(value, extra.getValue());
                mInfoTable.addView(root);

                mExtraViews.put(extra.getValue(), value);
            }
            mExtras.put(extra.getKey(), extra.getValue());
        }
    }

    /**
     * Get the list of extras.
     *
     * @return The list of extras
     */
    public LinkedHashMap<String, ExtraFieldHolder> getExtras() {
        return mExtras;
    }

    /**
     * Get the list of extra EditTexts.
     *
     * @return A map of ExtraFieldHolders to EditText.
     */
    public HashMap<ExtraFieldHolder, EditText> getExtraViews() {
        return mExtraViews;
    }

    /**
     * Set up the autocomplete for the maker field.
     */
    private void setupMakersAutoComplete() {
        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(mFragment.getContext(),
                R.layout.simple_dropdown_item_2line, null,
                new String[] {Tables.Makers.NAME, Tables.Makers.LOCATION},
                new int[] {android.R.id.text1, android.R.id.text2}, 0);

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                final Uri uri;
                if(TextUtils.isEmpty(constraint)) {
                    uri = Tables.Makers.CONTENT_URI;
                } else {
                    uri = Uri.withAppendedPath(Tables.Makers.CONTENT_FILTER_URI_BASE,
                            Uri.encode(constraint.toString()));
                }

                final Bundle args = new Bundle();
                args.putParcelable("uri", uri);

                mFragment.getLoaderManager()
                        .restartLoader(LOADER_MAKERS, args, EntryFormHelper.this);

                return adapter.getCursor();
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
     * Set up an EditText with an extra field.
     *
     * @param editText The EditText
     * @param extra    The extra field to associate with the View
     */
    protected static void initEditText(EditText editText, final ExtraFieldHolder extra) {
        if(extra == null) {
            return;
        }
        if(!extra.preset) {
            editText.setHint(extra.name);
        }
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
     * @param extra   The extra field to associate with the View
     */
    protected static void initSpinner(Spinner spinner, final ExtraFieldHolder extra) {
        if(extra == null) {
            return;
        }
        if(extra.value != null) {
            spinner.setSelection(Integer.valueOf(extra.value));
        }

        final AdapterView.OnItemSelectedListener listener = spinner.getOnItemSelectedListener();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                extra.value = position + "";
                if(listener != null) {
                    listener.onItemSelected(parent, view, position, id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if(listener != null) {
                    listener.onNothingSelected(parent);
                }
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_MAKERS:
                final String order = Tables.Makers.NAME + " ASC";
                final Uri uri = args.getParcelable("uri");
                return new CursorLoader(mFragment.getContext(), uri, null, null, null, order);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_MAKERS:
                ((CursorAdapter)mTxtMaker.getAdapter()).swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch(loader.getId()) {
            case LOADER_MAKERS:
                ((CursorAdapter)mTxtMaker.getAdapter()).swapCursor(null);
        }
    }
}
