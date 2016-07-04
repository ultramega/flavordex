package com.ultramegasoft.flavordex2.service;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.ultramegasoft.flavordex2.backend.BackendUtils;

/**
 * Service to handle execution of scheduled jobs.
 *
 * @author Steve Guidetti
 */
public class SyncService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters parameters) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch(parameters.getTag()) {
                    case BackendUtils.JOB_SYNC_DATA:
                        jobFinished(parameters, !syncData());
                        break;
                    case BackendUtils.JOB_SYNC_PHOTOS:
                        jobFinished(parameters, !syncPhotos());
                        break;
                    default:
                        jobFinished(parameters, false);
                }
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return false;
    }

    /**
     * Sync data with the backend.
     */
    private boolean syncData() {
        return new DataSyncHelper(this).sync();
    }

    /**
     * Sync photos with Google Drive.
     */
    private boolean syncPhotos() {
        final PhotoSyncHelper photoSyncHelper = new PhotoSyncHelper(this);
        if(photoSyncHelper.connect()) {
            photoSyncHelper.deletePhotos();
            photoSyncHelper.fetchPhotos();
            photoSyncHelper.pushPhotos();
            photoSyncHelper.disconnect();

            return true;
        }

        return false;
    }
}
