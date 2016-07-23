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
