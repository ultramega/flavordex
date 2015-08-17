package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ultramegasoft.flavordex2.beer.BeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.CoffeeInfoFragment;
import com.ultramegasoft.flavordex2.whiskey.WhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.wine.WineInfoFragment;

/**
 * This fragment contains all the details of a journal entry. This is a container for multiple
 * fragment in a tabbed navigation layout.
 *
 * @author Steve Guidetti
 */
public class EntryDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment represents
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The fragment argument representing the type of item
     */
    public static final String ARG_ITEM_TYPE = "item_type";

    /**
     * Special item types that have their own interface
     */
    private static final String TYPE_BEER = "_beer";
    private static final String TYPE_WINE = "_wine";
    private static final String TYPE_WHISKEY = "_whiskey";
    private static final String TYPE_COFFEE = "_coffee";

    /**
     * The saved state of the selected tab
     */
    private static final String STATE_SELECTED_TAB = "selected_tab";

    /**
     * The database id for this entry
     */
    private long mEntryId;

    /**
     * Handle to the ActionBar
     */
    private ActionBar mActionBar;

    public EntryDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments().containsKey(ARG_ITEM_ID)) {
            mEntryId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        final AppCompatActivity activity = (AppCompatActivity)getActivity();

        mActionBar = activity.getSupportActionBar();

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.removeAllTabs();

        ActionBar.Tab tab;
        final Bundle args = new Bundle();
        args.putLong(ARG_ITEM_ID, mEntryId);

        tab = mActionBar.newTab()
                .setIcon(R.drawable.ic_menu_details)
                .setText(R.string.tab_info)
                .setTabListener(new TabListener<>(activity, "info_" + mEntryId,
                        getEntryInfoClass(), args));
        mActionBar.addTab(tab);

        tab = mActionBar.newTab()
                .setIcon(R.drawable.ic_menu_radar)
                .setText(R.string.tab_chart)
                .setTabListener(new TabListener<>(activity, "flavors_" + mEntryId,
                        EntryFlavorsFragment.class, args));
        mActionBar.addTab(tab);

        tab = mActionBar.newTab()
                .setIcon(R.drawable.ic_menu_camera)
                .setText(R.string.tab_photos)
                .setTabListener(new TabListener<>(activity, "photos_" + mEntryId,
                        EntryPhotosFragment.class, args));
        mActionBar.addTab(tab);

        if(savedInstanceState != null) {
            mActionBar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_TAB, 0));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, mActionBar.getSelectedNavigationIndex());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry_detail, container, false);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.entry_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_edit_entry:
                // TODO: 8/14/2015 Add editing
                return true;
            case R.id.menu_share:
                // TODO: 8/14/2015 Add sharing
                return true;
            case R.id.menu_delete_entry:
                // TODO: 8/14/2015 Add deleting
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the fragment class to use for displaying the main details of the entry.
     *
     * @return The Fragment class
     */
    private Class<? extends EntryInfoFragment> getEntryInfoClass() {
        final String type = getArguments().getString(ARG_ITEM_TYPE);

        if(TYPE_BEER.equals(type)) {
            return BeerInfoFragment.class;
        }
        if(TYPE_WINE.equals(type)) {
            return WineInfoFragment.class;
        }
        if(TYPE_WHISKEY.equals(type)) {
            return WhiskeyInfoFragment.class;
        }
        if(TYPE_COFFEE.equals(type)) {
            return CoffeeInfoFragment.class;
        }

        return EntryInfoFragment.class;
    }

    /**
     * Custom listener for action bar tabs.
     *
     * @param <T> A Fragment class
     */
    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final AppCompatActivity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;

        /**
         * @param activity The activity that the fragment belongs to
         * @param tag      The tag to identify the fragment
         * @param clz      The fragment class
         * @param args     The arguments to pass to the fragment
         */
        public TabListener(AppCompatActivity activity, String tag, Class<T> clz, Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
            if(mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                ft.add(R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if(mFragment != null) {
                ft.detach(mFragment);
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
