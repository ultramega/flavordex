package com.ultramegasoft.flavordex2.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import com.ultramegasoft.flavordex2.R;

/**
 * Dialog to show simple messages.
 *
 * @author Steve Guidetti
 */
public class MessageDialog extends DialogFragment {
    private static final String TAG = "MessageDialog";

    /**
     * Arguments for the Fragment
     */
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_ICON = "icon";

    /**
     * Show a confirmation dialog.
     *
     * @param fm      The FragmentManager to use
     * @param title   The dialog title
     * @param message The dialog message
     */
    public static void showDialog(FragmentManager fm, String title, String message) {
        showDialog(fm, title, message, R.drawable.ic_info);
    }

    /**
     * Show a confirmation dialog.
     *
     * @param fm      The FragmentManager to use
     * @param title   The dialog title
     * @param message The dialog message
     * @param icon    Resource ID for the dialog icon
     */
    public static void showDialog(FragmentManager fm, String title, String message, int icon) {
        final DialogFragment fragment = new MessageDialog();

        final Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_ICON, icon);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments();
        return new AlertDialog.Builder(getContext())
                .setIcon(args.getInt(ARG_ICON))
                .setTitle(args.getString(ARG_TITLE))
                .setMessage(Html.fromHtml(args.getString(ARG_MESSAGE)))
                .setPositiveButton(R.string.button_ok, null)
                .create();
    }
}
