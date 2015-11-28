package com.ultramegasoft.flavordex2.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.ultramegasoft.flavordex2.R;

/**
 * Full implementation of the Dialog that shows information about the application. Adds a link to
 * show the Google Play Services legal notice.
 *
 * @author Steve Guidetti
 */
public class AboutDialog extends BaseAboutDialog {
    @Override
    protected View getLayout() {
        final View root = super.getLayout();
        if(GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(getContext())
                != null) {
            final TextView gmsText = (TextView)root.findViewById(R.id.about_gms);
            gmsText.setVisibility(View.VISIBLE);
            gmsText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GmsNoticeDialog.showDialog(getFragmentManager());
                }
            });
        }

        return root;
    }

    /**
     * Dialog to show the Google Play Services legal notice.
     */
    public static class GmsNoticeDialog extends DialogFragment {
        private static final String TAG = "GmsNoticeDialog";

        /**
         * Show the dialog.
         *
         * @param fm The FragmentManager to use
         */
        public static void showDialog(FragmentManager fm) {
            final DialogFragment fragment = new GmsNoticeDialog();
            fragment.show(fm, TAG);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String message = GoogleApiAvailability.getInstance()
                    .getOpenSourceSoftwareLicenseInfo(getContext());
            return new AlertDialog.Builder(getContext())
                    .setMessage(message)
                    .setPositiveButton(R.string.button_ok, null)
                    .create();
        }
    }
}
