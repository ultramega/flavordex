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
package com.ultramegasoft.flavordex2.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.PermissionUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Abstract class for shared photo Fragment functionality.
 *
 * @author Steve Guidetti
 */
abstract class AbsPhotosFragment extends Fragment {
    private static final String TAG = "AbsPhotosFragment";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final int REQUEST_SELECT_IMAGE = 101;

    /**
     * Keys for the saved state
     */
    private static final String STATE_PHOTOS = "photos";
    private static final String STATE_CAPTURE_URI = "capture_uri";
    private static final String STATE_LOADING_URI = "loading_uri";

    /**
     * Whether the external storage is readable
     */
    private boolean mMediaReadable;

    /**
     * Whether the device has a camera
     */
    private boolean mHasCamera;

    /**
     * The image file currently being captured
     */
    @Nullable
    private Uri mCapturedPhoto;

    /**
     * The currently running photo loader
     */
    @Nullable
    private PhotoLoader mPhotoLoader;

    /**
     * The information about each photo
     */
    @NonNull
    private ArrayList<PhotoHolder> mPhotos = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaReadable = Environment.getExternalStorageDirectory().canRead();
        if(!mMediaReadable) {
            return;
        }
        setHasOptionsMenu(true);

        mHasCamera = getContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA);

        if(savedInstanceState != null) {
            final ArrayList<PhotoHolder> photos =
                    savedInstanceState.getParcelableArrayList(STATE_PHOTOS);
            if(photos != null) {
                mPhotos = photos;
            }
            mCapturedPhoto = savedInstanceState.getParcelable(STATE_CAPTURE_URI);
            final Uri loadingUri = savedInstanceState.getParcelable(STATE_LOADING_URI);
            if(loadingUri != null) {
                mPhotoLoader = new PhotoLoader(loadingUri);
                mPhotoLoader.execute();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if(!mMediaReadable) {
            final View root = inflater.inflate(R.layout.no_media, container, false);

            if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return root;
            }

            if(!PermissionUtils.hasExternalStoragePerm(getContext())
                    && PermissionUtils.shouldAskExternalStoragePerm(getActivity())) {
                final Button permButton = root.findViewById(R.id.button_grant_permission);
                permButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PermissionUtils.requestExternalStoragePerm(getActivity());
                    }
                });
                permButton.setVisibility(View.VISIBLE);
            }

            return root;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_PHOTOS, mPhotos);
        outState.putParcelable(STATE_CAPTURE_URI, mCapturedPhoto);
        if(mPhotoLoader != null) {
            outState.putParcelable(STATE_LOADING_URI, mPhotoLoader.getUri());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPhotoLoader != null) {
            mPhotoLoader.cancel(true);
            mPhotoLoader = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_take_photo:
                takePhoto();
                return true;
            case R.id.menu_select_photo:
                addPhotoFromGallery();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            final ContentResolver cr = getContext().getContentResolver();
            Uri uri = null;
            switch(requestCode) {
                case REQUEST_CAPTURE_IMAGE:
                    try {
                        uri = mCapturedPhoto;
                        if(uri != null) {
                            MediaStore.Images.Media.insertImage(cr, uri.getPath(),
                                    uri.getLastPathSegment(), null);
                        }
                    } catch(FileNotFoundException e) {
                        Log.e(TAG, "Failed to save file", e);
                    }
                    break;
                case REQUEST_SELECT_IMAGE:
                    if(data != null) {
                        uri = data.getData();
                    }
                    break;
            }

            if(uri != null) {
                mPhotoLoader = new PhotoLoader(uri);
                mPhotoLoader.execute();
            }
        }
    }

    /**
     * Get the photo data.
     *
     * @return The list of photos
     */
    @NonNull
    final ArrayList<PhotoHolder> getPhotos() {
        return mPhotos;
    }

    /**
     * Does the device have a camera?
     *
     * @return Whether the device has a camera
     */
    final boolean hasCamera() {
        return mHasCamera;
    }

    /**
     * Is the external storage available?
     *
     * @return Whether the external storage is mounted
     */
    final boolean isMediaReadable() {
        return mMediaReadable;
    }

    /**
     * Launch an image capturing Intent.
     */
    final void takePhoto() {
        final Intent intent = PhotoUtils.getTakePhotoIntent(getContext());
        if(intent != null && intent.resolveActivity(getContext().getPackageManager()) != null) {
            mCapturedPhoto = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            getParentFragment().startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
            return;
        }
        Toast.makeText(getContext(), R.string.error_camera, Toast.LENGTH_LONG).show();
    }

    /**
     * Launch an image selection Intent.
     */
    final void addPhotoFromGallery() {
        final Intent intent = PhotoUtils.getSelectPhotoIntent();
        if(intent.resolveActivity(getContext().getPackageManager()) != null) {
            getParentFragment().startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        }
    }

    /**
     * Add a photo to this entry.
     *
     * @param newPhoto The photo being added
     */
    private void addPhoto(@NonNull PhotoHolder newPhoto) {
        mPhotoLoader = null;
        if(newPhoto.hash == null) {
            Toast.makeText(getContext(), R.string.error_insert_photo, Toast.LENGTH_LONG).show();
            return;
        }

        for(PhotoHolder photo : mPhotos) {
            if(newPhoto.hash.equals(photo.hash)) {
                Toast.makeText(getContext(), R.string.message_duplicate_photo, Toast.LENGTH_LONG)
                        .show();
                return;
            }
            if(photo.pos >= newPhoto.pos) {
                newPhoto.pos = photo.pos + 1;
            }
        }

        mPhotos.add(newPhoto);
        onPhotoAdded(newPhoto);
    }

    /**
     * Called when a photo is added.
     *
     * @param photo The new photo
     */
    protected abstract void onPhotoAdded(@NonNull PhotoHolder photo);

    /**
     * Remove the photo at the specified position.
     *
     * @param position The position index of the photo
     */
    final void removePhoto(int position) {
        if(position < 0 || position >= mPhotos.size()) {
            return;
        }

        final PhotoHolder photo = mPhotos.remove(position);
        if(photo != null) {
            onPhotoRemoved(photo);
        }
    }

    /**
     * Called when a photo is removed.
     *
     * @param photo The removed photo
     */
    protected abstract void onPhotoRemoved(@NonNull PhotoHolder photo);

    /**
     * Task for loading new photos in the background.
     */
    private class PhotoLoader extends AsyncTask<Void, Void, PhotoHolder> {
        /**
         * The Uri to load
         */
        @NonNull
        private final Uri mUri;

        /**
         * @param uri The Uri to load
         */
        PhotoLoader(@NonNull Uri uri) {
            mUri = uri;
        }

        /**
         * Get the Uri being loaded
         *
         * @return The Uri being loaded
         */
        @NonNull
        public Uri getUri() {
            return mUri;
        }

        @Override
        protected PhotoHolder doInBackground(Void... params) {
            final String hash = PhotoUtils.getMD5Hash(getContext().getContentResolver(), mUri);
            return new PhotoHolder(0, hash, mUri, 0);
        }

        @Override
        protected void onPostExecute(PhotoHolder holder) {
            if(isCancelled()) {
                return;
            }
            addPhoto(holder);
        }
    }
}
