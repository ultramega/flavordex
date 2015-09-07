package com.ultramegasoft.flavordex2;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ultramegasoft.flavordex2.beer.EditBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.EditCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.whiskey.EditWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.wine.EditWineInfoFragment;

/**
 * The parent Fragment for the entry creation pages.
 *
 * @author Steve Guidetti
 */
public class AddEntryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keys for the Fragment arguments
     */
    public static final String ARG_CAT_ID = "cat_id";

    /**
     * Keys for the saved state
     */
    private static final String STATE_CURRENT_PAGE = "current_page";

    /**
     * The category ID from the arguments
     */
    private long mCatId;

    /**
     * The name of the entry category
     */
    private String mCatName;

    /**
     * The ViewPager containing the Fragments
     */
    private ViewPager mPager;

    /**
     * Buttons from the main layout
     */
    private Button mBtnSave;

    /**
     * The currently displayed page in the ViewPager
     */
    private int mCurrentPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCatId = getArguments().getLong(ARG_CAT_ID, 0);
        setHasOptionsMenu(true);

        if(savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_add_entry, container, false);
        mPager = (ViewPager)root.findViewById(R.id.pager);

        mBtnSave = (Button)root.findViewById(R.id.button_save);
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEntry();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCurrentPage = mPager.getCurrentItem();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mCurrentPage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                final Fragment fragment = new CatListFragment();
                getFragmentManager().beginTransaction().replace(android.R.id.content, fragment)
                        .commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Fragment fragment = getChildFragmentManager().findFragmentById(R.id.pager);
        if(fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Get the Fragment class to use for adding the main details of the entry.
     *
     * @return The Fragment class
     */
    private Class<? extends EditInfoFragment> getEntryInfoClass() {
        if(FlavordexApp.CAT_BEER.equals(mCatName)) {
            return EditBeerInfoFragment.class;
        }
        if(FlavordexApp.CAT_WINE.equals(mCatName)) {
            return EditWineInfoFragment.class;
        }
        if(FlavordexApp.CAT_WHISKEY.equals(mCatName)) {
            return EditWhiskeyInfoFragment.class;
        }
        if(FlavordexApp.CAT_COFFEE.equals(mCatName)) {
            return EditCoffeeInfoFragment.class;
        }

        return EditInfoFragment.class;
    }

    /**
     * Insert the new entry into the database.
     */
    private void saveEntry() {
        mBtnSave.setEnabled(false);

        final FragmentManager fm = getChildFragmentManager();

        boolean isValid = false;
        ContentValues entryInfo = null;
        ContentValues[] entryExtras = null;
        ContentValues[] entryFlavors = null;
        ContentValues[] entryPhotos = null;
        for(Fragment fragment : fm.getFragments()) {
            if(fragment instanceof EditInfoFragment) {
                isValid = ((EditInfoFragment)fragment).isValid();
                if(!isValid) {
                    break;
                }
                entryInfo = ((EditInfoFragment)fragment).getData();
                entryExtras = ((EditInfoFragment)fragment).getExtras();
                continue;
            }
            if(fragment instanceof AddFlavorsFragment) {
                entryFlavors = ((AddFlavorsFragment)fragment).getData();
                continue;
            }
            if(fragment instanceof AddPhotosFragment) {
                entryPhotos = ((AddPhotosFragment)fragment).getData();
            }
        }

        if(isValid && entryInfo != null) {
            DataSaverFragment.init(getFragmentManager(), mCatId, mCatName, entryInfo, entryExtras,
                    entryFlavors, entryPhotos);
        } else {
            mBtnSave.setEnabled(true);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
        return new CursorLoader(getContext(), uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()) {
            mCatName = data.getString(data.getColumnIndex(Tables.Cats.NAME));

            mPager.setAdapter(new PagerAdapter());
            mPager.setCurrentItem(mCurrentPage);

            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null) {
                final String name = FlavordexApp.getRealCatName(getContext(), mCatName);
                final String title = getString(R.string.title_add_cat_entry, name);
                actionBar.setTitle(title);
                actionBar.setSubtitle(null);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Adapter for the ViewPager.
     */
    private class PagerAdapter extends FragmentPagerAdapter {
        /**
         * The list of Fragments
         */
        private final String[] mFragments = new String[] {
                EditInfoFragment.class.getName(),
                AddFlavorsFragment.class.getName(),
                AddPhotosFragment.class.getName()
        };

        /**
         * Page title string resource IDs
         */
        private final int[] mPageNames = {
                R.string.title_add_entry,
                R.string.title_add_flavor,
                R.string.title_add_photos
        };

        public PagerAdapter() {
            super(getChildFragmentManager());
            mFragments[0] = getEntryInfoClass().getName();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getText(mPageNames[position]);
        }

        @Override
        public Fragment getItem(int position) {
            final Bundle args = new Bundle();
            args.putLong(ARG_CAT_ID, mCatId);

            return Fragment.instantiate(getContext(), mFragments[position], args);
        }

        @Override
        public int getCount() {
            return mFragments.length;
        }
    }

    /**
     * Fragment for saving the entry in the background.
     */
    public static class DataSaverFragment extends Fragment {
        /**
         * The tag to identify this Fragment
         */
        private static final String TAG = "DataSaverFragment";

        /**
         * Keys for the Fragment arguments
         */
        public static final String ARG_CAT_ID = "cat_id";
        public static final String ARG_ENTRY_CAT = "entry_cat";
        public static final String ARG_ENTRY_INFO = "entry_info";
        public static final String ARG_ENTRY_EXTRAS = "entry_extras";
        public static final String ARG_ENTRY_FLAVORS = "entry_flavors";
        public static final String ARG_ENTRY_PHOTOS = "entry_photos";

        /**
         * The category ID of the entry
         */
        private long mCatId;

        /**
         * The name of the entry category
         */
        private String mEntryCat;

        /**
         * Values for the entries table row
         */
        private ContentValues mEntryInfo;

        /**
         * Values for the entries_extras table rows
         */
        private ContentValues[] mEntryExtras;

        /**
         * Values for the entries_flavors table rows
         */
        private ContentValues[] mEntryFlavors;

        /**
         * Values for the photos table rows
         */
        private ContentValues[] mEntryPhotos;

        /**
         * The newly inserted entry ID in case the Fragment was detached when the insert task
         * completed
         */
        private long mEntryId;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm           The FragmentManager to use
         * @param catId        The category ID of the entry
         * @param entryCat     The name of the entry category
         * @param entryInfo    Values for the entries table row
         * @param entryExtras  Values for the entries_extras table rows
         * @param entryFlavors Values for the entries_flavors table rows
         * @param entryPhotos  Values for the photos table rows
         */
        public static void init(FragmentManager fm, long catId, String entryCat,
                                ContentValues entryInfo, ContentValues[] entryExtras,
                                ContentValues[] entryFlavors, ContentValues[] entryPhotos) {
            final Bundle args = new Bundle();
            args.putLong(ARG_CAT_ID, catId);
            args.putString(ARG_ENTRY_CAT, entryCat);
            args.putParcelable(ARG_ENTRY_INFO, entryInfo);
            args.putParcelableArray(ARG_ENTRY_EXTRAS, entryExtras);
            args.putParcelableArray(ARG_ENTRY_FLAVORS, entryFlavors);
            args.putParcelableArray(ARG_ENTRY_PHOTOS, entryPhotos);

            final Fragment fragment = new DataSaverFragment();
            fragment.setArguments(args);

            fm.beginTransaction().add(fragment, TAG).commit();
        }

        public DataSaverFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

            final Bundle args = getArguments();
            mCatId = args.getLong(ARG_CAT_ID);
            mEntryCat = args.getString(ARG_ENTRY_CAT);
            mEntryInfo = args.getParcelable(ARG_ENTRY_INFO);
            mEntryExtras = (ContentValues[])args.getParcelableArray(ARG_ENTRY_EXTRAS);
            mEntryFlavors = (ContentValues[])args.getParcelableArray(ARG_ENTRY_FLAVORS);
            mEntryPhotos = (ContentValues[])args.getParcelableArray(ARG_ENTRY_PHOTOS);

            new DataSaver(getContext().getContentResolver()).execute();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if(mEntryId > 0) {
                onComplete(mEntryId);
            }
        }

        /**
         * Send the results to the Activity.
         *
         * @param entryId The ID for the new entry
         */
        private void onComplete(long entryId) {
            final AddEntryActivity activity = (AddEntryActivity)getActivity();
            if(activity == null) {
                mEntryId = entryId;
            } else {
                activity.publishResult(entryId, mEntryCat);
            }
        }

        /**
         * Task for saving a new entry into the database in the background.
         */
        private class DataSaver extends AsyncTask<Void, Void, Long> {
            /**
             * The ContentResolver to use for inserting data
             */
            private final ContentResolver mResolver;

            /**
             * @param contentResolver The ContentResolver to use for inserting data
             */
            public DataSaver(ContentResolver contentResolver) {
                mResolver = contentResolver;
            }

            @Override
            protected Long doInBackground(Void... params) {
                final Uri entryUri = mResolver.insert(Tables.Entries.CONTENT_URI, mEntryInfo);
                if(mEntryExtras != null) {
                    mResolver.bulkInsert(Uri.withAppendedPath(entryUri, "/extras"), mEntryExtras);
                }
                if(mEntryFlavors != null) {
                    mResolver.bulkInsert(Uri.withAppendedPath(entryUri, "/flavor"), mEntryFlavors);
                } else {
                    insertDefaultFlavors(entryUri);
                }
                if(mEntryPhotos != null) {
                    mResolver.bulkInsert(Uri.withAppendedPath(entryUri, "/photos"), mEntryPhotos);
                }

                checkLocation(mEntryInfo.getAsString(Tables.Entries.LOCATION));

                return Long.valueOf(entryUri.getLastPathSegment());
            }

            @Override
            protected void onPostExecute(Long entryId) {
                onComplete(entryId);
            }

            /**
             * Check for a new location and insert it into the database.
             *
             * @param newLocationName The name of the location supplied by the user
             */
            private void checkLocation(String newLocationName) {
                if(TextUtils.isEmpty(newLocationName)) {
                    return;
                }
                final FlavordexApp app = (FlavordexApp)getActivity().getApplication();
                final Location location = app.getLocation();
                final String locationName = app.getLocationName();
                if(location != null && !TextUtils.isEmpty(locationName)
                        && !newLocationName.equals(locationName)) {
                    final ContentValues values = new ContentValues();
                    values.put(Tables.Locations.NAME, newLocationName);
                    values.put(Tables.Locations.LATITUDE, location.getLatitude());
                    values.put(Tables.Locations.LONGITUDE, location.getLongitude());

                    mResolver.insert(Tables.Locations.CONTENT_URI, values);
                }
            }

            /**
             * Insert the default flavors with 0 values in case the user did not supply data.
             *
             * @param entryUri The Uri of the newly inserted entry
             */
            private void insertDefaultFlavors(Uri entryUri) {
                final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE, mCatId);
                final Cursor cursor = mResolver.query(Uri.withAppendedPath(uri, "flavor"), null,
                        null, null, Tables.Flavors._ID + " ASC");
                try {
                    final ContentValues flavor = new ContentValues();
                    flavor.put(Tables.EntriesFlavors.VALUE, 0);
                    while(cursor.moveToNext()) {
                        flavor.put(Tables.EntriesFlavors.FLAVOR,
                                cursor.getLong(cursor.getColumnIndex(Tables.Flavors._ID)));
                        mResolver.insert(Uri.withAppendedPath(entryUri, "flavor"), flavor);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }
}
