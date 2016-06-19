package com.ultramegasoft.flavordex2.fragment;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ultramegasoft.flavordex2.AddEntryActivity;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.beer.EditBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.EditCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.whiskey.EditWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
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
     * True while the save task is running
     */
    private boolean mIsSaving;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

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

        ((ViewPager.LayoutParams)root.findViewById(R.id.tabs).getLayoutParams()).isDecor = true;

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.entry_edit_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_save).setEnabled(!mIsSaving);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_save:
                saveEntry();
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
        if(mIsSaving) {
            return;
        }

        final FragmentManager fm = getChildFragmentManager();

        boolean isValid = false;
        final EntryHolder entry = new EntryHolder();
        for(Fragment fragment : fm.getFragments()) {
            if(fragment instanceof EditInfoFragment) {
                isValid = ((EditInfoFragment)fragment).isValid();
                if(!isValid) {
                    break;
                }
                ((EditInfoFragment)fragment).getData(entry);
                entry.catName = mCatName;
                continue;
            }
            if(fragment instanceof AddFlavorsFragment) {
                ((AddFlavorsFragment)fragment).getData(entry);
                continue;
            }
            if(fragment instanceof AddPhotosFragment) {
                ((AddPhotosFragment)fragment).getData(entry);
            }
        }

        if(isValid) {
            mIsSaving = true;
            ActivityCompat.invalidateOptionsMenu(getActivity());
            DataSaverFragment.init(getFragmentManager(), entry);
        } else {
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
        private static final String TAG = "DataSaverFragment";

        /**
         * Keys for the Fragment arguments
         */
        public static final String ARG_ENTRY = "entry";

        /**
         * The entry to insert
         */
        private EntryHolder mEntry;


        /**
         * The newly inserted entry ID in case the Fragment was detached when the insert task
         * completed
         */
        private long mEntryId;

        /**
         * Start a new instance of this Fragment.
         *
         * @param fm    The FragmentManager to use
         * @param entry The entry to insert
         */
        public static void init(FragmentManager fm, EntryHolder entry) {
            final Bundle args = new Bundle();
            args.putParcelable(ARG_ENTRY, entry);

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

            new DataSaver(getContext()).execute();
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
                activity.publishResult(entryId, mEntry.catName, mEntry.catId);
            }
        }

        /**
         * Task for saving a new entry into the database in the background.
         */
        private class DataSaver extends AsyncTask<Void, Void, Long> {
            /**
             * The Context
             */
            private final Context mContext;

            /**
             * @param context The Context
             */
            public DataSaver(Context context) {
                mContext = context.getApplicationContext();
            }

            @Override
            protected Long doInBackground(Void... params) {
                try {
                    if(mEntry.getFlavors().isEmpty()) {
                        getDefaultFlavors();
                    }
                    final Uri entryUri = EntryUtils.insertEntry(mContext, mEntry);
                    checkLocation(mEntry.location);
                    return Long.valueOf(entryUri.getLastPathSegment());
                } catch(SQLiteException e) {
                    Log.e(TAG, "Failed to insert entry: " + mEntry.title, e);
                }

                return 0L;
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

                    mContext.getContentResolver().insert(Tables.Locations.CONTENT_URI, values);
                }
            }

            /**
             * Get the default flavors with 0 values in case the user did not supply data.
             **/
            private void getDefaultFlavors() {
                final ContentResolver cr = mContext.getContentResolver();
                final Uri uri = ContentUris.withAppendedId(Tables.Cats.CONTENT_ID_URI_BASE,
                        mEntry.catId);
                final Cursor cursor = cr.query(Uri.withAppendedPath(uri, "flavor"), null, null,
                        null, Tables.Flavors.POS + " ASC");
                if(cursor != null) {
                    try {
                        String name;
                        while(cursor.moveToNext()) {
                            name = cursor.getString(cursor.getColumnIndex(Tables.Flavors.NAME));
                            mEntry.addFlavor(name, 0);
                        }
                    } finally {
                        cursor.close();
                    }
                }

            }
        }
    }
}
