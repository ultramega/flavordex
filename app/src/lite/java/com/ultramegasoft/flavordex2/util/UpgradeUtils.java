package com.ultramegasoft.flavordex2.util;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.ultramegasoft.flavordex2.R;

/**
 * Helpers for upgrading to Flavordex 2.
 *
 * @author Steve Guidetti
 */
public class UpgradeUtils {
    /**
     * The package name of the full version of Flavordex 2
     */
    private static final String PACKAGE = "com.ultramegasoft.flavordex2";

    /**
     * Open the application in the store.
     *
     * @param context The Context
     */
    public static void openStore(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Intent intent = new Intent(Intent.ACTION_VIEW);

        try {
            intent.setData(Uri.parse("market://details?id=" + PACKAGE));
            if(intent.resolveActivity(pm) == null) {
                intent.setData(Uri.parse("amzn://apps/android?p=" + PACKAGE));
                if(intent.resolveActivity(pm) == null) {
                    intent.setData(Uri.parse("http://flavordex.com/"));
                }
            }

            context.startActivity(intent);
        } catch(ActivityNotFoundException ignored) {
        }
    }

    /**
     * Show the upgrade dialog.
     *
     * @param fm The FragmentManager
     */
    public static void showDialog(FragmentManager fm) {
        final DialogFragment fragment = new UpgradeDialog();
        fragment.show(fm, UpgradeDialog.TAG);
    }

    /**
     * The dialog asking the user to upgrade to use a feature from the full version.
     */
    public static class UpgradeDialog extends DialogFragment {
        private final static String TAG = "UpgradeDialog";

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setIcon(R.drawable.ic_info)
                    .setTitle(R.string.title_upgrade)
                    .setMessage(R.string.message_upgrade)
                    .setPositiveButton(R.string.button_upgrade,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    openStore(getContext());
                                }
                            })
                    .setNegativeButton(R.string.button_cancel, null)
                    .create();
        }
    }
}
