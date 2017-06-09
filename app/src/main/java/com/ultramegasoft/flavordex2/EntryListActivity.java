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
package com.ultramegasoft.flavordex2;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.ultramegasoft.flavordex2.dialog.AboutDialog;
import com.ultramegasoft.flavordex2.dialog.AppChooserDialog;
import com.ultramegasoft.flavordex2.dialog.FileImportDialog;
import com.ultramegasoft.flavordex2.dialog.FileSelectorDialog;
import com.ultramegasoft.flavordex2.fragment.CatListFragment;
import com.ultramegasoft.flavordex2.fragment.EntryListFragment;
import com.ultramegasoft.flavordex2.fragment.EntrySearchFragment;
import com.ultramegasoft.flavordex2.fragment.ViewEntryFragment;
import com.ultramegasoft.flavordex2.fragment.WelcomeFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.AppImportUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

import io.fabric.sdk.android.Fabric;

/**
 * The main application Activity. This shows a list of the categories or all the journal entries in
 * a category. On narrow screens, selecting an entry launches a new Activity to show details. On
 * wide screens, selecting an entry shows details in a Fragment in this Activity.
 *
 * @author Steve Guidetti
 */
public class EntryListActivity extends AppCompatActivity
        implements FileSelectorDialog.OnFileSelectedCallbacks {
    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_SEARCH = 300;

    /**
     * Keys for the saved state
     */
    private static final String STATE_SELECTED_CAT = "selected_cat";
    private static final String STATE_SELECTED_ENTRY = "selected_entry";
    private static final String STATE_FILTERS = "filters";

    /**
     * Whether the Activity is in two-pane mode
     */
    private boolean mTwoPane;

    /**
     * Fragment to show in two-pane mode when no item is selected
     */
    @Nullable
    private Fragment mWelcomeFragment;

    /**
     * The category list Fragment
     */
    private final Fragment mCatListFragment = new CatListFragment();

    /**
     * The currently selected category
     */
    private long mSelectedCat = -1;

    /**
     * The currently selected journal entry
     */
    private long mSelectedItem = -1;

    /**
     * Map of filters to populate the filter fragment
     */
    @Nullable
    private ContentValues mFilters;

    /**
     * The result returned by the search Activity
     */
    @Nullable
    private Intent mSearchResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_list);

        if(findViewById(R.id.entry_detail_container) != null) {
            mTwoPane = true;
            mWelcomeFragment = new WelcomeFragment();
        }

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.entry_list, mCatListFragment)
                    .commit();
            if(mTwoPane) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.entry_detail_container, mWelcomeFragment).commit();
            }

            loadPreferences(PreferenceManager.getDefaultSharedPreferences(this));

            PermissionUtils.checkExternalStoragePerm(this, R.string.message_request_storage);
        } else {
            mSelectedCat = savedInstanceState.getLong(STATE_SELECTED_CAT, mSelectedCat);
            mSelectedItem = savedInstanceState.getLong(STATE_SELECTED_ENTRY, mSelectedItem);
            mFilters = savedInstanceState.getParcelable(STATE_FILTERS);
        }

        final TwitterAuthConfig twitterConfig = new TwitterAuthConfig(
                getString(R.string.twitter_key), getString(R.string.twitter_secret));
        Fabric.with(this, new TwitterCore(twitterConfig));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_CAT, mSelectedCat);
        outState.putLong(STATE_SELECTED_ENTRY, mSelectedItem);
        outState.putParcelable(STATE_FILTERS, mFilters);
    }

    /**
     * Load the Shared Preferences for the Activity.
     *
     * @param prefs The default SharedPreferences
     */
    private void loadPreferences(@NonNull SharedPreferences prefs) {
        if(prefs.getBoolean(FlavordexApp.PREF_FIRST_RUN, true)) {
            if(AppImportUtils.isAnyAppInstalled(this)) {
                AppChooserDialog.showDialog(getSupportFragmentManager(), true);
            }
            prefs.edit().putBoolean(FlavordexApp.PREF_FIRST_RUN, false).apply();
        }

        final int oldVersion = prefs.getInt(FlavordexApp.PREF_VERSION, 0);
        final int newVersion = BuildConfig.VERSION_CODE;
        if(newVersion > oldVersion) {
            if(oldVersion < 14) {
                if(prefs.getBoolean(FlavordexApp.PREF_SYNC_DATA, false)) {
                    startActivity(new Intent(this, LoginActivity.class));
                }
            }
            prefs.edit().putInt(FlavordexApp.PREF_VERSION, newVersion).apply();
        }

        final long catId = prefs.getLong(FlavordexApp.PREF_LIST_CAT_ID, -1);
        if(catId > -1) {
            onCatSelected(catId, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean showFileXport = Environment.getExternalStorageDirectory().canWrite()
                || PermissionUtils.shouldAskExternalStoragePerm(this);
        final boolean showAppImport = AppImportUtils.isAnyAppInstalled(this);
        menu.findItem(R.id.menu_xport).setVisible(showFileXport || showAppImport);
        menu.findItem(R.id.menu_import).setVisible(showFileXport);
        menu.findItem(R.id.menu_export).setVisible(showFileXport);
        menu.findItem(R.id.menu_import_app).setVisible(showAppImport);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_search:
                onOpenSearch();
                return true;
            case R.id.menu_import:
                if(!PermissionUtils.checkExternalStoragePerm(this,
                        R.string.message_request_storage_xport)) {
                    return true;
                }
                final String rootPath = Environment.getExternalStorageDirectory().getPath();
                FileSelectorDialog.showDialog(getSupportFragmentManager(), null, 0, rootPath, false,
                        ".csv");
                return true;
            case R.id.menu_import_app:
                AppChooserDialog.showDialog(getSupportFragmentManager(), false);
                return true;
            case R.id.menu_export:
                if(PermissionUtils.checkExternalStoragePerm(this,
                        R.string.message_request_storage_xport)) {
                    enableExportMode();
                }
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_about:
                AboutDialog.showDialog(getSupportFragmentManager());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_SEARCH && resultCode == RESULT_OK) {
            mSearchResult = data;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mSearchResult != null) {
            final ContentValues filters
                    = mSearchResult.getParcelableExtra(EntrySearchFragment.EXTRA_FILTERS);
            final String where = mSearchResult.getStringExtra(EntrySearchFragment.EXTRA_WHERE);
            final String[] whereArgs
                    = mSearchResult.getStringArrayExtra(EntrySearchFragment.EXTRA_WHERE_ARGS);
            onSearchSubmitted(filters, where, whereArgs);
            mSearchResult = null;
        }
    }

    /**
     * Called by the CatListFragment when an item is selected.
     *
     * @param id         The category ID
     * @param exportMode Whether to start the entry list Fragment in export mode
     */
    public void onCatSelected(long id, boolean exportMode) {
        mSelectedCat = id;
        final Fragment fragment;
        if(id < 0) {
            fragment = mCatListFragment;
            mFilters = null;
        } else {
            fragment = EntryListFragment.getInstance(id, mTwoPane, mSelectedItem, exportMode, null,
                    null);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.entry_list, fragment).commit();
    }

    /**
     * Called by the EntryListFragment when an item is selected.
     *
     * @param id      The entry ID
     * @param catName The name of the entry category
     * @param catId   The ID of the entry category
     */
    public void onItemSelected(long id, @Nullable String catName, long catId) {
        if(mTwoPane) {
            mSelectedItem = id;
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            final Fragment fragment;
            if(id == -1) {
                fragment = mWelcomeFragment;
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            } else {
                final Bundle args = new Bundle();
                args.putLong(ViewEntryFragment.ARG_ENTRY_ID, id);
                args.putString(ViewEntryFragment.ARG_ENTRY_CAT, catName);
                args.putLong(ViewEntryFragment.ARG_ENTRY_CAT_ID, catId);

                fragment = new ViewEntryFragment();
                fragment.setArguments(args);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            }

            ft.replace(R.id.entry_detail_container, fragment).commit();
        } else {
            final Intent intent = new Intent(this, ViewEntryActivity.class);
            intent.putExtra(ViewEntryFragment.ARG_ENTRY_ID, id);
            intent.putExtra(ViewEntryFragment.ARG_ENTRY_CAT, catName);
            intent.putExtra(ViewEntryFragment.ARG_ENTRY_CAT_ID, catId);
            startActivity(intent);
        }
    }

    /**
     * Open the search page.
     */
    private void onOpenSearch() {
        if(mTwoPane) {
            final FragmentManager fm = getSupportFragmentManager();
            if(fm.findFragmentById(R.id.entry_detail_container) instanceof EntrySearchFragment) {
                return;
            }
            final Bundle args = new Bundle();
            args.putLong(EntrySearchFragment.ARG_CAT_ID, mSelectedCat);
            args.putParcelable(EntrySearchFragment.ARG_FILTERS, mFilters);
            final Fragment fragment = new EntrySearchFragment();
            fragment.setArguments(args);
            fm.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.entry_detail_container, fragment).commit();
        } else {
            final Intent intent = new Intent(this, EntrySearchActivity.class);
            intent.putExtra(EntrySearchActivity.EXTRA_FILTERS, mFilters);
            startActivityForResult(intent, REQUEST_SEARCH);
        }
    }

    /**
     * Open the entry list with search results.
     *
     * @param filters   The filter values
     * @param where     The where clause
     * @param whereArgs The values for the parameters of the where clause
     */
    public void onSearchSubmitted(@NonNull ContentValues filters, @NonNull String where,
                                  @NonNull String[] whereArgs) {
        mFilters = filters;
        final long catId = filters.getAsLong(Tables.Entries.CAT_ID);
        final Fragment fragment = EntryListFragment.getInstance(catId, mTwoPane, mSelectedItem,
                false, where, whereArgs);
        getSupportFragmentManager().beginTransaction().replace(R.id.entry_list, fragment).commit();
    }

    @Override
    public void onFileSelected(@NonNull String filePath) {
        FileImportDialog.showDialog(getSupportFragmentManager(), filePath);
    }

    /**
     * Enable export mode.
     */
    private void enableExportMode() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.entry_list);
        if(fragment instanceof EntryListFragment) {
            ((EntryListFragment)fragment).setExportMode(true, true);
        } else {
            onCatSelected(0, true);
        }
    }

    @Override
    public void onBackPressed() {
        if(mTwoPane) {
            final FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = fm.findFragmentById(R.id.entry_detail_container);
            if(fragment instanceof ViewEntryFragment) {
                if(!((ViewEntryFragment)fragment).onBackButtonPressed()) {
                    fragment = fm.findFragmentById(R.id.entry_list);
                    if(fragment instanceof EntryListFragment) {
                        ((EntryListFragment)fragment).clearSelection();
                    } else {
                        final ActionBar actionBar = getSupportActionBar();
                        if(actionBar != null) {
                            actionBar.setSubtitle(null);
                        }
                        onItemSelected(-1, null, 0);
                    }
                }
                return;
            } else if(fragment instanceof EntrySearchFragment) {
                fm.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .replace(R.id.entry_detail_container, mWelcomeFragment).commit();
                return;
            }
        }
        super.onBackPressed();
    }
}
