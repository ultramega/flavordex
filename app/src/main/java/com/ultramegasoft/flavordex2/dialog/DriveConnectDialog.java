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
    public static final String TAG = "DriveConnectDialog";

    /**
     * Request codes for external Activities
     */
    public static final int REQUEST_RESOLVE_CONNECTION = 800;

    /**
     * The Google Play Services client
     */
    private GoogleApiClient mClient;

    /**
     * Show the dialog.
     *
     * @param fm The FragmentManager to use
     */
    public static void showDialog(@NonNull FragmentManager fm) {
        final DialogFragment fragment = new DriveConnectDialog();
        fragment.show(fm, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClient = new GoogleApiClient.Builder(getContext(), this, this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if(result.hasResolution()) {
            try {
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_CONNECTION);
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
