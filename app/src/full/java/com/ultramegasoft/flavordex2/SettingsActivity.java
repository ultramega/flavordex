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
import android.support.v4.app.Fragment;
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
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.twitter.sdk.android.core.TwitterCore;
import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.dialog.BackendRegistrationDialog;
import com.ultramegasoft.flavordex2.dialog.CatListDialog;
import com.ultramegasoft.flavordex2.dialog.DriveConnectDialog;
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
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        /**
         * Key for the category editing preference
         */
        private static final String PREF_EDIT_CATS = "pref_edit_cats";

        /**
         * Preference items
         */
        private CheckBoxPreference mPrefLocation;
        private SwitchPreferenceCompat mPrefAccount;
        private CheckBoxPreference mPrefSyncData;
        private CheckBoxPreference mPrefSyncPhotos;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences);

            mPrefLocation = (CheckBoxPreference)findPreference(FlavordexApp.PREF_DETECT_LOCATION);
            mPrefAccount = (SwitchPreferenceCompat)findPreference(FlavordexApp.PREF_ACCOUNT);
            mPrefSyncData = (CheckBoxPreference)findPreference(FlavordexApp.PREF_SYNC_DATA);
            mPrefSyncPhotos = (CheckBoxPreference)findPreference(FlavordexApp.PREF_SYNC_PHOTOS);

            setupEditCatsPref();
            setupLocationPref();
            setupAccountPref();
            setupSyncDataPref();
            setupSyncPhotosPref();

            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            mPrefLocation.setEnabled(PermissionUtils.hasLocationPerm(getContext())
                    || PermissionUtils.shouldAskLocationPerm(getActivity()));
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
         * Check if Google Play Services is available and show an error dialog if it is not.
         *
         * @return Whether Google Play Services is available
         */
        private boolean isGoogleAvailable() {
            final GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
            final int availability = gaa.isGooglePlayServicesAvailable(getContext());
            if(availability != ConnectionResult.SUCCESS) {
                gaa.showErrorDialogFragment(getActivity(), availability, 0);
                return false;
            }
            return true;
        }

        /**
         * Set up the account preference.
         */
        private void setupAccountPref() {
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null) {
                mPrefAccount.setSummary(getString(R.string.pref_summary_account_on,
                        user.getDisplayName()));
            }
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
         * Set up the data syncing preference.
         */
        private void setupSyncDataPref() {
            mPrefSyncData.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if(!(boolean)o) {
                                new UnregisterTask(getContext()).execute();
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
            } else if(FlavordexApp.PREF_ACCOUNT.equals(key)) {
                if(sharedPreferences.getBoolean(key, false)) {
                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(user != null) {
                        mPrefAccount.setSummary(getString(R.string.pref_summary_account_on,
                                user.getDisplayName()));
                    }
                    mPrefAccount.setChecked(true);
                } else {
                    new LogoutTask(getContext()).execute();
                    mPrefAccount.setChecked(false);
                }
            } else if(FlavordexApp.PREF_SYNC_DATA.equals(key)) {
                mPrefSyncData.setChecked(sharedPreferences.getBoolean(key, false));
            } else if(FlavordexApp.PREF_SYNC_PHOTOS.equals(key)) {
                mPrefSyncPhotos.setChecked(sharedPreferences.getBoolean(key, false));
            }
        }
    }

    /**
     * Task to log the user out in the background.
     */
    private static class LogoutTask extends AsyncTask<Void, Void, Void> {
        /**
         * The Context
         */
        private final Context mContext;

        /**
         * @param context The Context
         */
        public LogoutTask(Context context) {
            mContext = context.getApplicationContext();
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
            final GoogleApiClient apiClient = new GoogleApiClient.Builder(mContext)
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
         * The Context
         */
        private final Context mContext;

        /**
         * @param context The Context
         */
        public UnregisterTask(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Void... params) {
            BackendUtils.unregisterClient(mContext);
            return null;
        }
    }
}
