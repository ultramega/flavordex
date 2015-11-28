package com.ultramegasoft.flavordex2;

import android.view.Menu;

import com.ultramegasoft.flavordex2.util.AppImportUtils;

/**
 * Lite implementation of the main application Activity.
 *
 * @author Steve Guidetti
 * @see BaseEntryListActivity
 */
public class EntryListActivity extends BaseEntryListActivity {
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean showAppImport = AppImportUtils.isAnyAppInstalled(this);
        menu.findItem(R.id.menu_import_app).setVisible(showAppImport);
        return super.onPrepareOptionsMenu(menu);
    }
}
