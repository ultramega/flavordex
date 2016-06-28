package com.ultramegasoft.flavordex2.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.backend.BackendUtils;

/**
 * Service to handle execution of scheduled jobs.
 *
 * @author Steve Guidetti
 */
public class SyncService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters parameters) {
        final String tag = parameters.getTag();
        if(BackendUtils.JOB_SYNC_DATA.equals(tag)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    syncData();
                    jobFinished(parameters, false);
                }
            }).start();
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return false;
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
                        photoSyncHelper.fetchPhotos();
                        photoSyncHelper.pushPhotos();
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
