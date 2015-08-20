package com.ultramegasoft.flavordex2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.ultramegasoft.flavordex2.dialog.ConfirmationDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.util.PhotoManager;

import java.io.File;
import java.util.ArrayList;

/**
 * Fragment to display the photos for a journal entry.
 *
 * @author Steve Guidetti
 */
public class EntryPhotosFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Keys for the saved state
     */
    private static final String STATE_PHOTOS = "photos";

    private static final String ARG_PHOTO_POSITION = "photo_position";

    /**
     * Request codes for external activities
     */
    private static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final int REQUEST_SELECT_IMAGE = 200;
    private static final int REQUEST_DELETE_IMAGE = 300;

    /**
     * The database id for this entry
     */
    private long mEntryId;

    /**
     * Whether the external storage is mounted
     */
    private boolean mMediaMounted;

    /**
     * Whether the device has a camera
     */
    private boolean mHasCamera;

    /**
     * Views for this fragment
     */
    private ViewPager mPager;
    private LinearLayout mNoDataLayout;
    private ProgressBar mProgressBar;

    /**
     * The information about each photo
     */
    private ArrayList<PhotoHolder> mData = new ArrayList<>();

    public EntryPhotosFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntryId = getArguments().getLong(EntryDetailFragment.ARG_ITEM_ID);
        mMediaMounted = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(!mMediaMounted) {
            return;
        }

        mHasCamera = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

        if(savedInstanceState != null) {
            mData = savedInstanceState.getParcelableArrayList(STATE_PHOTOS);
        } else {
            getLoaderManager().initLoader(0, null, this);
        }

        if(!mData.isEmpty()) {
            notifyDataChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(!mMediaMounted) {
            return inflater.inflate(R.layout.no_media, container, false);
        }

        final View root = inflater.inflate(R.layout.fragment_entry_photos, container, false);

        mProgressBar = (ProgressBar)root.findViewById(R.id.progress);

        mPager = (ViewPager)root.findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(new PagerAdapter());

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPager = null;
        mNoDataLayout = null;
        mProgressBar = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_PHOTOS, mData);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.photos_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean showAdd = mMediaMounted;
        menu.findItem(R.id.menu_add_photo).setEnabled(showAdd).setVisible(showAdd);

        final boolean showTake = showAdd && mHasCamera;
        menu.findItem(R.id.menu_take_photo).setEnabled(showTake).setVisible(showTake);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_take_photo:
                takePhoto();
                return true;
            case R.id.menu_add_photo:
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
                    break;
                case REQUEST_SELECT_IMAGE:
                    break;
                case REQUEST_DELETE_IMAGE:
                    if(data != null) {
                        deletePhoto(data.getIntExtra(ARG_PHOTO_POSITION, -1));
                    }
                    break;
            }
        }
    }

    /**
     * Show the message that there are no photos for this entry along with buttons to add one.
     */
    private void showNoDataLayout() {
        final AppCompatActivity activity = (AppCompatActivity)getActivity();
        if(mNoDataLayout == null) {
            mNoDataLayout = (LinearLayout)((ViewStub)activity.findViewById(R.id.no_photos)).inflate();

            final Button btnTakePhoto = (Button)mNoDataLayout.findViewById(R.id.button_take_photo);
            if(mHasCamera) {
                btnTakePhoto.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        takePhoto();
                    }
                });
            } else {
                btnTakePhoto.setEnabled(false);
            }

            final Button btnAddPhoto = (Button)mNoDataLayout.findViewById(R.id.button_add_photo);
            btnAddPhoto.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    addPhotoFromGallery();
                }
            });
        }

        mNoDataLayout.setVisibility(View.VISIBLE);
        activity.getSupportActionBar().invalidateOptionsMenu();
    }

    /**
     * Launch an image capturing intent.
     */
    private void takePhoto() {
        // TODO: 8/19/2015 Launch camera activity
    }

    /**
     * Launch an image selection intent.
     */
    private void addPhotoFromGallery() {
        // TODO: 8/19/2015 Launch gallery activity
    }

    /**
     * Show a confirmation dialog to delete the shown image.
     */
    public void confirmDeletePhoto() {
        if(mData.isEmpty()) {
            return;
        }

        final int position = mPager.getCurrentItem();
        final PhotoHolder photo = mData.get(position);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        final int message;
        if(prefs.getBoolean(FlavordexApp.PREF_RETAIN_PHOTOS, false) || photo.fromGallery) {
            message = R.string.message_confirm_remove_photo;
        } else {
            message = R.string.message_confirm_delete_photo;
        }

        final Intent intent = new Intent();
        intent.putExtra(ARG_PHOTO_POSITION, position);

        ConfirmationDialog.showDialog(getFragmentManager(), this, REQUEST_DELETE_IMAGE,
                getString(R.string.menu_delete_photo), getString(message), intent);
    }

    /**
     * Delete the photo at the specified position.
     *
     * @param position The position index of the photo
     */
    private void deletePhoto(int position) {
        if(position < 0 || position >= mData.size()) {
            return;
        }

        final PhotoHolder photo = mData.get(position);

        EntryUtils.deletePhoto(getActivity(), photo.id);
        mData.remove(position);
        notifyDataChanged();

        if(position == 0) {
            updatePosterPhoto();
        }
    }

    /**
     * Update the main photo to use as this entry's thumbnail.
     */
    private void updatePosterPhoto() {
        final ContentResolver cr = getActivity().getContentResolver();
        if(!mData.isEmpty()) {
            new Handler().post(new Runnable() {
                public void run() {
                    PhotoManager.generateThumb(getActivity(), mData.get(0).path, mEntryId);
                }
            });
        } else {
            PhotoManager.deleteThumb(getActivity(), mEntryId);
        }
        cr.notifyChange(Tables.Entries.CONTENT_URI, null);
    }

    /**
     * Called whenever the list of photos might have been changed. This notifies the pager's adapter
     * and the action bar.
     */
    private void notifyDataChanged() {
        if(mPager != null) {
            mPager.getAdapter().notifyDataSetChanged();
        }

        if(!mData.isEmpty()) {
            if(mNoDataLayout != null) {
                mNoDataLayout.setVisibility(View.GONE);
            }
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            showNoDataLayout();
        }

        ((AppCompatActivity)getActivity()).getSupportActionBar().invalidateOptionsMenu();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId + "/photos");
        final String[] projection = new String[] {
                Tables.Photos._ID,
                Tables.Photos.PATH,
                Tables.Photos.FROM_GALLERY
        };
        return new CursorLoader(getActivity(), uri, projection, null, null, Tables.Photos._ID + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mData.clear();
        if(data.getCount() > 0) {
            while(data.moveToNext()) {
                final String path = data.getString(1);
                if(new File(path).exists()) {
                    mData.add(new PhotoHolder(data.getLong(0), path, data.getInt(2) == 1));
                }
            }
        }

        notifyDataChanged();

        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Adapter for the ViewPager
     */
    private class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter() {
            super(getChildFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            final Bundle args = new Bundle();
            args.putString(PhotoFragment.ARG_PATH, mData.get(position).path);
            return Fragment.instantiate(getActivity(), PhotoFragment.class.getName(), args);
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}
