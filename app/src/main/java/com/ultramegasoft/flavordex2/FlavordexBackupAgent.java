package com.ultramegasoft.flavordex2;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import com.ultramegasoft.flavordex2.provider.DatabaseHelper;
import com.ultramegasoft.flavordex2.provider.FlavordexProvider;

import java.io.IOException;

/**
 * Backup agent for the application.
 *
 * @author Steve Guidetti
 */
public class FlavordexBackupAgent extends BackupAgentHelper {
    /**
     * Keys for backed up items
     */
    private static final String KEY_PREFS = "prefs";
    private static final String KEY_DB = "db";

    @Override
    public void onCreate() {
        final SharedPreferencesBackupHelper prefs =
                new SharedPreferencesBackupHelper(this, getPackageName() + "_preferences");
        addHelper(KEY_PREFS, prefs);

        final FileBackupHelper db = new FileBackupHelper(this,
                "../databases/" + DatabaseHelper.DATABASE_NAME);
        addHelper(KEY_DB, db);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        synchronized(FlavordexProvider.class) {
            super.onBackup(oldState, data, newState);
        }
    }
}
