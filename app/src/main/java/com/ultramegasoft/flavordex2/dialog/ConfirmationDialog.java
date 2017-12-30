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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.HtmlCompat;

/**
 * Dialog to show confirmation messages.
 *
 * @author Steve Guidetti
 */
@SuppressWarnings("SameParameterValue")
public class ConfirmationDialog extends DialogFragment {
    private static final String TAG = "ConfirmationDialog";

    /**
     * Arguments for the Fragment
     */
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_ICON = "icon";
    private static final String ARG_DATA = "data";

    /**
     * Show a confirmation dialog.
     *
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to notify of the result
     * @param requestCode A number to identify this request
     * @param title       The dialog title
     * @param message     The dialog message
     */
    public static void showDialog(@NonNull FragmentManager fm, @Nullable Fragment target,
                                  int requestCode, @NonNull String title, @NonNull String message) {
        showDialog(fm, target, requestCode, title, message, R.drawable.ic_warning, null);
    }

    /**
     * Show a confirmation dialog.
     *
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to notify of the result
     * @param requestCode A number to identify this request
     * @param title       The dialog title
     * @param message     The dialog message
     * @param icon        Resource ID for the dialog icon
     */
    public static void showDialog(@NonNull FragmentManager fm, @Nullable Fragment target,
                                  int requestCode, @NonNull String title, @NonNull String message,
                                  int icon) {
        showDialog(fm, target, requestCode, title, message, icon, null);
    }

    /**
     * Show a confirmation dialog.
     *
     * @param fm          The FragmentManager to use
     * @param target      The Fragment to notify of the result
     * @param requestCode A number to identify this request
     * @param title       The dialog title
     * @param message     The dialog message
     * @param icon        Resource ID for the dialog icon
     * @param data        An Intent to store additional data
     */
    public static void showDialog(@NonNull FragmentManager fm, @Nullable Fragment target,
                                  int requestCode, @Nullable String title, @Nullable String message,
                                  int icon, @Nullable Intent data) {
        final DialogFragment fragment = new ConfirmationDialog();
        fragment.setTargetFragment(target, requestCode);

        final Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_ICON, icon);
        args.putParcelable(ARG_DATA, data);
        fragment.setArguments(args);

        fragment.show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        if(context == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        final Bundle args = getArguments();
        return new AlertDialog.Builder(context)
                .setIcon(args != null ? args.getInt(ARG_ICON) : 0)
                .setTitle(args != null ? args.getString(ARG_TITLE) : null)
                .setMessage(args != null ? HtmlCompat.fromHtml(args.getString(ARG_MESSAGE)) : null)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Fragment target = getTargetFragment();
                        if(target != null) {
                            final Intent data =
                                    args != null ? (Intent)args.getParcelable(ARG_DATA) : null;
                            target.onActivityResult(getTargetRequestCode(),
                                    Activity.RESULT_OK, data);
                        }
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                })
                .create();
    }
}
