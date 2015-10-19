package com.ultramegasoft.flavordex2.dialog;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.service.BackendService;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Dialog for registering the client device with the backend.
 *
 * @author Steve Guidetti
 */
public class BackendRegistrationDialog extends DialogFragment {
    /**
     * Tag to identify the Fragment
     */
    private static final String TAG = "BackendRegistrationDialog";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_SELECT_ACCOUNT = 900;

    /**
     * Listener for the BackendService
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dismiss();
        }
    };

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);

        final IntentFilter filter = new IntentFilter(BackendService.ACTION_COMPLETED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);

        if(savedInstanceState == null && PermissionUtils.checkAccountsPerm(this)) {
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
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
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
            startActivityForResult(credential.newChooseAccountIntent(), REQUEST_SELECT_ACCOUNT);
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
        BackendService.registerClient(getContext(), accountName);
    }
}
