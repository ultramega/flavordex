package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

/**
 * Activity for authorizing the application to connect to Google Drive.
 *
 * @author Steve Guidetti
 */
public class DriveConnectActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    /**
     * Intent extras for the result data
     */
    public static final String EXTRA_ERROR_CODE = "error_code";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_RESOLVE_CONNECTION = 800;

    /**
     * The Google Play Services client
     */
    private GoogleApiClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClient = new GoogleApiClient.Builder(this, this, this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();
        mClient.connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_RESOLVE_CONNECTION:
                if(resultCode == RESULT_OK) {
                    mClient.connect();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if(result.hasResolution()) {
            try {
                result.startResolutionForResult(this, REQUEST_RESOLVE_CONNECTION);
            } catch(IntentSender.SendIntentException e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
            }
        } else {
            final Intent intent = new Intent();
            intent.putExtra(EXTRA_ERROR_CODE, result.getErrorCode());
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }
}
