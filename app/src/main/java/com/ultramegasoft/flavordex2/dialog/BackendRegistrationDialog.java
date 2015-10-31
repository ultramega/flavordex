package com.ultramegasoft.flavordex2.dialog;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Dialog for registering the client device with the backend.
 *
 * @author Steve Guidetti
 */
public class BackendRegistrationDialog extends BackgroundProgressDialog {
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
            if(data != null) {
                final String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if(accountName != null) {
                    registerClient(accountName);
                    return;
                }
            }
        }
        setCancelable(true);
    }

    /**
     * Select an account to use to access the backend.
     */
    public void selectAccount() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final String accountName = prefs.getString(FlavordexApp.PREF_ACCOUNT_NAME, null);
        if(accountName == null) {
            final GoogleAccountCredential credential = BackendUtils.getCredential(getContext());
            final Intent intent = credential.newChooseAccountIntent();
            if(intent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
            } else {
                Toast.makeText(getContext(), R.string.error_get_accounts, Toast.LENGTH_LONG).show();
                dismiss();
            }
        } else {
            registerClient(accountName);
        }
    }

    /**
     * Register this client with the backend.
     *
     * @param accountName The account name
     */
    private void registerClient(String accountName) {
        new RegisterTask(accountName).execute();
    }

    /**
     * Task to register the client with the backend.
     */
    private class RegisterTask extends AsyncTask<Void, Void, Boolean> {
        /**
         * The account name
         */
        private final String mAccountName;

        /**
         * @param accountName The account name
         */
        public RegisterTask(String accountName) {
            mAccountName = accountName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return BackendUtils.registerClient(getContext(), mAccountName);
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
