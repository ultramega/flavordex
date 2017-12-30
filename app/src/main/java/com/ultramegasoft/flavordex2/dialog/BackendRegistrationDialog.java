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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.BackendUtils;

import java.lang.ref.WeakReference;

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
    public static void showDialog(@NonNull FragmentManager fm) {
        final DialogFragment fragment = new BackendRegistrationDialog();
        fragment.show(fm, TAG);
    }

    @Override
    protected void startTask() {
        final Context context = getContext();
        if(context != null) {
            new RegisterTask(context, this).execute();
        }
    }

    @SuppressWarnings("deprecation")
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
    private static class RegisterTask extends AsyncTask<Void, Void, Boolean> {
        /**
         * The Context reference
         */
        @NonNull
        private final WeakReference<Context> mContext;

        /**
         * The Fragment
         */
        @NonNull
        private final BackendRegistrationDialog mFragment;

        /**
         * @param context  The Context
         * @param fragment The Fragment
         */
        RegisterTask(@NonNull Context context, @NonNull BackendRegistrationDialog fragment) {
            mContext = new WeakReference<>(context.getApplicationContext());
            mFragment = fragment;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final Context context = mContext.get();
            return context != null && BackendUtils.registerClient(context);

        }

        @Override
        protected void onPostExecute(Boolean result) {
            final Context context = mContext.get();
            if(!result && context != null) {
                Toast.makeText(context, R.string.error_register_failed, Toast.LENGTH_LONG).show();
            }

            mFragment.dismiss();
        }
    }
}
