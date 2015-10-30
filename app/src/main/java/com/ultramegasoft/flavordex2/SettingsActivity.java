package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.ultramegasoft.flavordex2.dialog.BackendRegistrationDialog;
import com.ultramegasoft.flavordex2.dialog.CatListDialog;
import com.ultramegasoft.flavordex2.dialog.DriveConnectDialog;
import com.ultramegasoft.flavordex2.service.BackendService;
import com.ultramegasoft.flavordex2.service.PhotoSyncService;
import com.ultramegasoft.flavordex2.service.TaskService;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Activity for changing user preferences.
 *
 * @author Steve Guidetti
 */
public class SettingsActivity extends AppCompatActivity {
    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_EDIT_CAT = 400;

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
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
         * Preference items
         */
        private CheckBoxPreference mPrefLocation;
        private CheckBoxPreference mPrefSyncData;
        private CheckBoxPreference mPrefSyncPhotos;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);

            addPreferencesFromResource(R.xml.preferences);

            mPrefLocation = (CheckBoxPreference)findPreference(FlavordexApp.PREF_DETECT_LOCATION);
            mPrefSyncData = (CheckBoxPreference)findPreference(FlavordexApp.PREF_SYNC_DATA);
            mPrefSyncPhotos = (CheckBoxPreference)findPreference(FlavordexApp.PREF_SYNC_PHOTOS);

            setupEditCatsPref();
            setupLocationPref();
            setupSyncDataPref();
            setupSyncPhotosPref();
        }

        @Override
        public void onResume() {
            super.onResume();
            mPrefLocation.setEnabled(PermissionUtils.hasLocationPerm(getContext())
                    || PermissionUtils.shouldAskLocationPerm(getActivity()));
            mPrefSyncData.setEnabled(PermissionUtils.hasAccountsPerm(getContext())
                    || PermissionUtils.shouldAskAccountsPerm(getActivity()));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
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

        /**
         * Set up the location detection preference.
         */
        private void setupLocationPref() {
            final LocationManager lm =
                    (LocationManager)getActivity().getSystemService(LOCATION_SERVICE);
            if(!lm.getProviders(true).contains(LocationManager.NETWORK_PROVIDER)) {
                mPrefLocation.setVisible(false);
                return;
            }

            mPrefLocation.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            return !((boolean)o)
                                    || PermissionUtils.checkLocationPerm(SettingsFragment.this);
                        }
                    });
        }

        /**
         * Set up the data syncing preference.
         */
        private void setupSyncDataPref() {
            mPrefSyncData.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if(!(boolean)o) {
                                BackendService.unregisterClient(getContext());
                                return true;
                            }
                            BackendRegistrationDialog.showDialog(getFragmentManager());
                            return false;
                        }
                    });
        }

        /**
         * Set up the photo syncing preference.
         */
        private void setupSyncPhotosPref() {
            mPrefSyncPhotos.setEnabled(PermissionUtils.hasExternalStoragePerm(getContext()));

            mPrefSyncPhotos.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if((Boolean)o) {
                                DriveConnectDialog.showDialog(getFragmentManager());
                                return false;
                            }
                            return true;
                        }
                    });
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            PermissionUtils.onRequestPermissionsResult(getContext(), requestCode, permissions,
                    grantResults);
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
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(FlavordexApp.PREF_DETECT_LOCATION.equals(key)) {
                if(sharedPreferences.getBoolean(key, false)) {
                    mPrefLocation.setChecked(true);
                }
            } else if(FlavordexApp.PREF_SYNC_DATA.equals(key)) {
                if(sharedPreferences.getBoolean(key, false)) {
                    mPrefSyncData.setChecked(true);
                    BackendUtils.requestSync(getContext());
                } else {
                    GcmNetworkManager.getInstance(getContext()).cancelAllTasks(TaskService.class);
                }
            } else if(FlavordexApp.PREF_SYNC_PHOTOS.equals(key)) {
                if(sharedPreferences.getBoolean(key, false)) {
                    mPrefSyncPhotos.setChecked(true);
                    PhotoSyncService.syncPhotos(getContext());
                }
            }
        }
    }
}
