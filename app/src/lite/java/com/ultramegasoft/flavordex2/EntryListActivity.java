package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.ultramegasoft.flavordex2.util.UpgradeUtils;

/**
 * Lite implementation of the main application Activity. Adds a banner ad and an upgrade menu
 * option.
 *
 * @author Steve Guidetti
 * @see BaseEntryListActivity
 */
public class EntryListActivity extends BaseEntryListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final AdView adView = (AdView)findViewById(R.id.ad_banner);
        if(adView != null) {
            final AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();
            adView.loadAd(adRequest);
        }
    }

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
