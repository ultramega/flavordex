package com.ultramegasoft.flavordex2.service;

import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * Service to handle execution of scheduled tasks.
 *
 * @author Steve Guidetti
 */
public class TaskService extends GcmTaskService {
    @Override
    public int onRunTask(TaskParams params) {
        Log.d(getClass().getSimpleName(), "Running Task: " + params.getTag());
        BackendService.syncData(this);
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
