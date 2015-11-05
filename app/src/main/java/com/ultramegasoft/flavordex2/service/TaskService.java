package com.ultramegasoft.flavordex2.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.ultramegasoft.flavordex2.FlavordexApp;
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
        if(BackendUtils.TASK_SYNC_DATA.equals(tag)) {
            syncData();
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    /**
     * Sync data with the backend and photos with Google Drive.
     */
    private void syncData() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if(prefs.getBoolean(FlavordexApp.PREF_SYNC_DATA, false)) {
            PhotoSyncHelper photoSyncHelper = null;

            if(prefs.getBoolean(FlavordexApp.PREF_SYNC_PHOTOS, false)) {
                photoSyncHelper = new PhotoSyncHelper(this);
                if(BackendUtils.isPhotoSyncRequested(this)) {
                    BackendUtils.requestPhotoSync(this, false);
                    if(photoSyncHelper.connect()) {
                        photoSyncHelper.deletePhotos();
                        photoSyncHelper.pushPhotos();
                        photoSyncHelper.fetchPhotos();
                    } else {
                        BackendUtils.requestPhotoSync(this);
                    }
                }
            }

            if(BackendUtils.isDataSyncRequested(this)) {
                BackendUtils.requestDataSync(this, false);
                if(!new DataSyncHelper(this, photoSyncHelper).sync()) {
                    BackendUtils.requestDataSync(this);
                }
            }

            if(photoSyncHelper != null) {
                photoSyncHelper.disconnect();
            }
        }
    }
}
