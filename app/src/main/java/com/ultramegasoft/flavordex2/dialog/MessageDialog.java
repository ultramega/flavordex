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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.HtmlCompat;

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
    public static void showDialog(@NonNull FragmentManager fm, @NonNull String title,
                                  @NonNull String message) {
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
    public static void showDialog(@NonNull FragmentManager fm, @NonNull String title,
                                  @NonNull String message, int icon) {
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
                .setMessage(HtmlCompat.fromHtml(args.getString(ARG_MESSAGE)))
                .setPositiveButton(R.string.button_ok, null)
                .create();
    }
}
