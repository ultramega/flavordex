package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.util.PermissionUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Abstract class for shared photo Fragment functionality.
 *
 * @author Steve Guidetti
 */
public abstract class AbsPhotosFragment extends Fragment {
    /**
     * Keys for the saved state
     */
    private static final String STATE_PHOTOS = "photos";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final int REQUEST_SELECT_IMAGE = 200;

    /**
     * Whether the external storage is mounted
     */
    private boolean mMediaMounted;

    /**
     * Whether the device has a camera
     */
    private boolean mHasCamera;

    /**
     * Uri to the image currently being captured
     */
    private Uri mCapturedPhoto;

    /**
     * The information about each photo
     */
    private ArrayList<PhotoHolder> mPhotos = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaMounted = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        if(!mMediaMounted) {
            return;
        }
        setHasOptionsMenu(true);

        mHasCamera = getContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA);

        if(savedInstanceState != null) {
            mPhotos = savedInstanceState.getParcelableArrayList(STATE_PHOTOS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_PHOTOS, mPhotos);
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
            switch(requestCode) {
                case REQUEST_CAPTURE_IMAGE:
                    final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(mCapturedPhoto);
                    getContext().sendBroadcast(scanIntent);

                    addPhoto(mCapturedPhoto);
                    break;
                case REQUEST_SELECT_IMAGE:
                    if(data != null) {
                        addPhoto(data.getData());
                    }
                    break;
            }
        }
    }

    /**
     * Get the photo data.
     *
     * @return The list of photos
     */
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
    final boolean isMediaMounted() {
        return mMediaMounted;
    }

    /**
     * Launch an image capturing Intent.
     */
    final void takePhoto() {
        if(!PermissionUtils.checkExternalStoragePerm(getActivity(),
                R.string.message_request_storage_camera)) {
            return;
        }
        try {
            mCapturedPhoto = PhotoUtils.getOutputMediaUri();
            final Intent intent = PhotoUtils.getTakePhotoIntent(mCapturedPhoto);
            if(intent.resolveActivity(getContext().getPackageManager()) != null) {
                getParentFragment().startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
            }
        } catch(IOException e) {
            Toast.makeText(getContext(), R.string.error_camera, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Launch an image selection Intent.
     */
    final void addPhotoFromGallery() {
        final Intent intent = PhotoUtils.getSelectPhotoIntent();
        getParentFragment().startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    /**
     * Add a photo to this entry.
     *
     * @param uri The Uri to the image file
     */
    private void addPhoto(Uri uri) {
        if(uri == null) {
            return;
        }

        final String path = PhotoUtils.getPath(getContext(), uri);
        if(path == null) {
            return;
        }

        for(PhotoHolder photo : mPhotos) {
            if(path.equals(photo.path)) {
                return;
            }
        }

        final PhotoHolder photo = new PhotoHolder(path);
        mPhotos.add(photo);
        onPhotoAdded(photo);
    }

    /**
     * Called when a photo is added.
     *
     * @param photo The new photo
     */
    protected abstract void onPhotoAdded(PhotoHolder photo);

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
    protected abstract void onPhotoRemoved(PhotoHolder photo);
}
