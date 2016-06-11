package com.ultramegasoft.flavordex2;

import android.view.MenuItem;

import com.ultramegasoft.flavordex2.util.UpgradeUtils;

/**
 * Lite implementation of the main application Activity. Adds an upgrade menu option.
 *
 * @author Steve Guidetti
 * @see BaseEntryListActivity
 */
public class EntryListActivity extends BaseEntryListActivity {
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_xport:
                UpgradeUtils.showDialog(getSupportFragmentManager());
                return true;
            case R.id.menu_upgrade:
                UpgradeUtils.openStore(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
