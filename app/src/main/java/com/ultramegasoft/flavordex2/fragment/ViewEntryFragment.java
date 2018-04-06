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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import com.ultramegasoft.flavordex2.EntryListActivity;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.beer.ViewBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.ViewCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.dialog.ConfirmationDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryDeleter;
import com.ultramegasoft.flavordex2.whiskey.ViewWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.wine.ViewWineInfoFragment;

/**
 * This Fragment contains all the details of a journal entry. This is a container for multiple
 * Fragment in a tabbed navigation layout.
 *
 * @author Steve Guidetti
 */
public class ViewEntryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keys for the Fragment arguments
     */
    public static final String ARG_ENTRY_ID = "entry_id";
    public static final String ARG_ENTRY_CAT = "entry_cat";
    public static final String ARG_ENTRY_CAT_ID = "entry_cat_id";

    /**
     * Request code for deleting an entry
     */
    private static final int REQUEST_DELETE_ENTRY = 500;

    /**
     * The database ID for this entry
     */
    private long mEntryId;

    /**
     * The title of this entry
     */
    @Nullable
    private String mEntryTitle;

    /**
     * The FragmentTabHost
     */
    private FragmentTabHost mTabHost;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        final Bundle args = getArguments();
        if(args != null) {
            mEntryId = args.getLong(ARG_ENTRY_ID);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        if(context == null) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        mTabHost = (FragmentTabHost)inflater.inflate(R.layout.tab_layout, container, false);
        mTabHost.setup(getContext(), getChildFragmentManager(), R.id.content);

        final Bundle tabArgs = new Bundle();
        tabArgs.putLong(ARG_ENTRY_ID, mEntryId);

        final Bundle args = getArguments();
        tabArgs.putLong(ARG_ENTRY_CAT_ID, args != null ? args.getLong(ARG_ENTRY_CAT_ID) : 0);

        Drawable icon;
        TabHost.TabSpec tab;

        icon = ActivityCompat.getDrawable(context, R.drawable.ic_description);
        tab = mTabHost.newTabSpec("info_" + mEntryId).setIndicator(null, icon);
        mTabHost.addTab(tab, getEntryInfoClass(), tabArgs);

        icon = ActivityCompat.getDrawable(context, R.drawable.ic_radar);
        tab = mTabHost.newTabSpec("flavors_" + mEntryId).setIndicator(null, icon);
        mTabHost.addTab(tab, ViewFlavorsFragment.class, tabArgs);

        icon = ActivityCompat.getDrawable(context, R.drawable.ic_photo);
        tab = mTabHost.newTabSpec("photos_" + mEntryId).setIndicator(null, icon);
        mTabHost.addTab(tab, ViewPhotosFragment.class, tabArgs);

        return mTabHost;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.view_entry_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_delete_entry:
                final FragmentManager fm = getFragmentManager();
                if(fm != null) {
                    ConfirmationDialog.showDialog(fm, this, REQUEST_DELETE_ENTRY,
                            getString(R.string.title_delete_entry),
                            getString(R.string.message_confirm_delete, mEntryTitle),
                            R.drawable.ic_delete);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_DELETE_ENTRY:
                    final Context context = getContext();
                    if(context != null) {
                        new EntryDeleter(context, mEntryId).execute();
                    }
                    return;
            }
        }
        final Fragment fragment = getChildFragmentManager().findFragmentById(R.id.content);
        if(fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Called when the back button is pressed.
     *
     * @return Whether the back button press was intercepted
     */
    public boolean onBackButtonPressed() {
        if(mTabHost.getCurrentTab() == 1) {
            final FragmentManager fm = getChildFragmentManager();
            final Fragment fragment = fm.findFragmentByTag("flavors_" + mEntryId);
            if(fragment instanceof ViewFlavorsFragment) {
                return ((ViewFlavorsFragment)fragment).onBackButtonPressed();
            }
        }
        return false;
    }

    /**
     * Called when the entry no longer exists.
     */
    private void onEntryDeleted() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final EntryListActivity activity = (EntryListActivity)getActivity();
                final FragmentManager fm = getFragmentManager();
                if(activity != null && fm != null) {
                    final Fragment fragment = fm.findFragmentById(R.id.entry_list);
                    if(fragment instanceof EntryListFragment) {
                        ((EntryListFragment)fragment).clearSelection();
                    } else if(fragment instanceof CatListFragment) {
                        final ActionBar actionBar = activity.getSupportActionBar();
                        if(actionBar != null) {
                            actionBar.setSubtitle(null);
                        }
                        activity.onItemSelected(-1, null, 0);
                    } else {
                        activity.finish();
                    }
                }
            }
        });
    }

    /**
     * Get the Fragment class to use for displaying the main details of the entry.
     *
     * @return The Fragment class
     */
    @NonNull
    private Class<? extends ViewInfoFragment> getEntryInfoClass() {
        final Bundle args = getArguments();
        final String cat = args != null ? args.getString(ARG_ENTRY_CAT) : null;

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
    private void setEntryTitle(@Nullable String title) {
        mEntryTitle = title;

        final AppCompatActivity activity = (AppCompatActivity)getActivity();
        if(activity != null) {
            final ActionBar actionBar = activity.getSupportActionBar();
            if(actionBar != null) {
                actionBar.setSubtitle(title);
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Context context = getContext();
        if(context == null) {
            return null;
        }

        final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
        final String[] projection = new String[] {Tables.Entries.TITLE};
        return new CursorLoader(context, uri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()) {
            setEntryTitle(data.getString(data.getColumnIndex(Tables.Entries.TITLE)));
        } else {
            onEntryDeleted();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}
