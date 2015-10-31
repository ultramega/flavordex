package com.ultramegasoft.flavordex2.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;

/**
 * Dialog for authorizing the application to connect to Google Drive.
 *
 * @author Steve Guidetti
 */
public class DriveConnectDialog extends DialogFragment
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DriveConnectDialog";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_RESOLVE_CONNECTION = 800;

    /**
     * The Google Play Services client
     */
    private GoogleApiClient mClient;

    /**
     * Show the dialog.
     *
     * @param fm The FragmentManager to use
     */
    public static void showDialog(FragmentManager fm) {
        final DialogFragment fragment = new DriveConnectDialog();
        fragment.show(fm, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClient = new GoogleApiClient.Builder(getContext(), this, this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();
        if(savedInstanceState == null) {
            mClient.connect();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage(getString(R.string.message_connecting_drive));
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mClient.disconnect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_RESOLVE_CONNECTION:
                if(resultCode == Activity.RESULT_OK) {
                    mClient.connect();
                } else {
                    dismiss();
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putBoolean(FlavordexApp.PREF_SYNC_PHOTOS, true).apply();
        dismiss();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if(result.hasResolution()) {
            try {
                final int requestCode = REQUEST_RESOLVE_CONNECTION
                        + ((getFragmentManager().getFragments().indexOf(this) + 1) << 16);
                result.startResolutionForResult(getActivity(), requestCode);
            } catch(IntentSender.SendIntentException e) {
                Log.e(TAG, "Connection to Google Drive failed", e);
                dismiss();
            }
        } else {
            GoogleApiAvailability.getInstance()
                    .showErrorDialogFragment(getActivity(), result.getErrorCode(), 0);
            dismiss();
        }
    }
}
