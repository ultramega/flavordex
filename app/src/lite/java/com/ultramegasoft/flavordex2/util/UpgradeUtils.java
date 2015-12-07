package com.ultramegasoft.flavordex2.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

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
}
