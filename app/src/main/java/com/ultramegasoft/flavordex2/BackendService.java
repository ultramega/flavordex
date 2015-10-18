package com.ultramegasoft.flavordex2;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

/**
 * Service for accessing the backend.
 *
 * @author Steve Guidetti
 */
public class BackendService extends IntentService {
    /**
     * Keys for the Intent extras
     */
    private static final String EXTRA_COMMAND = "command";
    private static final String EXTRA_ACCOUNT_NAME = "account_name";

    /**
     * Commands this service will accept
     */
    private static final int COMMAND_REGISTER = 0;
    private static final int COMMAND_SYNC = 1;

    public BackendService() {
        super("BackendService");
    }

    /**
     * Register the client with the backend.
     *
     * @param context     The Context
     * @param accountName The name of the account to use
     */
    public static void registerClient(Context context, String accountName) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_REGISTER);
        intent.putExtra(EXTRA_ACCOUNT_NAME, accountName);
        context.startService(intent);
    }

    /**
     * Sync the journal data with the backend.
     *
     * @param context The Context
     */
    public static void syncData(Context context) {
        final Intent intent = new Intent(context, BackendService.class);
        intent.putExtra(EXTRA_COMMAND, COMMAND_SYNC);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch(intent.getIntExtra(EXTRA_COMMAND, -1)) {
            case COMMAND_REGISTER:
                doRegisterClient(intent.getStringExtra(EXTRA_ACCOUNT_NAME));
                break;
            case COMMAND_SYNC:
                doSyncData();
                break;
        }
    }

    /**
     * Handle client registration.
     *
     * @param accountName The name of the account to use
     */
    private void doRegisterClient(String accountName) {
        // TODO: 10/18/2015 Implement registration
    }

    /**
     * Handle journal data synchronization.
     */
    private void doSyncData() {
        // TODO: 10/18/2015 Implement syncing
    }
}
