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
import android.os.AsyncTask;
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
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.MenuItem;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.twitter.sdk.android.core.TwitterCore;
import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.dialog.AccountDialog;
import com.ultramegasoft.flavordex2.dialog.BackendRegistrationDialog;
import com.ultramegasoft.flavordex2.dialog.CatListDialog;
import com.ultramegasoft.flavordex2.dialog.DriveConnectDialog;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

import java.lang.ref.WeakReference;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case DriveConnectDialog.REQUEST_RESOLVE_CONNECTION:
                final Fragment fragment =
                        getSupportFragmentManager().findFragmentByTag(DriveConnectDialog.TAG);
                if(fragment != null) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }

    /**
     * The Fragment handling the preferences interface.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener,
            FirebaseAuth.AuthStateListener {
        /**
         * Key for the category editing preference
         */
        private static final String PREF_EDIT_CATS = "pref_edit_cats";
        private static final String PREF_EDIT_ACCOUNT = "pref_edit_account";

        /**
         * Preference items
         */
        private CheckBoxPreference mPrefLocation;
        private SwitchPreferenceCompat mPrefAccount;
        private Preference mPrefEditAccount;
        private CheckBoxPreference mPrefSyncData;
        private CheckBoxPreference mPrefSyncPhotos;

        /**
         * The FirebaseAuth instance
         */
        private FirebaseAuth mAuth;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences);

            mPrefLocation = (CheckBoxPreference)findPreference(FlavordexApp.PREF_DETECT_LOCATION);
            mPrefAccount = (SwitchPreferenceCompat)findPreference(FlavordexApp.PREF_ACCOUNT);
            mPrefEditAccount = findPreference(PREF_EDIT_ACCOUNT);
            mPrefSyncData = (CheckBoxPreference)findPreference(FlavordexApp.PREF_SYNC_DATA);
            mPrefSyncPhotos = (CheckBoxPreference)findPreference(FlavordexApp.PREF_SYNC_PHOTOS);

            setupEditCatsPref();
            setupLocationPref();
            setupAccountPref();
            setupEditAccountPref();
            setupSyncDataPref();
            setupSyncPhotosPref();

            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);

            mAuth = FirebaseAuth.getInstance();
        }

        @Override
        public void onStart() {
            super.onStart();
            mAuth.addAuthStateListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            mAuth.removeAuthStateListener(this);
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

        /**
         * Check if Google Play Services is available and show an error dialog if it is not.
         *
         * @return Whether Google Play Services is available
         */
        private boolean isGoogleAvailable() {
            final Activity activity = getActivity();
            if(activity == null) {
                return false;
            }

            final GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
            final int availability = gaa.isGooglePlayServicesAvailable(activity);
            if(availability != ConnectionResult.SUCCESS) {
                gaa.showErrorDialogFragment(activity, availability, 0);
                return false;
            }
            return true;
        }

        /**
         * Set up the account preference.
         */
        private void setupAccountPref() {
            mPrefAccount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if((boolean)o) {
                        if(isGoogleAvailable()) {
                            startActivity(new Intent(getContext(), LoginActivity.class));
                        }
                        return false;
                    }
                    return true;
                }
            });
        }

        /**
         * Set up the edit account preference.
         */
        private void setupEditAccountPref() {
            invalidateEditAccountPref();
            mPrefEditAccount
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            final FragmentManager fm = getFragmentManager();
                            if(fm != null) {
                                AccountDialog.showDialog(fm);
                            }
                            return false;
                        }
                    });
        }

        /**
         * Update the visibility of the edit account preference based on whether the current user
         * is authenticated using the email provider.
         */
        private void invalidateEditAccountPref() {
            mPrefEditAccount.setVisible(false);
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null) {
                for(UserInfo info : user.getProviderData()) {
                    if(info.getProviderId().equals(EmailAuthProvider.PROVIDER_ID)) {
                        mPrefEditAccount.setVisible(true);
                        return;
                    }
                }
            }
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
                                final Context context = getContext();
                                if(context != null) {
                                    new UnregisterTask(context).execute();
                                }
                                return true;
                            }

                            final FragmentManager fm = getFragmentManager();
                            if(fm != null) {
                                BackendRegistrationDialog.showDialog(fm);
                            }
                            return false;
                        }
                    });
        }

        /**
         * Set up the photo syncing preference.
         */
        private void setupSyncPhotosPref() {
            final Context context = getContext();
            if(context == null) {
                return;
            }

            mPrefSyncPhotos.setEnabled(PermissionUtils.hasExternalStoragePerm(context));

            mPrefSyncPhotos.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if((Boolean)o) {
                                final FragmentManager fm = getFragmentManager();
                                if(fm != null) {
                                    DriveConnectDialog.showDialog(fm);
                                }
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

            final Context context = getContext();
            if(context != null) {
                PermissionUtils.onRequestPermissionsResult(context, requestCode, permissions,
                        grantResults);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    break;
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(FlavordexApp.PREF_DETECT_LOCATION.equals(key)) {
                if(sharedPreferences.getBoolean(key, false)) {
                    mPrefLocation.setChecked(true);
                }
            } else if(FlavordexApp.PREF_ACCOUNT.equals(key)) {
                if(!sharedPreferences.getBoolean(key, false)) {
                    final Context context = getContext();
                    if(context != null) {
                        new LogoutTask(context).execute();
                    }
                    mPrefAccount.setChecked(false);
                }
                invalidateEditAccountPref();
            } else if(FlavordexApp.PREF_SYNC_DATA.equals(key)) {
                mPrefSyncData.setChecked(sharedPreferences.getBoolean(key, false));
            } else if(FlavordexApp.PREF_SYNC_PHOTOS.equals(key)) {
                mPrefSyncPhotos.setChecked(sharedPreferences.getBoolean(key, false));
            }
        }

        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null) {
                final String name = user.getDisplayName() == null ? user.getEmail()
                        : user.getDisplayName();
                mPrefAccount.setSummary(getString(R.string.pref_summary_account_on, name));
                mPrefAccount.setChecked(true);
            }
        }
    }

    /**
     * Task to log the user out in the background.
     */
    private static class LogoutTask extends AsyncTask<Void, Void, Void> {
        /**
         * The Context reference
         */
        @NonNull
        private final WeakReference<Context> mContext;

        /**
         * @param context The Context
         */
        LogoutTask(@NonNull Context context) {
            mContext = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null) {
                for(UserInfo info : user.getProviderData()) {
                    switch(info.getProviderId()) {
                        case GoogleAuthProvider.PROVIDER_ID:
                            logoutGoogle();
                            break;
                        case FacebookAuthProvider.PROVIDER_ID:
                            logoutFacebook();
                            break;
                        case TwitterAuthProvider.PROVIDER_ID:
                            logoutTwitter();
                            break;
                    }
                }
                FirebaseAuth.getInstance().signOut();
            }
            return null;
        }

        /**
         * Log the user out from Google.
         */
        private void logoutGoogle() {
            final Context context = mContext.get();
            if(context == null) {
                return;
            }

            final GoogleApiClient apiClient = new GoogleApiClient.Builder(context)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .build();
            final ConnectionResult result = apiClient.blockingConnect();
            if(result.isSuccess()) {
                Auth.GoogleSignInApi.signOut(apiClient);
            }
        }

        /**
         * Log the user out from Facebook.
         */
        private void logoutFacebook() {
            LoginManager.getInstance().logOut();
        }

        /**
         * Log the user out from Twitter.
         */
        private void logoutTwitter() {
            TwitterCore.getInstance().getSessionManager().clearActiveSession();
        }
    }

    /**
     * Task to unregister the client in the background.
     */
    private static class UnregisterTask extends AsyncTask<Void, Void, Void> {
        /**
         * The Context reference
         */
        @NonNull
        private final WeakReference<Context> mContext;

        /**
         * @param context The Context
         */
        UnregisterTask(@NonNull Context context) {
            mContext = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Context context = mContext.get();
            if(context != null) {
                BackendUtils.unregisterClient(context);
            }

            return null;
        }
    }
}
