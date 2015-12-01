package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.dialog.AppChooserDialog;
import com.ultramegasoft.flavordex2.dialog.FileImportDialog;
import com.ultramegasoft.flavordex2.dialog.FileSelectorDialog;
import com.ultramegasoft.flavordex2.fragment.EntryListFragment;
import com.ultramegasoft.flavordex2.util.AppImportUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Full implementation of the main application Activity. Adds import and export menu items.
 *
 * @author Steve Guidetti
 * @see BaseEntryListActivity
 */
public class EntryListActivity extends BaseEntryListActivity
        implements FileSelectorDialog.OnFileSelectedCallbacks {
    @Override
    protected void loadPreferences(SharedPreferences prefs) {
        super.loadPreferences(prefs);
        if(prefs.getBoolean(FlavordexApp.PREF_FIRST_RUN, true)) {
            if(AppImportUtils.isAnyAppInstalled(this)) {
                AppChooserDialog.showDialog(getSupportFragmentManager(), true);
            }
            prefs.edit().putBoolean(FlavordexApp.PREF_FIRST_RUN, false).apply();
        }
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
                if(PermissionUtils.checkExternalStoragePerm(this,
                        R.string.message_request_storage_xport)) {
                    enableExportMode();
                }
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFileSelected(String filePath) {
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
}
