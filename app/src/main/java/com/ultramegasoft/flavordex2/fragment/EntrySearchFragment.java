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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.EntryListActivity;
import com.ultramegasoft.flavordex2.EntrySearchActivity;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.beer.BeerSearchFormFragment;
import com.ultramegasoft.flavordex2.coffee.CoffeeSearchFormFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryFormHelper;
import com.ultramegasoft.flavordex2.whiskey.WhiskeySearchFormFragment;
import com.ultramegasoft.flavordex2.widget.CatListAdapter;
import com.ultramegasoft.flavordex2.widget.DateInputWidget;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.wine.WineSearchFormFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Fragment for searching journal entries.
 *
 * @author Steve Guidetti
 */
public class EntrySearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Arguments for the Fragment
     */
    public static final String ARG_CAT_ID = "cat_id";
    public static final String ARG_FILTERS = "filters";

    /**
     * Keys for the result data Intent
     */
    public static final String EXTRA_FILTERS = "filters";
    public static final String EXTRA_WHERE = "where";
    public static final String EXTRA_WHERE_ARGS = "where_args";

    /**
     * Loader IDs
     */
    private static final int LOADER_CAT = 0;

    /**
     * Views from the layout
     */
    private Spinner mSpnCat;

    @Nullable
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_search, null);

        mSpnCat = root.findViewById(R.id.entry_cat);

        root.findViewById(R.id.button_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Fragment fragment
                        = getChildFragmentManager().findFragmentById(R.id.search_form);
                if(fragment instanceof SearchFormFragment) {
                    ((SearchFormFragment)fragment).resetForm();
                }
            }
        });

        root.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFilter();
            }
        });

        mSpnCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCategory();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        getLoaderManager().initLoader(LOADER_CAT, null, this);

        return root;
    }

    /**
     * Set the category to search.
     */
    private void setCategory() {
        final FragmentManager fm = getChildFragmentManager();
        final CatListAdapter.Category cat = (CatListAdapter.Category)mSpnCat.getSelectedItem();
        final Bundle args = getArguments();
        if(fm.findFragmentById(R.id.search_form) != null
                && args.getLong(ARG_CAT_ID, -1) == cat.id) {
            return;
        }
        args.putLong(ARG_CAT_ID, cat.id);

        final Fragment fragment;
        switch(cat.name) {
            case FlavordexApp.CAT_BEER:
                fragment = new BeerSearchFormFragment();
                break;
            case FlavordexApp.CAT_COFFEE:
                fragment = new CoffeeSearchFormFragment();
                break;
            case FlavordexApp.CAT_WHISKEY:
                fragment = new WhiskeySearchFormFragment();
                break;
            case FlavordexApp.CAT_WINE:
                fragment = new WineSearchFormFragment();
                break;
            default:
                fragment = new SearchFormFragment();
        }
        fragment.setArguments(args);
        fm.beginTransaction().replace(R.id.search_form, fragment).commit();
    }

    /**
     * Parse the form fields and send the results to the Activity.
     */
    private void performFilter() {
        final Fragment fragment = getChildFragmentManager().findFragmentById(R.id.search_form);
        if(fragment instanceof SearchFormFragment) {
            final Intent intent = ((SearchFormFragment)fragment).getData();
            if(getActivity() instanceof EntrySearchActivity) {
                ((EntrySearchActivity)getActivity()).publishResult(intent);
            } else if(getActivity() instanceof EntryListActivity) {
                final ContentValues filters = intent.getParcelableExtra(EXTRA_FILTERS);
                final String where = intent.getStringExtra(EXTRA_WHERE);
                final String[] whereArgs = intent.getStringArrayExtra(EXTRA_WHERE_ARGS);
                ((EntryListActivity)getActivity()).onSearchSubmitted(filters, where, whereArgs);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_CAT:
                return new CursorLoader(getContext(), Tables.Cats.CONTENT_URI, null, null, null,
                        null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_CAT:
                final CatListAdapter adapter = new CatListAdapter(getContext(), data,
                        android.R.layout.simple_spinner_item,
                        android.R.layout.simple_spinner_dropdown_item);
                adapter.setShowAllCats(true);
                mSpnCat.setAdapter(adapter);

                if(getArguments() != null) {
                    final long catId = getArguments().getLong(ARG_CAT_ID);
                    if(catId > 0) {
                        for(int i = 0; i < adapter.getCount(); i++) {
                            if(adapter.getItemId(i) == catId) {
                                mSpnCat.setSelection(i);
                                break;
                            }
                        }
                    }
                }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch(loader.getId()) {
            case LOADER_CAT:
                ((CatListAdapter)mSpnCat.getAdapter()).swapCursor(null);
        }
    }

    /**
     * Fragment containing the main search form.
     */
    @SuppressWarnings("unused")
    public static class SearchFormFragment extends Fragment
            implements LoaderManager.LoaderCallbacks<Cursor> {
        /**
         * Arguments for the Fragment
         */
        private static final String ARG_DATE_MIN = "date_min";
        private static final String ARG_DATE_MAX = "date_max";
        private static final String ARG_RATING_MIN = "rating_min";
        private static final String ARG_RATING_MAX = "rating_max";

        /**
         * The types of comparisons available to make between values
         */
        static final String COMP_LIKE = "LIKE";
        protected static final String COMP_EQ = "=";
        protected static final String COMP_NE = "!=";
        protected static final String COMP_GT = ">";
        protected static final String COMP_GTE = ">=";
        protected static final String COMP_LT = "<";
        protected static final String COMP_LTE = "<=";

        /**
         * Prefix for keys for extra fields in the filter list
         */
        private static final String EXTRA_PREFIX = "_extra_";

        /**
         * Loader IDs
         */
        private static final int LOADER_EXTRAS = 0;

        /**
         * Keys for the saved state
         */
        private static final String STATE_EXTRAS = "extras";

        /**
         * Views from the layout
         */
        private DateInputWidget mDateMin;
        private DateInputWidget mDateMax;
        private RatingBar mRatingMin;
        private RatingBar mRatingMax;
        private TextView mTxtRatingMin;
        private TextView mTxtRatingMax;

        /**
         * The EntryFormHelper
         */
        protected EntryFormHelper mFormHelper;

        /**
         * The category ID
         */
        private long mCatId;

        /**
         * The list of filter values
         */
        @NonNull
        private final ContentValues mFilters = new ContentValues();

        /**
         * The where clause
         */
        @NonNull
        private final StringBuilder mWhere = new StringBuilder();

        /**
         * The values for the parameters of the where clause
         */
        @NonNull
        private final ArrayList<String> mWhereArgs = new ArrayList<>();

        @NonNull
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            final View root = inflater.inflate(getLayoutId(), container, false);

            mFormHelper = createHelper(root);

            mDateMin = root.findViewById(R.id.entry_date_min);
            mDateMax = root.findViewById(R.id.entry_date_max);
            mRatingMin = root.findViewById(R.id.entry_rating_min);
            mRatingMax = root.findViewById(R.id.entry_rating_max);
            mTxtRatingMin = root.findViewById(R.id.rating_min_text);
            mTxtRatingMax = root.findViewById(R.id.rating_max_text);

            setupEventHandlers();

            return root;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            final Bundle args = getArguments();
            if(args != null) {
                mCatId = args.getLong(ARG_CAT_ID);
                final ContentValues filters = args.getParcelable(ARG_FILTERS);
                if(filters != null) {
                    mFormHelper.mTxtTitle.setText(filters.getAsString(Tables.Entries.TITLE));
                    mFormHelper.mTxtMaker.setText(filters.getAsString(Tables.Entries.MAKER));
                    mFormHelper.mTxtOrigin.setText(filters.getAsString(Tables.Entries.ORIGIN));
                    mFormHelper.mTxtPrice.setText(filters.getAsString(Tables.Entries.PRICE));
                    mFormHelper.mTxtLocation.setText(filters.getAsString(Tables.Entries.LOCATION));

                    if(filters.containsKey(ARG_DATE_MIN)) {
                        mDateMin.setDate(new Date(filters.getAsLong(ARG_DATE_MIN)));
                    }
                    if(filters.containsKey(ARG_DATE_MAX)) {
                        mDateMax.setDate(new Date(filters.getAsLong(ARG_DATE_MAX)));
                    }

                    if(filters.containsKey(ARG_RATING_MIN)) {
                        mRatingMin.setRating(filters.getAsFloat(ARG_RATING_MIN));
                    }
                    if(filters.containsKey(ARG_RATING_MAX)) {
                        mRatingMax.setRating(filters.getAsFloat(ARG_RATING_MAX));
                    }
                }
            }

            if(savedInstanceState != null) {
                //noinspection unchecked
                final LinkedHashMap<String, ExtraFieldHolder> extras =
                        (LinkedHashMap<String, ExtraFieldHolder>)savedInstanceState
                                .getSerializable(STATE_EXTRAS);
                if(extras != null) {
                    mFormHelper.setExtras(extras);
                }
            } else {
                getLoaderManager().initLoader(LOADER_EXTRAS, null, this);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putSerializable(STATE_EXTRAS, mFormHelper.getExtras());
        }

        /**
         * Get the ID for the layout to use.
         *
         * @return An ID from R.layout
         */
        protected int getLayoutId() {
            return R.layout.fragment_search_form;
        }

        /**
         * Create the EntryFormHelper.
         *
         * @param root The root of the layout
         * @return The EntryFormHelper
         */
        @NonNull
        protected EntryFormHelper createHelper(View root) {
            return new EntryFormHelper(this, root);
        }

        /**
         * Add event handlers to fields.
         */
        private void setupEventHandlers() {
            mDateMin.setListener(new DateInputWidget.OnDateChangeListener() {
                @Override
                public void onDateChanged(@NonNull Date date) {
                    final Date maxDate = mDateMax.getDate();
                    if(maxDate != null && date.after(maxDate)) {
                        mDateMax.setDate(date);
                    }
                }

                @Override
                public void onDateCleared() {
                }
            });

            mDateMax.setListener(new DateInputWidget.OnDateChangeListener() {
                @Override
                public void onDateChanged(@NonNull Date date) {
                    final Date minDate = mDateMin.getDate();
                    if(minDate != null && date.before(minDate)) {
                        mDateMin.setDate(date);
                    }
                }

                @Override
                public void onDateCleared() {
                }
            });

            mRatingMin.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar bar, float v, boolean b) {
                    mTxtRatingMin.setText(String.format(Locale.US, "%.1f", v));
                    if(v > mRatingMax.getRating()) {
                        mRatingMax.setRating(v);
                    }
                }
            });

            mRatingMax.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar bar, float v, boolean b) {
                    mTxtRatingMax.setText(String.format(Locale.US, "%.1f", v));
                    if(v < mRatingMin.getRating()) {
                        mRatingMin.setRating(v);
                    }
                }
            });
        }

        /**
         * Clear all form fields.
         */
        public void resetForm() {
            mFormHelper.mTxtTitle.setText(null);
            mFormHelper.mTxtMaker.setText(null);
            mFormHelper.mTxtOrigin.setText(null);
            mFormHelper.mTxtPrice.setText(null);
            mFormHelper.mTxtLocation.setText(null);
            mFormHelper.mTxtNotes.setText(null);

            for(EditText view : mFormHelper.getExtraViews().values()) {
                view.setText(null);
            }

            mDateMin.setDate(null);
            mDateMax.setDate(null);

            mRatingMin.setRating(0);
            mRatingMax.setRating(5);
        }

        /**
         * Parse the form fields and place the data into the Intent.
         *
         * @return An Intent holding the data
         */
        @NonNull
        public Intent getData() {
            mFilters.clear();
            mWhere.setLength(0);
            mWhereArgs.clear();

            mFilters.put(Tables.Entries.CAT_ID, mCatId);

            parseFields();

            if(mWhere.length() > 5) {
                mWhere.delete(mWhere.length() - 5, mWhere.length());
            }

            final Intent data = new Intent();
            data.putExtra(EXTRA_FILTERS, mFilters);
            data.putExtra(EXTRA_WHERE, mWhere.toString());
            data.putExtra(EXTRA_WHERE_ARGS, mWhereArgs.toArray(new String[mWhereArgs.size()]));
            return data;
        }

        /**
         * Parse the form fields.
         */
        void parseFields() {
            parseTextField(mFormHelper.mTxtTitle, Tables.Entries.TITLE);
            parseTextField(mFormHelper.mTxtMaker, Tables.Entries.MAKER);
            parseTextField(mFormHelper.mTxtOrigin, Tables.Entries.ORIGIN);
            parseTextField(mFormHelper.mTxtPrice, Tables.Entries.PRICE);
            parseTextField(mFormHelper.mTxtLocation, Tables.Entries.LOCATION);
            parseTextField(mFormHelper.mTxtNotes, Tables.Entries.NOTES);

            final Date minDate = mDateMin.getDate();
            final Date maxDate = mDateMax.getDate();
            if(minDate != null || maxDate != null) {
                if(minDate != null) {
                    final long minTime = minDate.getTime();
                    mFilters.put(ARG_DATE_MIN, minTime);
                    mWhere.append(Tables.Entries.DATE).append(" >= ").append(minTime)
                            .append(" AND ");
                }
                if(maxDate != null) {
                    final long maxTime = maxDate.getTime();
                    mFilters.put(ARG_DATE_MAX, maxTime);
                    mWhere.append(Tables.Entries.DATE).append(" < ")
                            .append(maxTime + (24 * 60 * 60 * 1000)).append(" AND ");
                }
            }

            if(mRatingMin.getRating() > 0) {
                mFilters.put(ARG_RATING_MIN, mRatingMin.getRating());
                mWhere.append(Tables.Entries.RATING).append(" >= ? AND ");
                mWhereArgs.add(mRatingMin.getRating() + "");
            }
            if(mRatingMax.getRating() < 5) {
                mFilters.put(ARG_RATING_MAX, mRatingMax.getRating());
                mWhere.append(Tables.Entries.RATING).append(" <= ? AND ");
                mWhereArgs.add(mRatingMax.getRating() + "");
            }

            parseExtras();
        }

        /**
         * Parse the extra fields.
         */
        private void parseExtras() {
            for(ExtraFieldHolder extra : mFormHelper.getExtras().values()) {
                if(!extra.preset || !parsePresetField(extra)) {
                    parseExtraField(extra, COMP_LIKE);
                }
            }
        }

        /**
         * Parse a preset extra field.
         *
         * @param extra The extra field
         * @return Whether the field was parsed
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        protected boolean parsePresetField(@NonNull ExtraFieldHolder extra) {
            return false;
        }

        /**
         * Parse a text field into a like statement.
         *
         * @param field     The text field containing the value
         * @param fieldName The name of the database column
         */
        private void parseTextField(@NonNull EditText field, @NonNull String fieldName) {
            if(!TextUtils.isEmpty(field.getText())) {
                mFilters.put(fieldName, field.getText().toString());
                final String[] words = field.getText().toString().split(" ");
                mWhere.append("(");
                for(String word : words) {
                    mWhere.append(fieldName).append(" LIKE ? AND ");
                    mWhereArgs.add("%" + word + "%");
                }
                mWhere.delete(mWhere.length() - 5, mWhere.length());
                mWhere.append(") AND ");
            }
        }

        /**
         * Parse an extra field.
         *
         * @param extra      The extra field
         * @param comparison The type of comparison to perform
         */
        protected void parseExtraField(@NonNull ExtraFieldHolder extra,
                                       @NonNull String comparison) {
            if(!TextUtils.isEmpty(extra.value)) {
                mFilters.put(EXTRA_PREFIX + extra.id, extra.value);
                final String[] words = extra.value.split(" ");
                mWhere.append("(SELECT 1 FROM ").append(Tables.EntriesExtras.TABLE_NAME)
                        .append(" WHERE extra = ? AND ");
                mWhereArgs.add(extra.id + "");
                for(String word : words) {
                    mWhere.append("value ").append(comparison).append(" ? AND ");
                    if(COMP_LIKE.equals(comparison)) {
                        mWhereArgs.add("%" + word + "%");
                    } else {
                        mWhereArgs.add(word);
                    }
                }
                mWhere.delete(mWhere.length() - 4, mWhere.length());
                mWhere.append("LIMIT 1) AND ");
            }
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch(id) {
                case LOADER_EXTRAS:
                    final Uri uri = Tables.Cats.getExtrasUri(mCatId);
                    final String[] projection = new String[] {
                            Tables.Extras._ID,
                            Tables.Extras.NAME,
                            Tables.Extras.PRESET
                    };
                    final String sort = Tables.Extras.POS;
                    return new CursorLoader(getContext(), uri, projection, null, null, sort);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            switch(loader.getId()) {
                case LOADER_EXTRAS:
                    ContentValues filters = null;
                    if(getArguments() != null) {
                        filters = getArguments().getParcelable(ARG_FILTERS);
                        if(filters != null) {
                            mFormHelper.mTxtTitle
                                    .setText(filters.getAsString(Tables.Entries.TITLE));
                            mFormHelper.mTxtMaker
                                    .setText(filters.getAsString(Tables.Entries.MAKER));
                            mFormHelper.mTxtOrigin
                                    .setText(filters.getAsString(Tables.Entries.ORIGIN));
                            mFormHelper.mTxtPrice
                                    .setText(filters.getAsString(Tables.Entries.PRICE));
                            mFormHelper.mTxtLocation
                                    .setText(filters.getAsString(Tables.Entries.LOCATION));
                            mFormHelper.mTxtNotes
                                    .setText(filters.getAsString(Tables.Entries.NOTES));
                        }
                    }

                    if(data != null) {
                        data.moveToPosition(-1);
                        final LinkedHashMap<String, ExtraFieldHolder> extras =
                                new LinkedHashMap<>();
                        while(data.moveToNext()) {
                            final long id = data.getLong(data.getColumnIndex(Tables.Extras._ID));
                            final String name =
                                    data.getString(data.getColumnIndex(Tables.Extras.NAME));
                            final boolean preset =
                                    data.getLong(data.getColumnIndex(Tables.Extras.PRESET)) == 1;
                            final ExtraFieldHolder extra = new ExtraFieldHolder(id, name, preset);
                            if(filters != null) {
                                extra.value = filters.getAsString(EXTRA_PREFIX + id);
                            }
                            extras.put(name, extra);
                        }

                        mFormHelper.setExtras(extras);
                    }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }
}
