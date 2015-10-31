package com.ultramegasoft.flavordex2.service;

import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.ultramegasoft.flavordex2.util.BackendUtils;

/**
 * Service to handle execution of scheduled tasks.
 *
 * @author Steve Guidetti
 */
public class TaskService extends GcmTaskService {
    private static final String TAG = "TaskService";

    @Override
    public int onRunTask(TaskParams params) {
        final String tag = params.getTag();
        Log.i(TAG, "Running Task: " + tag);
        if(BackendUtils.TASK_SYNC_DATA.equals(tag)) {
            if(BackendUtils.isSyncRequested(this)) {
                new DataSyncHelper(this).sync();
                BackendUtils.setLastSync(this);
                BackendUtils.requestSync(this, false);
                new PhotoSyncHelper(this).sync();
            }
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
