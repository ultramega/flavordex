package com.ultramegasoft.flavordex2.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.BackendUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Dialog for registering the client device with the backend.
 *
 * @author Steve Guidetti
 */
public class BackendRegistrationDialog extends BackgroundProgressDialog
        implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "BackendRegistrationDialog";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_SELECT_ACCOUNT = 900;

    /**
     * Show the dialog.
     *
     * @param fm The FragmentManager to use
     */
    public static void showDialog(FragmentManager fm) {
        final DialogFragment fragment = new BackendRegistrationDialog();
        fragment.show(fm, TAG);
    }

    @Override
    protected void startTask() {
        if(PermissionUtils.checkAccountsPerm(this)) {
            selectAccount();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage(getString(R.string.message_registering_client));
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isCancelable()) {
            dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(requestCode == PermissionUtils.REQUEST_ACCOUNTS) {
            if(PermissionUtils.hasAccountsPerm(getContext())) {
                selectAccount();
            } else {
                setCancelable(true);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_SELECT_ACCOUNT) {
            final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()) {
                firebaseAuthWithGoogle(result.getSignInAccount());
            }
        }
        setCancelable(true);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        final AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            registerClient();
                        }
                    }
                });
    }

    /**
     * Select an account to use to access the backend.
     */
    public void selectAccount() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null) {
            final GoogleSignInOptions gso =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.google_web_client_id))
                            .requestEmail()
                            .build();
            final GoogleApiClient client = new GoogleApiClient.Builder(getContext())
                    .enableAutoManage(getActivity(), this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
            startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(client),
                    REQUEST_SELECT_ACCOUNT);
        } else {
            registerClient();
        }
    }

    /**
     * Register this client with the backend.
     */
    private void registerClient() {
        new RegisterTask().execute();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * Task to register the client with the backend.
     */
    private class RegisterTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            return BackendUtils.registerClient(getContext());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(!result) {
                Toast.makeText(getContext(), R.string.error_register_failed, Toast.LENGTH_LONG)
                        .show();
            }
            dismiss();
        }
    }
}
