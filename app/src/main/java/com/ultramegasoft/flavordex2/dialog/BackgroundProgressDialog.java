package com.ultramegasoft.flavordex2.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Base for DialogFragments that host a background task.
 *
 * @author Steve Guidetti
 */
public abstract class BackgroundProgressDialog extends DialogFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState == null) {
            startTask();
        }
    }

    @Override
    public void onDestroyView() {
        final Dialog dialog = getDialog();
        if(dialog != null) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    /**
     * Start the task associated with this progress dialog.
     */
    protected abstract void startTask();
}
