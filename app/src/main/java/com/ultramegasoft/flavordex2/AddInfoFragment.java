package com.ultramegasoft.flavordex2;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
    private static final int LOADER_LOCATIONS = 1;

    /**
     * Keys for the saved state
     */
    private static final String STATE_LOCATION = "location";
    private static final String STATE_LOCATION_NAME = "location_name";

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

    /**
     * The current location
     */
    private Location mLocation;

    /**
     * The name of the current location
     */
    private String mLocationName;

    /**
     * The LocationManager service
     */
    private LocationManager mLocationManager;

    /**
     * The listener for location updates
     */
    private LocationListener mLocationListener;

    /**
     * Handler for the location detection timeout
     */
    private final Handler mHandler = new Handler();

    public AddInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTypeId = getArguments().getLong(AddEntryFragment.ARG_TYPE_ID);

        if(savedInstanceState != null) {
            mLocation = savedInstanceState.getParcelable(STATE_LOCATION);
            mLocationName = savedInstanceState.getString(STATE_LOCATION_NAME);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_EXTRAS, null, this);

        if(mLocation == null) {
            final SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            if(prefs.getBoolean(FlavordexApp.PREF_DETECT_LOCATION, false)) {
                setupLocationListener();
            }
        }
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_LOCATION, mLocation);
        outState.putString(STATE_LOCATION_NAME, mLocationName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        if(mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
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
     * Set up location detection.
     */
    private void setupLocationListener() {
        final LocationManager lm = (LocationManager)getActivity()
                .getSystemService(Context.LOCATION_SERVICE);
        mLocationManager = lm;

        if(!lm.getProviders(true).contains(LocationManager.NETWORK_PROVIDER)) {
            return;
        }

        final Runnable locationTimeout = new Runnable() {
            public void run() {
                lm.removeUpdates(mLocationListener);
                setLocation(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
            }
        };

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                mHandler.removeCallbacks(locationTimeout);
                lm.removeUpdates(this);
                setLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
        mHandler.postDelayed(locationTimeout, 5000);
    }

    /**
     * Set the current location.
     *
     * @param location A location received from the location listener
     */
    private void setLocation(Location location) {
        if(location == null) {
            return;
        }
        mLocation = location;
        getLoaderManager().initLoader(LOADER_LOCATIONS, null, this);
    }

    /**
     * Find the name of the nearest location from the database.
     *
     * @param cursor Cursor from the locations table
     */
    private void findNearestLocation(Cursor cursor) {
        if(mLocation == null) {
            return;
        }
        final double lat = mLocation.getLatitude();
        final double lon = mLocation.getLongitude();

        String closestName = null;
        float closestDistance = Float.MAX_VALUE;
        final float[] distance = new float[1];
        while(cursor.moveToNext()) {
            Location.distanceBetween(
                    lat, lon,
                    cursor.getDouble(cursor.getColumnIndex(Tables.Locations.LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(Tables.Locations.LONGITUDE)),
                    distance);

            if(distance[0] < closestDistance) {
                closestDistance = distance[0];
                closestName = cursor.getString(cursor.getColumnIndex(Tables.Locations.NAME));
            }
        }

        setLocationName(closestName);
    }

    /**
     * Set the name of the current location from the database.
     *
     * @param locationName The name of the current location
     */
    public void setLocationName(String locationName) {
        mLocationName = locationName;
        if(mTxtLocation != null && TextUtils.isEmpty(mTxtLocation.getText())) {
            mTxtLocation.setText(mLocationName);
        }
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

    /**
     * Read the detected location into a ContentValues object ready to be inserted into the
     * locations database table.
     *
     * @return ContentValues containing the data for the locations table
     */
    public ContentValues getLocation() {
        if(mLocation != null) {
            final String location = mTxtLocation.getText().toString();
            if(!TextUtils.isEmpty(location) && !location.equals(mLocationName)) {
                final ContentValues values = new ContentValues();
                values.put(Tables.Locations.NAME, location);
                values.put(Tables.Locations.LATITUDE, mLocation.getLatitude());
                values.put(Tables.Locations.LONGITUDE, mLocation.getLongitude());

                return values;
            }
        }
        return null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_EXTRAS:
                final Uri uri = ContentUris.withAppendedId(Tables.Types.CONTENT_ID_URI_BASE,
                        mTypeId);
                return new CursorLoader(getActivity(), Uri.withAppendedPath(uri, "/extras"), null,
                        null, null, Tables.Extras._ID + " ASC");
            case LOADER_LOCATIONS:
                return new CursorLoader(getActivity(), Tables.Locations.CONTENT_URI, null, null,
                        null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case LOADER_EXTRAS:
                loadExtras(data);
                break;
            case LOADER_LOCATIONS:
                findNearestLocation(data);
                break;
        }

        getLoaderManager().destroyLoader(loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
