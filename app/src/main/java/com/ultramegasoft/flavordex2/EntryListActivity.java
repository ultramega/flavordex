package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.dialog.AboutDialog;
import com.ultramegasoft.flavordex2.dialog.AppChooserDialog;
import com.ultramegasoft.flavordex2.dialog.FileImportDialog;
import com.ultramegasoft.flavordex2.dialog.FileSelectorDialog;
import com.ultramegasoft.flavordex2.fragment.CatListFragment;
import com.ultramegasoft.flavordex2.fragment.EntryListFragment;
import com.ultramegasoft.flavordex2.fragment.ViewEntryFragment;
import com.ultramegasoft.flavordex2.fragment.WelcomeFragment;
import com.ultramegasoft.flavordex2.util.AppImportUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * The main application Activity. This shows a list of all the journal entries. On narrow screens,
 * selecting an entry launches a new Activity to show details. On wide screens, selecting an entry
 * shows details in a Fragment in this Activity.
 *
 * @author Steve Guidetti
 */
public class EntryListActivity extends AppCompatActivity
        implements FileSelectorDialog.OnFileSelectedCallbacks {
    /**
     * Whether the Activity is in two-pane mode
     */
    private boolean mTwoPane;

    /**
     * Fragment to show in two-pane mode when no item is selected
     */
    private Fragment mWelcomeFragment;

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
            PermissionUtils.checkExternalStoragePerm(this, R.string.message_request_storage);
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
                if(!PermissionUtils.checkExternalStoragePerm(this,
                        R.string.message_request_storage_xport)) {
                    return true;
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
    public void onFileSelected(String filePath) {
        FileImportDialog.showDialog(getSupportFragmentManager(), filePath);
    }

    /**
     * Called by the CatListFragment when an item is selected.
     *
     * @param id The category ID
     */
    public void onCatSelected(long id, String catName) {
        final Fragment fragment = new EntryListFragment();
        final Bundle args = new Bundle();
        args.putLong(EntryListFragment.ARG_CAT, id);
        args.putString(EntryListFragment.ARG_CAT_NAME, catName);
        args.putBoolean(EntryListFragment.ARG_TWO_PANE, mTwoPane);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fragment_in_left, R.anim.fragment_out_left,
                        R.anim.fragment_in_right, R.anim.fragment_out_right)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.entry_list, fragment)
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
            final Fragment fragment = fm.findFragmentById(R.id.entry_detail_container);
            if(fragment instanceof ViewEntryFragment) {
                if(!((ViewEntryFragment)fragment).onBackButtonPressed()) {
                    ((EntryListFragment)fm.findFragmentById(R.id.entry_list)).clearSelection();
                }
                return;
            }
        }
        super.onBackPressed();
    }
}
