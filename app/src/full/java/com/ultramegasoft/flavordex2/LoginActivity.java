package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.ultramegasoft.flavordex2.backend.BackendUtils;

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

    /**
     * The TwitterLoginButton
     */
    private TwitterLoginButton mTwitterLoginButton;

    /**
     * The ViewSwitcher to switch between the login buttons and the progress indicator
     */
    private ViewSwitcher mSwitcher;

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
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null) {
                    BackendUtils.setUid(LoginActivity.this, user.getUid());
                    finish();
                }
            }
        };

        setContentView(R.layout.activity_login);

        mSwitcher = (ViewSwitcher)findViewById(R.id.switcher);

        setupGoogle((SignInButton)findViewById(R.id.button_google));
        setupFacebook((LoginButton)findViewById(R.id.button_facebook));
        setupTwitter((TwitterLoginButton)findViewById(R.id.button_twitter));
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
        mSwitcher.setDisplayedChild(1);
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
                mSwitcher.setDisplayedChild(0);
            }

            @Override
            public void onError(FacebookException e) {
                showError();
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwitcher.setDisplayedChild(1);
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

    /**
     * Set up Twitter sign-in.
     *
     * @param button The TwitterLoginButton
     */
    private void setupTwitter(TwitterLoginButton button) {
        mTwitterLoginButton = button;
        mTwitterLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                firebaseAuthWithTwitter(result.data);
            }

            @Override
            public void failure(TwitterException exception) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        mSwitcher.setDisplayedChild(0);
                    }
                });
            }
        });
        mTwitterLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwitcher.setDisplayedChild(1);
            }
        });
    }

    /**
     * Authenticate with Firebase using a Twitter token.
     *
     * @param session The TwitterSession
     */
    private void firebaseAuthWithTwitter(TwitterSession session) {
        final AuthCredential credential = TwitterAuthProvider.getCredential(
                session.getAuthToken().token,
                session.getAuthToken().secret);
        mAuth.signInWithCredential(credential);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_LOGIN_GOOGLE:
                if(resultCode == RESULT_OK) {
                    final GoogleSignInResult result =
                            Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                    if(result.isSuccess()) {
                        firebaseAuthWithGoogle(result.getSignInAccount());
                    } else {
                        showError();
                    }
                } else {
                    mSwitcher.setDisplayedChild(0);
                }
                break;
            default:
                mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
                mTwitterLoginButton.onActivityResult(requestCode, resultCode, data);
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
        mSwitcher.setDisplayedChild(0);
    }
}
