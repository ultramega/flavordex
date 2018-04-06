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
package com.ultramegasoft.flavordex2.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @NonNull
    protected final Fragment mFragment;

    /**
     * Views from the layout
     */
    public EditText mTxtTitle;
    public AutoCompleteTextView mTxtMaker;
    public EditText mTxtOrigin;
    public EditText mTxtPrice;
    public EditText mTxtLocation;
    public EditText mTxtNotes;

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
    public EntryFormHelper(@NonNull Fragment fragment, @NonNull View layoutRoot) {
        mFragment = fragment;
        loadLayout(layoutRoot);
        setupMakersAutoComplete();
    }

    /**
     * Load the elements from the layout.
     *
     * @param root The root of the layout
     */
    protected void loadLayout(@NonNull View root) {
        mTxtTitle = root.findViewById(R.id.entry_title);
        mTxtMaker = root.findViewById(R.id.entry_maker);
        mTxtOrigin = root.findViewById(R.id.entry_origin);
        mTxtPrice = root.findViewById(R.id.entry_price);
        mTxtLocation = root.findViewById(R.id.entry_location);
        mTxtNotes = root.findViewById(R.id.entry_notes);
        mInfoTable = root.findViewById(R.id.entry_info);
    }

    /**
     * Set up the extra fields in the form.
     *
     * @param extras The list of extras
     */
    public void setExtras(@NonNull LinkedHashMap<String, ExtraFieldHolder> extras) {
        final LayoutInflater inflater = LayoutInflater.from(mFragment.getContext());
        for(Map.Entry<String, ExtraFieldHolder> extra : extras.entrySet()) {
            mExtras.put(extra.getKey(), extra.getValue());
            if(!extra.getValue().preset) {
                final View root = inflater.inflate(R.layout.edit_info_extra, mInfoTable, false);
                final TextView label = root.findViewById(R.id.label);
                final EditText value = root.findViewById(R.id.value);
                label.setText(mFragment.getString(R.string.label_field, extra.getValue().name));
                initEditText(value, extra.getValue());
                mInfoTable.addView(root);

                getExtraViews().put(extra.getValue(), value);
            }
        }
    }

    /**
     * Get the list of extras.
     *
     * @return The list of extras
     */
    @NonNull
    public LinkedHashMap<String, ExtraFieldHolder> getExtras() {
        return mExtras;
    }

    /**
     * Get the list of extra EditTexts.
     *
     * @return A map of ExtraFieldHolders to EditText.
     */
    @NonNull
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
    protected static void initEditText(@NonNull EditText editText,
                                       @Nullable final ExtraFieldHolder extra) {
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
    protected static void initSpinner(@NonNull Spinner spinner,
                                      @Nullable final ExtraFieldHolder extra) {
        if(extra == null) {
            return;
        }
        if(extra.value == null) {
            extra.value = "0";
        }
        spinner.setSelection(Integer.valueOf(extra.value));

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

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_MAKERS:
                final Context context = mFragment.getContext();
                if(context != null) {
                    final String order = Tables.Makers.NAME + " ASC";
                    final Uri uri = args.getParcelable("uri");
                    if(uri != null) {
                        return new CursorLoader(context, uri, null, null, null, order);
                    }
                }
        }
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_MAKERS:
                ((CursorAdapter)mTxtMaker.getAdapter()).swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {
        switch(loader.getId()) {
            case LOADER_MAKERS:
                ((CursorAdapter)mTxtMaker.getAdapter()).swapCursor(null);
        }
    }
}
