package com.ultramegasoft.flavordex2.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.dialog.ConfirmationDialog;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BackendUtils;
import com.ultramegasoft.flavordex2.util.EntryUtils;
import com.ultramegasoft.flavordex2.util.PhotoUtils;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;

import java.util.ArrayList;

/**
 * Fragment to display the photos for a journal entry.
 *
 * @author Steve Guidetti
 */
public class ViewPhotosFragment extends AbsPhotosFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Argument for the photo removal confirmation dialog
     */
    private static final String ARG_PHOTO_POSITION = "photo_position";

    /**
     * Request codes for external Activities
     */
    private static final int REQUEST_DELETE_IMAGE = 700;

    /**
     * The database ID for this entry
     */
    private long mEntryId;

    /**
     * Views for this Fragment
     */
    private ViewPager mPager;
    private LinearLayout mNoDataLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntryId = getArguments().getLong(ViewEntryFragment.ARG_ENTRY_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(!isMediaReadable()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        final View root = inflater.inflate(R.layout.fragment_entry_photos, container, false);

        mPager = (ViewPager)root.findViewById(R.id.pager);
        mPager.setAdapter(new PagerAdapter());

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(!isMediaReadable()) {
            return;
        }
        if(getPhotos().isEmpty()) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            notifyDataChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPager = null;
        mNoDataLayout = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setHasOptionsMenu(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.view_photos_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean showAdd = isMediaReadable();
        menu.findItem(R.id.menu_add_photo).setEnabled(showAdd).setVisible(showAdd);
        menu.findItem(R.id.menu_select_photo).setEnabled(showAdd);

        final boolean showTake = showAdd && hasCamera();
        menu.findItem(R.id.menu_take_photo).setEnabled(showTake);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_DELETE_IMAGE:
                    if(data != null) {
                        removePhoto(data.getIntExtra(ARG_PHOTO_POSITION, -1));
                    }
                    break;
            }
        }
    }

    @Override
    protected void onPhotoAdded(PhotoHolder photo) {
        notifyDataChanged();
        mPager.setCurrentItem(getPhotos().size() - 1, true);

        new PhotoSaver(getContext(), mEntryId, photo).execute();
    }

    @Override
    protected void onPhotoRemoved(PhotoHolder photo) {
        notifyDataChanged();
        new PhotoDeleter(getContext(), mEntryId, photo).execute();
    }

    /**
     * Show the message that there are no photos for this entry along with buttons to add one.
     */
    private void showNoDataLayout() {
        final AppCompatActivity activity = (AppCompatActivity)getActivity();
        if(mNoDataLayout == null) {
            mNoDataLayout =
                    (LinearLayout)((ViewStub)activity.findViewById(R.id.no_photos)).inflate();

            final Button btnTakePhoto = (Button)mNoDataLayout.findViewById(R.id.button_take_photo);
            if(hasCamera()) {
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
        final ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null) {
            actionBar.invalidateOptionsMenu();
        }
    }

    /**
     * Show a confirmation dialog to delete the shown image.
     */
    public void confirmDeletePhoto() {
        if(getPhotos().isEmpty()) {
            return;
        }

        final int position = mPager.getCurrentItem();

        final Intent intent = new Intent();
        intent.putExtra(ARG_PHOTO_POSITION, position);

        ConfirmationDialog.showDialog(getFragmentManager(), this, REQUEST_DELETE_IMAGE,
                getString(R.string.title_remove_photo),
                getString(R.string.message_confirm_remove_photo), R.drawable.ic_delete, intent);
    }

    /**
     * Called whenever the list of photos might have been changed. This notifies the ViewPager's
     * Adapter and the ActionBar.
     */
    private void notifyDataChanged() {
        if(mPager != null) {
            mPager.getAdapter().notifyDataSetChanged();
        }

        if(!getPhotos().isEmpty()) {
            if(mNoDataLayout != null) {
                mNoDataLayout.setVisibility(View.GONE);
            }
        } else {
            showNoDataLayout();
        }

        final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.invalidateOptionsMenu();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri =
                Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId + "/photos");
        final String[] projection = new String[] {
                Tables.Photos._ID,
                Tables.Photos.HASH,
                Tables.Photos.PATH,
                Tables.Photos.POS
        };
        final String where = Tables.Photos.PATH + " NOT NULL";
        final String order = Tables.Photos.POS + " ASC";
        return new CursorLoader(getContext(), uri, projection, where, null, order);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final ArrayList<PhotoHolder> photos = getPhotos();
        photos.clear();
        long id;
        String hash;
        String path;
        int pos;
        Uri uri;
        while(data.moveToNext()) {
            id = data.getLong(data.getColumnIndex(Tables.Photos._ID));
            hash = data.getString(data.getColumnIndex(Tables.Photos.HASH));
            path = data.getString(data.getColumnIndex(Tables.Photos.PATH));
            pos = data.getInt(data.getColumnIndex(Tables.Photos.POS));
            uri = PhotoUtils.parsePath(path);
            if(uri != null) {
                photos.add(new PhotoHolder(id, hash, uri, pos));
            }
        }

        notifyDataChanged();

        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Adapter for the ViewPager.
     */
    private class PagerAdapter extends FragmentStatePagerAdapter {
        /**
         * The data backing the Adapter
         */
        private final ArrayList<PhotoHolder> mData;

        public PagerAdapter() {
            super(getChildFragmentManager());
            mData = getPhotos();
        }

        @Override
        public Fragment getItem(int position) {
            final Bundle args = new Bundle();
            args.putParcelable(PhotoFragment.ARG_URI, mData.get(position).uri);
            return instantiate(getContext(), PhotoFragment.class.getName(), args);
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

    /**
     * Task to insert a photo into the database in the background.
     */
    private static class PhotoSaver extends AsyncTask<Void, Void, Boolean> {
        /**
         * The Context
         */
        private final Context mContext;

        /**
         * The entry ID to assign the photo to
         */
        private final long mEntryId;

        /**
         * The photo to save
         */
        private final PhotoHolder mPhoto;

        /**
         * @param context The Context
         * @param entryId The entry ID
         * @param photo   The photo to save
         */
        public PhotoSaver(Context context, long entryId, PhotoHolder photo) {
            mContext = context.getApplicationContext();
            mEntryId = entryId;
            mPhoto = photo;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final ContentResolver cr = mContext.getContentResolver();
            mPhoto.uri = PhotoUtils.getFileUri(cr, mPhoto.uri);
            if(mPhoto.uri == null) {
                return false;
            }
            Uri uri =
                    Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId + "/photos");

            final ContentValues values = new ContentValues();
            values.put(Tables.Photos.HASH, PhotoUtils.getMD5Hash(cr, mPhoto.uri));
            values.put(Tables.Photos.PATH, mPhoto.uri.getLastPathSegment());
            values.put(Tables.Photos.POS, mPhoto.pos);

            uri = cr.insert(uri, values);
            if(uri != null) {
                mPhoto.id = Long.valueOf(uri.getLastPathSegment());
                PhotoUtils.deleteThumb(mContext, mEntryId);
                EntryUtils.markChanged(cr, mEntryId);
                BackendUtils.requestDataSync(mContext);
                BackendUtils.requestPhotoSync(mContext);
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(!result) {
                Toast.makeText(mContext, R.string.error_insert_photo, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Task to delete a photo from the database in the background.
     */
    private static class PhotoDeleter extends AsyncTask<Void, Void, Void> {
        /**
         * The Context
         */
        private final Context mContext;

        /**
         * The entry ID the photo is assigned to
         */
        private final long mEntryId;

        /**
         * The photo to delete
         */
        private final PhotoHolder mPhoto;

        /**
         * @param context The Context
         * @param entryId The entry ID
         * @param photo   The photo to delete
         */
        public PhotoDeleter(Context context, long entryId, PhotoHolder photo) {
            mContext = context.getApplicationContext();
            mEntryId = entryId;
            mPhoto = photo;
        }

        @Override
        protected Void doInBackground(Void... params) {
            EntryUtils.deletePhoto(mContext, mPhoto.id);
            EntryUtils.markChanged(mContext.getContentResolver(), mEntryId);
            BackendUtils.requestDataSync(mContext);
            return null;
        }
    }
}
