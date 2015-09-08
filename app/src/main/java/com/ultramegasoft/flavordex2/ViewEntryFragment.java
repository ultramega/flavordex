package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import com.ultramegasoft.flavordex2.beer.ViewBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.ViewCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.whiskey.ViewWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.wine.ViewWineInfoFragment;

/**
 * This Fragment contains all the details of a journal entry. This is a container for multiple
 * Fragment in a tabbed navigation layout.
 *
 * @author Steve Guidetti
 */
public class ViewEntryFragment extends Fragment {
    /**
     * Keys for the Fragment arguments
     */
    public static final String ARG_ENTRY_ID = "entry_id";
    public static final String ARG_ENTRY_CAT = "entry_cat";

    /**
     * Keys for the saved state
     */
    private static final String STATE_ENTRY_TITLE = "entry_title";

    /**
     * The database ID for this entry
     */
    private long mEntryId;

    /**
     * The title of this entry
     */
    private String mEntryTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntryId = getArguments().getLong(ARG_ENTRY_ID);
        if(savedInstanceState != null) {
            setEntryTitle(savedInstanceState.getString(STATE_ENTRY_TITLE));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentTabHost tabHost =
                (FragmentTabHost)inflater.inflate(R.layout.fragment_view_entry, container, false);
        tabHost.setup(getContext(), getChildFragmentManager(), R.id.content);

        final Resources res = getResources();
        final Bundle args = new Bundle();
        args.putLong(ARG_ENTRY_ID, mEntryId);

        Drawable icon;
        TabHost.TabSpec tab;

        icon = res.getDrawable(R.drawable.ic_description);
        tab = tabHost.newTabSpec("info_" + mEntryId).setIndicator(null, icon);
        tabHost.addTab(tab, getEntryInfoClass(), args);

        icon = res.getDrawable(R.drawable.ic_radar);
        tab = tabHost.newTabSpec("flavors_" + mEntryId).setIndicator(null, icon);
        tabHost.addTab(tab, ViewFlavorsFragment.class, args);

        icon = res.getDrawable(R.drawable.ic_photo);
        tab = tabHost.newTabSpec("photos_" + mEntryId).setIndicator(null, icon);
        tabHost.addTab(tab, ViewPhotosFragment.class, args);

        return tabHost;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Fragment fragment = getChildFragmentManager().findFragmentById(R.id.content);
        if(fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ENTRY_TITLE, mEntryTitle);
    }

    /**
     * Get the Fragment class to use for displaying the main details of the entry.
     *
     * @return The Fragment class
     */
    private Class<? extends ViewInfoFragment> getEntryInfoClass() {
        final String cat = getArguments().getString(ARG_ENTRY_CAT);

        if(FlavordexApp.CAT_BEER.equals(cat)) {
            return ViewBeerInfoFragment.class;
        }
        if(FlavordexApp.CAT_WINE.equals(cat)) {
            return ViewWineInfoFragment.class;
        }
        if(FlavordexApp.CAT_WHISKEY.equals(cat)) {
            return ViewWhiskeyInfoFragment.class;
        }
        if(FlavordexApp.CAT_COFFEE.equals(cat)) {
            return ViewCoffeeInfoFragment.class;
        }

        return ViewInfoFragment.class;
    }

    /**
     * Set the title of this entry to be displayed as the ActionBar subtitle.
     *
     * @param title The title of the entry
     */
    public void setEntryTitle(String title) {
        mEntryTitle = title;
        final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setSubtitle(title);
        }
    }
}
