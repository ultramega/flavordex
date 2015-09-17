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
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ultramegasoft.flavordex2.beer.EditBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.EditCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.whiskey.EditWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.wine.EditWineInfoFragment;

/**
 * The parent Fragment for the entry creation pages.
 *
 * @author Steve Guidetti
 */
public class AddEntryFragment extends Fragment {
    /**
     * Keys for the Fragment arguments
     */
    public static final String ARG_CAT_ID = "cat_id";
    public static final String ARG_CAT_NAME = "cat_name";

    /**
     * The category ID
     */
    private long mCatId;

    /**
     * The name of the category
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        mCatId = args.getLong(ARG_CAT_ID);
        mCatName = args.getString(ARG_CAT_NAME);

        final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if(actionBar != null) {
            final String name = FlavordexApp.getRealCatName(getContext(), mCatName);
            final String title = getString(R.string.title_add_cat_entry, name);
            actionBar.setTitle(title);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_add_entry, container, false);
        mPager = (ViewPager)root.findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(new PagerAdapter());

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
        EntryHolder entry = null;
        ContentValues[] entryFlavors = null;
        ContentValues[] entryPhotos = null;
        for(Fragment fragment : fm.getFragments()) {
            if(fragment instanceof EditInfoFragment) {
                isValid = ((EditInfoFragment)fragment).isValid();
                if(!isValid) {
                    break;
                }
                entry = ((EditInfoFragment)fragment).getData();
                entry.catName = mCatName;
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

        if(isValid) {
            DataSaverFragment.init(getFragmentManager(), entry, entryFlavors, entryPhotos);
        } else {
            mBtnSave.setEnabled(true);
            mPager.setCurrentItem(0);
        }
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
        public static final String ARG_ENTRY = "entry";
        public static final String ARG_ENTRY_FLAVORS = "entry_flavors";
        public static final String ARG_ENTRY_PHOTOS = "entry_photos";

        /**
         * The entry to insert
         */
        private EntryHolder mEntry;

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
         * @param entry        The entry to insert
         * @param entryFlavors Values for the entries_flavors table rows
         * @param entryPhotos  Values for the photos table rows
         */
        public static void init(FragmentManager fm, EntryHolder entry, ContentValues[] entryFlavors,
                                ContentValues[] entryPhotos) {
            final Bundle args = new Bundle();
            args.putParcelable(ARG_ENTRY, entry);
            args.putParcelableArray(ARG_ENTRY_FLAVORS, entryFlavors);
            args.putParcelableArray(ARG_ENTRY_PHOTOS, entryPhotos);

            final Fragment fragment = new DataSaverFragment();
            fragment.setArguments(args);

            fm.beginTransaction().add(fragment, TAG).commit();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

            final Bundle args = getArguments();
            mEntry = args.getParcelable(ARG_ENTRY);
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
                activity.publishResult(entryId, mEntry.catName);
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
                final Uri entryUri = mResolver.insert(Tables.Entries.CONTENT_URI, getInfo());
                insertExtras(entryUri);

                if(mEntryFlavors != null) {
                    mResolver.bulkInsert(Uri.withAppendedPath(entryUri, "flavor"), mEntryFlavors);
                } else {
                    insertDefaultFlavors(entryUri);
                }

                if(mEntryPhotos != null) {
                    mResolver.bulkInsert(Uri.withAppendedPath(entryUri, "photos"), mEntryPhotos);
                }

                checkLocation(mEntry.location);

                return Long.valueOf(entryUri.getLastPathSegment());
            }

            @Override
            protected void onPostExecute(Long entryId) {
                onComplete(entryId);
            }

            /**
             * Get the data for the entries table.
             *
             * @return ContentValues ready to be inserted into the entries table
             */
            private ContentValues getInfo() {
                final ContentValues values = new ContentValues();
                values.put(Tables.Entries.TITLE, mEntry.title);
                values.put(Tables.Entries.CAT, mEntry.catId);
                values.put(Tables.Entries.MAKER, mEntry.maker);
                values.put(Tables.Entries.ORIGIN, mEntry.origin);
                values.put(Tables.Entries.LOCATION, mEntry.location);
                values.put(Tables.Entries.DATE, mEntry.date);
                values.put(Tables.Entries.PRICE, mEntry.price);
                values.put(Tables.Entries.RATING, mEntry.rating);
                values.put(Tables.Entries.NOTES, mEntry.notes);
                return values;
            }

            /**
             * Insert the extra fields for the new entry.
             *
             * @param entryUri The Uri for the new entry
             */
            private void insertExtras(Uri entryUri) {
                final Uri uri = Uri.withAppendedPath(entryUri, "extras");
                final ContentValues values = new ContentValues();
                for(ExtraFieldHolder extra : mEntry.getExtras()) {
                    if(!extra.preset && TextUtils.isEmpty(extra.value)) {
                        continue;
                    }
                    values.put(Tables.EntriesExtras.EXTRA, extra.id);
                    values.put(Tables.EntriesExtras.VALUE, extra.value);
                    mResolver.insert(uri, values);
                }
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
                final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE,
                        mEntry.catId);
                final Cursor cursor = mResolver.query(Uri.withAppendedPath(uri, "flavor"), null,
                        null, null, Tables.Flavors._ID + " ASC");
                try {
                    final ContentValues flavor = new ContentValues();
                    flavor.put(Tables.EntriesFlavors.VALUE, 0);
                    while(cursor.moveToNext()) {
                        flavor.put(Tables.EntriesFlavors.FLAVOR,
                                cursor.getString(cursor.getColumnIndex(Tables.Flavors.NAME)));
                        mResolver.insert(Uri.withAppendedPath(entryUri, "flavor"), flavor);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }
}
