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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.backend.BackendUtils;

/**
 * Dialog for changing the email address or password for an email based account.
 *
 * @author Steve Guidetti
 */
public class AccountDialog extends DialogFragment {
    private static final String TAG = "AccountDialog";

    /**
     * Views from the layout
     */
    private TextView mTxtMessage;
    private EditText mTxtPassword;
    private EditText mTxtEmail;
    private EditText mTxtNewPassword;
    private Button mButtonEmail;
    private Button mButtonPassword;

    /**
     * The current user
     */
    @Nullable
    private FirebaseUser mUser;

    /**
     * The current email address
     */
    @Nullable
    private String mCurrentEmail;

    /**
     * Handles reauthenticating the user before requesting a change.
     */
    private abstract class AccountUpdater {
        /**
         * Execute the update request.
         *
         * @param context The Context
         */
        public void execute(@NonNull final Context context) {
            if(mUser == null || mCurrentEmail == null) {
                return;
            }
            final String password = mTxtPassword.getText().toString();
            mUser.reauthenticate(EmailAuthProvider.getCredential(mCurrentEmail, password))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(!task.isSuccessful()) {
                                try {
                                    final Exception exception = task.getException();
                                    if(exception == null) {
                                        throw new NullPointerException("exception is null");
                                    }
                                    throw exception;
                                } catch(FirebaseAuthInvalidUserException e) {
                                    onUserError();
                                } catch(FirebaseAuthInvalidCredentialsException e) {
                                    mTxtPassword.setText(null);
                                    mTxtPassword
                                            .setError(getString(R.string.error_incorrect_password));
                                    mTxtPassword.requestFocus();
                                } catch(Exception e) {
                                    Log.e(TAG, e.getMessage());
                                    onUnknownError();
                                }
                            } else {
                                doTask(context.getApplicationContext());
                            }
                        }
                    });
        }

        /**
         * Executed after the user is successfully reauthenticated.
         *
         * @param context The Context
         */
        protected abstract void doTask(@NonNull Context context);
    }

    /**
     * Show the dialog.
     *
     * @param fm The FragmentManager to use
     */
    public static void showDialog(@NonNull FragmentManager fm) {
        new AccountDialog().show(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mUser == null) {
            onUserError();
            return;
        }
        mCurrentEmail = mUser.getEmail();
        if(mCurrentEmail == null) {
            dismiss();
        }
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        if(context == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        final View root =
                LayoutInflater.from(context).inflate(R.layout.dialog_account, null, false);

        mTxtMessage = root.findViewById(R.id.message);
        mTxtPassword = root.findViewById(R.id.password);
        mTxtEmail = root.findViewById(R.id.email);
        mTxtNewPassword = root.findViewById(R.id.new_password);
        mButtonEmail = root.findViewById(R.id.button_change_email);
        mButtonPassword = root.findViewById(R.id.button_change_password);

        mTxtEmail.setText(mCurrentEmail);

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkForm();
            }
        };

        mTxtPassword.addTextChangedListener(textWatcher);
        mTxtEmail.addTextChangedListener(textWatcher);
        mTxtNewPassword.addTextChangedListener(textWatcher);

        mButtonEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                clearMessages();
                changeEmail();
            }
        });

        mButtonPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                clearMessages();
                changePassword();
            }
        });

        return new AlertDialog.Builder(context)
                .setTitle(R.string.title_account)
                .setView(root)
                .setPositiveButton(R.string.button_close, null)
                .create();
    }

    /**
     * Update the status of the form buttons based on the field contents.
     */
    private void checkForm() {
        final boolean hasPassword = !TextUtils.isEmpty(mTxtPassword.getText().toString());
        final boolean hasEmail = !mTxtEmail.getText().toString().equals(mCurrentEmail);
        final boolean hasNewPassword = mTxtNewPassword.getText().toString().length() >= 6;
        mButtonEmail.setEnabled(hasPassword && hasEmail);
        mButtonPassword.setEnabled(hasPassword && hasNewPassword);
    }

    /**
     * Clear all messages from the form.
     */
    private void clearMessages() {
        mTxtMessage.setText(null);
        mTxtPassword.setError(null);
        mTxtEmail.setError(null);
        mTxtNewPassword.setError(null);
    }

    /**
     * Initiate a request to change the email address for the current user.
     */
    private void changeEmail() {
        final Context context = getContext();
        if(context == null) {
            return;
        }

        new AccountUpdater() {
            protected void doTask(@NonNull final Context context) {
                if(mUser == null) {
                    return;
                }
                mUser.updateEmail(mTxtEmail.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(!task.isSuccessful()) {
                                    try {
                                        final Exception exception = task.getException();
                                        if(exception == null) {
                                            throw new NullPointerException("exception is null");
                                        }
                                        throw exception;
                                    } catch(FirebaseAuthInvalidCredentialsException e) {
                                        mTxtEmail.setError(getString(R.string.error_invalid_email));
                                        mTxtEmail.requestFocus();
                                    } catch(FirebaseAuthUserCollisionException e) {
                                        mTxtEmail.setError(getString(R.string.error_user_exists));
                                        mTxtEmail.requestFocus();
                                    } catch(FirebaseAuthInvalidUserException
                                            | FirebaseAuthRecentLoginRequiredException e) {
                                        onUserError();
                                    } catch(Exception e) {
                                        if(e.getMessage().contains("INVALID_EMAIL")) {
                                            mTxtEmail.setError(
                                                    getString(R.string.error_invalid_email));
                                            mTxtEmail.requestFocus();
                                        } else if(e.getMessage().contains("EMAIL_EXISTS")) {
                                            mTxtEmail.setError(
                                                    getString(R.string.error_user_exists));
                                            mTxtEmail.requestFocus();
                                        } else {
                                            Log.e(TAG, e.getMessage());
                                            onUnknownError();
                                        }
                                    }
                                } else {
                                    mTxtPassword.setText(null);
                                    mCurrentEmail = mUser.getEmail();
                                    BackendUtils.setEmail(context, mCurrentEmail);
                                    mTxtMessage.setText(R.string.message_email_changed);
                                }
                            }
                        });
            }
        }.execute(context);
    }

    /**
     * Initiate a request to change the password for the current user.
     */
    private void changePassword() {
        final Context context = getContext();
        if(context == null) {
            return;
        }

        new AccountUpdater() {
            protected void doTask(@NonNull Context context) {
                if(mUser == null) {
                    return;
                }
                mUser.updatePassword(mTxtNewPassword.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(!task.isSuccessful()) {
                                    mTxtNewPassword.setText(null);
                                    try {
                                        final Exception exception = task.getException();
                                        if(exception == null) {
                                            throw new NullPointerException("exception is null");
                                        }
                                        throw exception;
                                    } catch(FirebaseAuthWeakPasswordException e) {
                                        mTxtNewPassword
                                                .setError(getString(R.string.error_weak_password));
                                        mTxtNewPassword.requestFocus();
                                    } catch(FirebaseAuthInvalidUserException
                                            | FirebaseAuthRecentLoginRequiredException e) {
                                        onUserError();
                                    } catch(Exception e) {
                                        Log.e(TAG, e.getMessage());
                                        onUnknownError();
                                    }
                                } else {
                                    mTxtPassword.setText(null);
                                    mTxtNewPassword.setText(null);
                                    mTxtMessage.setText(R.string.message_password_changed);
                                }
                            }
                        });
            }
        }.execute(context);
    }

    /**
     * Log the user out and show an error in case of an authentication error.
     */
    private void onUserError() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(getContext(), R.string.error_not_logged_in, Toast.LENGTH_LONG).show();
        dismiss();
    }

    /**
     * Show an error message when an unknown error occurs.
     */
    private void onUnknownError() {
        checkForm();
        Toast.makeText(getContext(), R.string.error_unexpected, Toast.LENGTH_LONG).show();
    }
}
