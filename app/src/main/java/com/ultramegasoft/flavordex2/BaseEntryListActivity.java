package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.dialog.AboutDialog;
import com.ultramegasoft.flavordex2.dialog.AppChooserDialog;
import com.ultramegasoft.flavordex2.fragment.CatListFragment;
import com.ultramegasoft.flavordex2.fragment.EntryListFragment;
import com.ultramegasoft.flavordex2.fragment.ViewEntryFragment;
import com.ultramegasoft.flavordex2.fragment.WelcomeFragment;
import com.ultramegasoft.flavordex2.util.AppImportUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Base class for the main application Activity. This shows a list of the categories or all the
 * journal entries in a category. On narrow screens, selecting an entry launches a new Activity to
 * show details. On wide screens, selecting an entry shows details in a Fragment in this Activity.
 *
 * @author Steve Guidetti
 */
public class BaseEntryListActivity extends AppCompatActivity {
    /**
     * Whether the Activity is in two-pane mode
     */
    private boolean mTwoPane;

    /**
     * Fragment to show in two-pane mode when no item is selected
     */
    private Fragment mWelcomeFragment;

    /**
     * The currently selected journal entry
     */
    private long mSelectedItem = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_list);

        if(findViewById(R.id.entry_detail_container) != null) {
            mTwoPane = true;
            mWelcomeFragment = new WelcomeFragment();
        }

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.entry_list, new CatListFragment()).commit();
            if(mTwoPane) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.entry_detail_container, mWelcomeFragment).commit();
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if(prefs.getBoolean(FlavordexApp.PREF_FIRST_RUN, true)) {
                if(AppImportUtils.isAnyAppInstalled(this)) {
                    AppChooserDialog.showDialog(getSupportFragmentManager(), true);
                }
                prefs.edit().putBoolean(FlavordexApp.PREF_FIRST_RUN, false).apply();
            }

            final long catId = prefs.getLong(FlavordexApp.PREF_LIST_CAT_ID, -1);
            if(catId > -1) {
                onCatSelected(catId, false);
            }

            PermissionUtils.checkExternalStoragePerm(this, R.string.message_request_storage);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_import_app:
                AppChooserDialog.showDialog(getSupportFragmentManager(), false);
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

    /**
     * Called by the CatListFragment when an item is selected.
     *
     * @param id         The category ID
     * @param exportMode Whether to start the entry list Fragment in export mode
     */
    public void onCatSelected(long id, boolean exportMode) {
        final Fragment fragment = new EntryListFragment();
        final Bundle args = new Bundle();
        args.putLong(EntryListFragment.ARG_CAT, id);
        args.putBoolean(EntryListFragment.ARG_TWO_PANE, mTwoPane);
        args.putLong(EntryListFragment.ARG_SELECTED_ITEM, mSelectedItem);
        args.putBoolean(EntryListFragment.ARG_EXPORT_MODE, exportMode);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.entry_list, fragment)
                .addToBackStack(null).commit();
    }

    /**
     * Called by the EntryListFragment when an item is selected.
     *
     * @param id      The entry ID
     * @param catName The name of the entry category
     * @param catId   The ID of the entry category
     */
    public void onItemSelected(long id, String catName, long catId) {
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
            }
        }
        super.onBackPressed();
    }
}
