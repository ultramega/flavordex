package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.LinearLayout;

import com.ultramegasoft.flavordex2.provider.Tables;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
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

        if(mData.size() > 0) {
            notifyDataChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(!mMediaMounted) {
            return inflater.inflate(R.layout.no_media, container, false);
        }

        final View root = inflater.inflate(R.layout.fragment_entry_photos, container, false);

        mPager = (ViewPager)root.findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(new PagerAdapter());

        return root;
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.photo_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_delete_photo:
                deletePhoto();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO: 8/19/2015 Handle results of external activities
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
    private void deletePhoto() {
        // TODO: 8/19/2015 Open delete confirmation dialog for the shown image
    }

    /**
     * Called whenever the list of photos might have been changed. This notifies the pager's
     * adapter
     * and the action bar.
     */
    private void notifyDataChanged() {
        if(mPager != null) {
            mPager.getAdapter().notifyDataSetChanged();
            if(mData.size() > 0) {
                registerForContextMenu(mPager);
            } else {
                unregisterForContextMenu(mPager);
            }
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
        } else {
            showNoDataLayout();
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
    private class PagerAdapter extends FragmentPagerAdapter {

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
    }
}
