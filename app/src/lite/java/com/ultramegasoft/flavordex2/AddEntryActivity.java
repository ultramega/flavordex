package com.ultramegasoft.flavordex2;

import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Lite implementation of the Activity for adding a new journal entry. Adds an interstitial ad that
 * shows after a new entry is created.
 *
 * @author Steve Guidetti
 */
public class AddEntryActivity extends BaseAddEntryActivity {
    /**
     * The interstitial ad to display after a new entry is added
     */
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.ad_add_entry_interstitial));
        final AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    public void publishResult(long entryId, String entryCat, long entryCatId) {
        super.publishResult(entryId, entryCat, entryCatId);
        if(entryId > 0 && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }
}
