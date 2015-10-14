package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        /**
         * Key for the category editing preference
         */
        private static final String PREF_EDIT_CATS = "pref_edit_cats";

        /**
         * Request codes for external Activities
         */
        private static final int REQUEST_EDIT_CAT = 400;
        private static final int REQUEST_CONNECT_DRIVE = 401;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);

            addPreferencesFromResource(R.xml.preferences);

            setupSyncPhotosPref();
            setupLocationPref();
            setupEditCatsPref();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        /**
         * Set up the photo syncing preference.
         */
        private void setupSyncPhotosPref() {
            final Preference pref = findPreference(FlavordexApp.PREF_SYNC_PHOTOS);
            final boolean enabled = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(getContext()) == ConnectionResult.SUCCESS;
            pref.setEnabled(enabled && PermissionUtils.hasExternalStoragePerm(getContext()));

            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if((Boolean)o) {
                        connectToDrive();
                        return false;
                    }
                    return true;
                }
            });
        }

        /**
         * Launch the Activity to authorize the connection to Google Drive.
         */
        private void connectToDrive() {
            final Intent intent = new Intent(getContext(), DriveConnectActivity.class);
            startActivityForResult(intent, REQUEST_CONNECT_DRIVE);
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
            switch(requestCode) {
                case REQUEST_EDIT_CAT:
                    if(resultCode == Activity.RESULT_OK) {
                        EditCatActivity.startActivity(getContext(),
                                data.getLongExtra(CatListDialog.EXTRA_CAT_ID, 0),
                                data.getStringExtra(CatListDialog.EXTRA_CAT_NAME));
                    }
                    break;
                case REQUEST_CONNECT_DRIVE:
                    if(resultCode == RESULT_OK) {
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                .putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, true).apply();
                    } else if(data != null) {
                        GoogleApiAvailability.getInstance().showErrorDialogFragment(getActivity(),
                                data.getIntExtra(DriveConnectActivity.EXTRA_ERROR_CODE, 0), 0);
                    }
                    break;
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(FlavordexApp.PREF_SYNC_PHOTOS.equals(key)) {
                if(sharedPreferences.getBoolean(key, false)) {
                    ((CheckBoxPreference)findPreference(key)).setChecked(true);
                    PhotoSyncService.syncPhotos(getContext());
                }
            }
        }
    }
}
