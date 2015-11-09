package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.ultramegasoft.flavordex2.BuildConfig;
import com.ultramegasoft.flavordex2.R;

/**
 * Dialog that shows information about the application.
 *
 * @author Steve Guidetti
 */
public class AboutDialog extends DialogFragment {
    /**
     * Tag to identify the Fragment
     */
    private static final String TAG = "AboutDialog";

    /**
     * Show the dialog.
     *
     * @param fm The FragmentManager to use
     */
    public static void showDialog(FragmentManager fm) {
        final DialogFragment fragment = new AboutDialog();
        fragment.show(fm, TAG);
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View root = LayoutInflater.from(getContext()).inflate(R.layout.dialog_about, null);

        ((TextView)root.findViewById(R.id.about_version)).setText(BuildConfig.VERSION_NAME);
        root.findViewById(R.id.about_support).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmail();
            }
        });
        root.findViewById(R.id.about_website).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebsite();
            }
        });

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

        return new AlertDialog.Builder(getContext())
                .setIcon(R.drawable.ic_info)
                .setTitle(R.string.title_about)
                .setPositiveButton(R.string.button_ok, null)
                .setView(root)
                .create();
    }

    /**
     * Open an email client to send a support request.
     */
    private void openEmail() {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.about_email)});
        intent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.about_email_subject, BuildConfig.VERSION_NAME));
        if(intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, getString(R.string.about_send_email)));
        } else {
            Toast.makeText(getContext(), R.string.error_no_email, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Open the Flavordex website in a browser.
     */
    private void openWebsite() {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(R.string.about_website_url)));
        startActivity(intent);
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
