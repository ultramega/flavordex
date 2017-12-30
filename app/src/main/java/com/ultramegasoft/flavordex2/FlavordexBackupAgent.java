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
package com.ultramegasoft.flavordex2;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
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
    private static final String KEY_DB = "db";

    @Override
    public void onCreate() {
        super.onCreate();

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
