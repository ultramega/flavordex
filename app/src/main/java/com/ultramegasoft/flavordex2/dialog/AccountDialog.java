package com.ultramegasoft.flavordex2.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
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
    private FirebaseUser mUser;

    /**
     * The current email address
     */
    private String mCurrentEmail;

    /**
     * Handles reauthenticating the user before requesting a change.
     */
    private abstract class AccountUpdater {
        /**
         * Execute the update request.
         */
        public void execute() {
            final String password = mTxtPassword.getText().toString();
            mUser.reauthenticate(EmailAuthProvider.getCredential(mCurrentEmail, password))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(!task.isSuccessful()) {
                                try {
                                    throw task.getException();
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
                                doTask();
                            }
                        }
                    });
        }

        /**
         * Executed after the user is successfully reauthenticated.
         */
        protected abstract void doTask();
    }

    /**
     * Show the dialog.
     *
     * @param fm The FragmentManager to use
     */
    public static void showDialog(FragmentManager fm) {
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
        final View root =
                LayoutInflater.from(getContext()).inflate(R.layout.dialog_account, null, false);

        mTxtMessage = (TextView)root.findViewById(R.id.message);
        mTxtPassword = (EditText)root.findViewById(R.id.password);
        mTxtEmail = (EditText)root.findViewById(R.id.email);
        mTxtNewPassword = (EditText)root.findViewById(R.id.new_password);
        mButtonEmail = (Button)root.findViewById(R.id.button_change_email);
        mButtonPassword = (Button)root.findViewById(R.id.button_change_password);

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

        return new AlertDialog.Builder(getContext())
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
        new AccountUpdater() {
            protected void doTask() {
                mUser.updateEmail(mTxtEmail.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(!task.isSuccessful()) {
                                    try {
                                        throw task.getException();
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
                                        Log.e(TAG, e.getMessage());
                                        onUnknownError();
                                    }
                                } else {
                                    mTxtPassword.setText(null);
                                    mCurrentEmail = mUser.getEmail();
                                    BackendUtils.setEmail(getContext(), mCurrentEmail);
                                    mTxtMessage.setText(R.string.message_email_changed);
                                }
                            }
                        });
            }
        }.execute();
    }

    /**
     * Initiate a request to change the password for the current user.
     */
    private void changePassword() {
        new AccountUpdater() {
            protected void doTask() {
                mUser.updatePassword(mTxtNewPassword.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(!task.isSuccessful()) {
                                    mTxtNewPassword.setText(null);
                                    try {
                                        throw task.getException();
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
        }.execute();
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
