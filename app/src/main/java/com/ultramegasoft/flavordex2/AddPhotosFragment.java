package com.ultramegasoft.flavordex2;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.BitmapCache;
import com.ultramegasoft.flavordex2.widget.ImageLoader;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Fragment for adding photos to a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddPhotosFragment extends AbsPhotosFragment {
    /**
     * The Adapter backing the GridView
     */
    private ImageAdapter mAdapter;

    /**
     * Memory cache for Bitmaps
     */
    private final BitmapCache mCache = new BitmapCache();

    public AddPhotosFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!isMediaMounted()) {
            return;
        }
        mAdapter = new ImageAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(!isMediaMounted()) {
            return inflater.inflate(R.layout.no_media, container, false);
        }

        final View root = inflater.inflate(R.layout.fragment_add_photos, container, false);
        ((GridView)root).setAdapter(mAdapter);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.add_photos_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_take_photo).setEnabled(hasCamera());
    }

    /**
     * Get the photos data as an array of ContentValues objects ready to be bulk inserted into the
     * photos database table.
     *
     * @return Array of ContentValues containing the data for the photos table
     */
    public ContentValues[] getData() {
        final ArrayList<ContentValues> values = new ArrayList<>(getPhotos().size());

        ContentValues rowValues;
        for(PhotoHolder photo : getPhotos()) {
            rowValues = new ContentValues();
            rowValues.put(Tables.Photos.PATH, photo.path);

            values.add(rowValues);
        }

        return values.toArray(new ContentValues[values.size()]);
    }

    @Override
    protected void onPhotoAdded(PhotoHolder photo) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPhotoRemoved(PhotoHolder photo) {
        mCache.remove(photo.path);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Custom Adapter for loading images with remove buttons into the GridView.
     */
    private class ImageAdapter extends BaseAdapter {
        /**
         * The data backing the Adapter
         */
        private final ArrayList<PhotoHolder> mData;

        /**
         * List of reusable Views
         */
        private final HashMap<String, View> mViews = new HashMap<>();

        /**
         * The size of the cell containing the image
         */
        private final int mFrameSize;

        public ImageAdapter() {
            mData = getPhotos();
            mFrameSize = getResources().getDimensionPixelSize(R.dimen.photo_grid_size);
        }

        @Override
        public void notifyDataSetChanged() {
            mViews.clear();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mData.get(position).id;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.photo_grid_item, parent, false);
            }

            final String path = mData.get(position).path;

            if(mViews.containsKey(path)) {
                return mViews.get(path);
            }

            final ImageView imageView = (ImageView)convertView.findViewById(R.id.image);
            loadImage(imageView, path);

            convertView.findViewById(R.id.button_remove_photo)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            removePhoto(position);
                        }
                    });

            mViews.put(path, convertView);
            return convertView;
        }

        /**
         * Load a Bitmap in the background.
         *
         * @param view The ImageView to hold the Bitmap
         * @param path The path to the photo to load
         */
        private void loadImage(ImageView view, String path) {
            final Bitmap bitmap = mCache.get(path);
            if(bitmap != null) {
                view.setImageBitmap(bitmap);
            } else {
                new ImageLoader(view, mFrameSize, mFrameSize, path, mCache).execute();
            }
        }
    }
}
