package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

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
     * The database id for this entry
     */
    private long mEntryId;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentTabHost tabHost =
                (FragmentTabHost)inflater.inflate(R.layout.fragment_entry_detail, container, false);
        tabHost.setup(getActivity(), getChildFragmentManager(), R.id.content);

        final Resources res = getResources();
        final Bundle args = new Bundle();
        args.putLong(ARG_ITEM_ID, mEntryId);

        Drawable icon;
        TabHost.TabSpec tab;

        icon = res.getDrawable(R.drawable.ic_description);
        tab = tabHost.newTabSpec("info_" + mEntryId).setIndicator(null, icon);
        tabHost.addTab(tab, getEntryInfoClass(), args);

        icon = res.getDrawable(R.drawable.ic_radar);
        tab = tabHost.newTabSpec("flavors_" + mEntryId).setIndicator(null, icon);
        tabHost.addTab(tab, EntryFlavorsFragment.class, args);

        icon = res.getDrawable(R.drawable.ic_photo);
        tab = tabHost.newTabSpec("photos_" + mEntryId).setIndicator(null, icon);
        tabHost.addTab(tab, EntryPhotosFragment.class, args);

        return tabHost;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Fragment fragment = getChildFragmentManager().findFragmentById(R.id.content);
        if(fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Get the fragment class to use for displaying the main details of the entry.
     *
     * @return The Fragment class
     */
    private Class<? extends EntryInfoFragment> getEntryInfoClass() {
        final String type = getArguments().getString(ARG_ITEM_TYPE);

        if(FlavordexApp.TYPE_BEER.equals(type)) {
            return BeerInfoFragment.class;
        }
        if(FlavordexApp.TYPE_WINE.equals(type)) {
            return WineInfoFragment.class;
        }
        if(FlavordexApp.TYPE_WHISKEY.equals(type)) {
            return WhiskeyInfoFragment.class;
        }
        if(FlavordexApp.TYPE_COFFEE.equals(type)) {
            return CoffeeInfoFragment.class;
        }

        return EntryInfoFragment.class;
    }
}
