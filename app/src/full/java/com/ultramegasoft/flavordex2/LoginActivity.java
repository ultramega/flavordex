package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Activity to allow the user to log in using one of the auth providers.
 *
 * @author Steve Guidetti
 */
public class LoginActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {
    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_LOGIN_GOOGLE = 900;

    /**
     * The Google API client
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * The FirebaseAuth instance
     */
    private FirebaseAuth mAuth;

    /**
     * The Firebase AuthStateListener
     */
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    /**
     * The Facebook CallbackManager
     */
    private CallbackManager mFacebookCallbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() != null) {
                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit()
                            .putBoolean(FlavordexApp.PREF_ACCOUNT, true).apply();
                    finish();
                }
            }
        };

        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_login);

        setupGoogle((SignInButton)findViewById(R.id.button_google));
        setupFacebook((LoginButton)findViewById(R.id.button_facebook));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
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
     * Set up Google sign-in.
     *
     * @param button The SignInButton
     */
    private void setupGoogle(SignInButton button) {
        final GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        button.setSize(SignInButton.SIZE_WIDE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithGoogle();
            }
        });
    }

    /**
     * Initiate the Google sign-in flow.
     */
    private void signInWithGoogle() {
        startActivityForResult(Auth.GoogleSignInApi
                .getSignInIntent(mGoogleApiClient), REQUEST_LOGIN_GOOGLE);
    }

    /**
     * Authenticate with Firebase using a Google token.
     *
     * @param account The GoogleSignInAccount
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        final AuthCredential credential =
                GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential);
    }

    /**
     * Set up Facebook sign-in.
     *
     * @param button The LoginButton
     */
    private void setupFacebook(LoginButton button) {
        mFacebookCallbackManager = CallbackManager.Factory.create();

        button.setReadPermissions("email", "public_profile");
        button.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                firebaseAuthWithFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {
                showError();
            }
        });
    }

    /**
     * Authenticate with Firebase using a Facebook token.
     *
     * @param token The AccessToken
     */
    private void firebaseAuthWithFacebook(AccessToken token) {
        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_LOGIN_GOOGLE:
                final GoogleSignInResult result =
                        Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if(result.isSuccess()) {
                    firebaseAuthWithGoogle(result.getSignInAccount());
                } else {
                    showError();
                }
                break;
            default:
                mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showError();
    }

    /**
     * Show a message indicating a login failure.
     */
    private void showError() {
        Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_LONG).show();
    }
}
