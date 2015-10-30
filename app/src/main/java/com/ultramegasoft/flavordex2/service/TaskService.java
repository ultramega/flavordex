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
    @Override
    public int onRunTask(TaskParams params) {
        final String tag = params.getTag();
        Log.d(getClass().getSimpleName(), "Running Task: " + tag);
        if(BackendUtils.TASK_SYNC_DATA.equals(tag)) {
            BackendService.syncData(this);
        } else if(BackendUtils.TASK_SYNC_PHOTOS.equals(tag)) {
            PhotoSyncService.syncPhotos(this);
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
