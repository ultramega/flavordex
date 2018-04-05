/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
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
    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_EDIT_CAT = 400;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences);

            mPrefLocation = (CheckBoxPreference)findPreference(FlavordexApp.PREF_DETECT_LOCATION);

            setupEditCatsPref();
            setupLocationPref();

            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();

            final Activity activity = getActivity();
            if(activity != null) {
                mPrefLocation.setEnabled(PermissionUtils.hasLocationPerm(activity)
                        || PermissionUtils.shouldAskLocationPerm(activity));
            }
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
                    final FragmentManager fm = getFragmentManager();
                    if(fm != null) {
                        CatListDialog.showDialog(fm, SettingsFragment.this, REQUEST_EDIT_CAT);
                    }
                    return false;
                }
            });
        }

        /**
         * Set up the location detection preference.
         */
        private void setupLocationPref() {
            final Activity activity = getActivity();
            if(activity == null) {
                return;
            }

            final LocationManager lm = (LocationManager)activity.getSystemService(LOCATION_SERVICE);
            if(lm == null || !lm.getProviders(true).contains(LocationManager.NETWORK_PROVIDER)) {
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

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            final Context context = getContext();
            if(context != null) {
                PermissionUtils.onRequestPermissionsResult(context, requestCode, permissions,
                        grantResults);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            final Context context = getContext();
            if(context == null) {
                return;
            }

            switch(requestCode) {
                case REQUEST_EDIT_CAT:
                    if(resultCode == Activity.RESULT_OK) {
                        EditCatActivity.startActivity(context,
                                data.getLongExtra(CatListDialog.EXTRA_CAT_ID, 0),
                                data.getStringExtra(CatListDialog.EXTRA_CAT_NAME));
                    }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(FlavordexApp.PREF_DETECT_LOCATION.equals(key)) {
                if(sharedPreferences.getBoolean(key, false)) {
                    mPrefLocation.setChecked(true);
                }
            }
        }
    }
}
