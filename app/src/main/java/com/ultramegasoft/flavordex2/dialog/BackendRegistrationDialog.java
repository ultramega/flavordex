package com.ultramegasoft.flavordex2.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.BackendUtils;

/**
 * Dialog for registering the client device with the backend.
 *
 * @author Steve Guidetti
 */
public class BackendRegistrationDialog extends BackgroundProgressDialog {
    private static final String TAG = "BackendRegistrationDialog";

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
        new RegisterTask().execute();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage(getString(R.string.message_registering_client));
        return dialog;
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
