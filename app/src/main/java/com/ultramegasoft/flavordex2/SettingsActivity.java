package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.dialog.CatListDialog;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Activity for changing user preferences.
 *
 * @author Steve Guidetti
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null) {
            final Fragment fragment = new SettingsFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, EntryListActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    /**
     * The Fragment handling the preferences interface.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        /**
         * Key for the category editing preference
         */
        private static final String PREF_EDIT_CATS = "pref_edit_cats";

        /**
         * Request codes for external Activities
         */
        private static final int REQUEST_EDIT_CAT = 100;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences);

            setupLocationPref();
            setupEditCatsPref();
        }

        /**
         * Set up the location detection preference.
         */
        private void setupLocationPref() {
            final Preference pref = findPreference(FlavordexApp.PREF_DETECT_LOCATION);
            final LocationManager lm =
                    (LocationManager)getActivity().getSystemService(LOCATION_SERVICE);
            pref.setEnabled(lm.getProviders(true).contains(LocationManager.NETWORK_PROVIDER));

            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    return !((boolean)o) || PermissionUtils.checkLocationPerm(getActivity());
                }
            });
        }

        /**
         * Set up the category editing preference.
         */
        private void setupEditCatsPref() {
            final Preference pref = findPreference(PREF_EDIT_CATS);
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CatListDialog.showDialog(getFragmentManager(), SettingsFragment.this,
                            REQUEST_EDIT_CAT);
                    return false;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(resultCode == Activity.RESULT_OK) {
                switch(requestCode) {
                    case REQUEST_EDIT_CAT:
                        final Intent intent = new Intent(getContext(), EditCatActivity.class);
                        intent.putExtra(EditCatActivity.EXTRA_CAT_ID,
                                data.getLongExtra(CatListDialog.EXTRA_CAT_ID, 0));
                        intent.putExtra(EditCatActivity.EXTRA_CAT_NAME,
                                data.getStringExtra(CatListDialog.EXTRA_CAT_NAME));
                        startActivity(intent);
                        break;
                }
            }
        }
    }
}
