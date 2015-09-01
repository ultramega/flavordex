package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
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
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ultramegasoft.flavordex2.beer.AddBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.AddCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.whiskey.AddWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.wine.AddWineInfoFragment;

/**
 * The parent fragment for the entry creation pages.
 *
 * @author Steve Guidetti
 */
public class AddEntryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keys for the fragment arguments
     */
    public static final String ARG_TYPE_ID = "type_id";

    /**
     * Keys for the saved state
     */
    private static final String STATE_CURRENT_PAGE = "current_page";

    /**
     * The type id from the arguments
     */
    private long mTypeId;

    /**
     * The name of the entry type
     */
    private String mTypeName;

    /**
     * The ViewPager containing the fragments
     */
    private ViewPager mPager;

    /**
     * Buttons from the main layout
     */
    private Button mBtnSave;

    /**
     * The currently displayed page in the pager
     */
    private int mCurrentPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTypeId = getArguments().getLong(ARG_TYPE_ID, 0);
        setHasOptionsMenu(true);

        if(savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                final Fragment fragment = new TypeListFragment();
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
     * Get the fragment class to use for adding the main details of the entry.
     *
     * @return The Fragment class
     */
    private Class<? extends AddInfoFragment> getEntryInfoClass() {
        if(FlavordexApp.TYPE_BEER.equals(mTypeName)) {
            return AddBeerInfoFragment.class;
        }
        if(FlavordexApp.TYPE_WINE.equals(mTypeName)) {
            return AddWineInfoFragment.class;
        }
        if(FlavordexApp.TYPE_WHISKEY.equals(mTypeName)) {
            return AddWhiskeyInfoFragment.class;
        }
        if(FlavordexApp.TYPE_COFFEE.equals(mTypeName)) {
            return AddCoffeeInfoFragment.class;
        }

        return AddInfoFragment.class;
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
        ContentValues entryLocation = null;
        ContentValues[] entryFlavors = null;
        ContentValues[] entryPhotos = null;
        for(Fragment fragment : fm.getFragments()) {
            if(fragment instanceof AddInfoFragment) {
                isValid = ((AddInfoFragment)fragment).isValid();
                if(!isValid) {
                    break;
                }
                entryInfo = ((AddInfoFragment)fragment).getData();
                entryExtras = ((AddInfoFragment)fragment).getExtras();
                entryLocation = ((AddInfoFragment)fragment).getLocation();
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
            DataSaverFragment.init(getFragmentManager(), mTypeName, entryInfo, entryExtras,
                    entryLocation, entryFlavors, entryPhotos);
        } else {
            mBtnSave.setEnabled(true);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = ContentUris.withAppendedId(Tables.Types.CONTENT_ID_URI_BASE, mTypeId);
        return new CursorLoader(getActivity(), uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()) {
            mTypeName = data.getString(data.getColumnIndex(Tables.Types.NAME));

            mPager.setAdapter(new PagerAdapter());
            mPager.setCurrentItem(mCurrentPage);

            final String name = FlavordexApp.getRealTypeName(getActivity(), mTypeName);
            final String title = getString(R.string.title_add_type_entry, name);
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Adapter for the ViewPager
     */
    private class PagerAdapter extends FragmentPagerAdapter {
        /**
         * The list of fragments
         */
        private String[] mFragments = new String[] {
                AddInfoFragment.class.getName(),
                AddFlavorsFragment.class.getName(),
                AddPhotosFragment.class.getName()
        };

        /**
         * Page title string resource ids
         */
        private int[] mPageNames = {
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
            args.putLong(ARG_TYPE_ID, mTypeId);

            return Fragment.instantiate(getActivity(), mFragments[position], args);
        }

        @Override
        public int getCount() {
            return mFragments.length;
        }
    }

    public static class DataSaverFragment extends Fragment {
        /**
         * The tag to identify this fragment
         */
        private static final String TAG = "DataSaverFragment";

        /**
         * Keys for the fragment arguments
         */
        public static final String ARG_ENTRY_TYPE = "entry_type";
        public static final String ARG_ENTRY_INFO = "entry_info";
        public static final String ARG_ENTRY_EXTRAS = "entry_extras";
        public static final String ARG_ENTRY_LOCATION = "entry_location";
        public static final String ARG_ENTRY_FLAVORS = "entry_flavors";
        public static final String ARG_ENTRY_PHOTOS = "entry_photos";

        /**
         * The name of the type of entry
         */
        private String mEntryType;

        /**
         * Values for the entries table row
         */
        private ContentValues mEntryInfo;

        /**
         * Values for the entries_extras table rows
         */
        private ContentValues[] mEntryExtras;

        /**
         * Values for the locations table row
         */
        private ContentValues mEntryLocation;

        /**
         * Values for the entries_flavors table rows
         */
        private ContentValues[] mEntryFlavors;

        /**
         * Values for the photos table rows
         */
        private ContentValues[] mEntryPhotos;

        /**
         * The newly inserted entry id in case the fragment was detached when the insert task
         * completed
         */
        private long mEntryId;

        /**
         * Start a new instance of this fragment.
         *
         * @param fm            The FragmentManager to use
         * @param entryType     The name of the type of entry
         * @param entryInfo     Values for the entries table row
         * @param entryExtras   Values for the entries_extras table rows
         * @param entryLocation Values for the locations table
         * @param entryFlavors  Values for the entries_flavors table rows
         * @param entryPhotos   Values for the photos table rows
         */
        public static void init(FragmentManager fm, String entryType, ContentValues entryInfo,
                                ContentValues[] entryExtras, ContentValues entryLocation,
                                ContentValues[] entryFlavors, ContentValues[] entryPhotos) {
            final Bundle args = new Bundle();
            args.putString(ARG_ENTRY_TYPE, entryType);
            args.putParcelable(ARG_ENTRY_INFO, entryInfo);
            args.putParcelableArray(ARG_ENTRY_EXTRAS, entryExtras);
            args.putParcelable(ARG_ENTRY_LOCATION, entryLocation);
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
            mEntryType = args.getString(ARG_ENTRY_TYPE);
            mEntryInfo = args.getParcelable(ARG_ENTRY_INFO);
            mEntryExtras = (ContentValues[])args.getParcelableArray(ARG_ENTRY_EXTRAS);
            mEntryLocation = args.getParcelable(ARG_ENTRY_LOCATION);
            mEntryFlavors = (ContentValues[])args.getParcelableArray(ARG_ENTRY_FLAVORS);
            mEntryPhotos = (ContentValues[])args.getParcelableArray(ARG_ENTRY_PHOTOS);

            new DataSaver(getActivity().getContentResolver()).execute();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if(mEntryId > 0) {
                onComplete(mEntryId);
            }
        }

        /**
         * Send the results to the activity.
         *
         * @param entryId The id for the new entry
         */
        private void onComplete(long entryId) {
            final AddEntryActivity activity = (AddEntryActivity)getActivity();
            if(activity == null) {
                mEntryId = entryId;
            } else {
                activity.publishResult(entryId, mEntryType);
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
                if(mEntryLocation != null) {
                    mResolver.insert(Tables.Locations.CONTENT_URI, mEntryLocation);
                }
                if(mEntryFlavors != null) {
                    mResolver.bulkInsert(Uri.withAppendedPath(entryUri, "/flavor"), mEntryFlavors);
                }
                if(mEntryPhotos != null) {
                    mResolver.bulkInsert(Uri.withAppendedPath(entryUri, "/photos"), mEntryPhotos);
                }

                return Long.valueOf(entryUri.getLastPathSegment());
            }

            @Override
            protected void onPostExecute(Long entryId) {
                onComplete(entryId);
            }
        }
    }
}
