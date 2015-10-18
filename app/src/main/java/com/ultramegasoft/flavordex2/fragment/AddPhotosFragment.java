package com.ultramegasoft.flavordex2.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.BitmapCache;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ImageLoader;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;

import java.util.ArrayList;

/**
 * Fragment for adding photos to a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddPhotosFragment extends AbsPhotosFragment {
    /**
     * Keys for the saved state
     */
    private static final String STATE_CACHE = "cache";

    /**
     * The Adapter backing the GridView
     */
    private ImageAdapter mAdapter;

    /**
     * Memory cache for Bitmaps
     */
    private BitmapCache mCache = new BitmapCache();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!isMediaReadable()) {
            return;
        }
        if(savedInstanceState != null) {
            mCache = savedInstanceState.getParcelable(STATE_CACHE);
        }
        mAdapter = new ImageAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(!isMediaReadable()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        final View root = inflater.inflate(R.layout.fragment_add_photos, container, false);
        ((GridView)root).setAdapter(mAdapter);
        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_CACHE, mCache);
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
     * Load the photos into the entry.
     *
     * @param entry The entry
     */
    public void getData(EntryHolder entry) {
        for(PhotoHolder photo : getPhotos()) {
            entry.addPhoto(photo.id, photo.path);
        }
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
    private class ImageAdapter extends BaseAdapter implements PopupMenu.OnMenuItemClickListener {
        /**
         * View types
         */
        public static final int NORMAL_VIEW_TYPE = 0;
        public static final int ADD_BUTTON_VIEW_TYPE = 1;

        /**
         * Empty PhotoHolder to serve as a placeholder for the add button
         */
        private final PhotoHolder mPlaceholder = new PhotoHolder(null);

        /**
         * The data backing the Adapter
         */
        private final ArrayList<PhotoHolder> mData;

        /**
         * The layout for the add button
         */
        private RelativeLayout mAddLayout;

        /**
         * The size of the cell containing the image
         */
        private final int mFrameSize;

        public ImageAdapter() {
            mData = new ArrayList<>(getPhotos());
            mData.add(mPlaceholder);
            mFrameSize = getResources().getDimensionPixelSize(R.dimen.photo_grid_size);
        }

        @Override
        public void notifyDataSetChanged() {
            mData.clear();
            mData.addAll(getPhotos());
            mData.add(mPlaceholder);

            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public PhotoHolder getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mData.get(position).id;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return mData.get(position) == mPlaceholder ? ADD_BUTTON_VIEW_TYPE : NORMAL_VIEW_TYPE;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final String path = mData.get(position).path;

            if(path == null) {
                return getAddLayout(parent);
            }

            if(convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.photo_grid_item, parent, false);

                final Holder holder = new Holder();
                holder.image = (ImageView)convertView.findViewById(R.id.image);
                holder.removeButton = convertView.findViewById(R.id.button_remove_photo);
                convertView.setTag(holder);
            }

            final Holder holder = (Holder)convertView.getTag();
            loadImage(holder.image, path);

            holder.removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removePhoto(position);
                }
            });

            return convertView;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return onOptionsItemSelected(item);
        }

        /**
         * Get the layout for the add button.
         *
         * @param parent The parent for this View
         * @return The add button layout
         */
        private RelativeLayout getAddLayout(ViewGroup parent) {
            if(mAddLayout == null) {
                mAddLayout = (RelativeLayout)LayoutInflater.from(getContext())
                        .inflate(R.layout.photo_add_grid_item, parent, false);
                mAddLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(hasCamera()) {
                            openAddMenu(mAddLayout.findViewById(R.id.button_add_photo));
                        } else {
                            addPhotoFromGallery();
                        }
                    }
                });
            }
            return mAddLayout;
        }

        /**
         * Open the add photo PopupMenu.
         *
         * @param anchor The View to anchor to
         */
        private void openAddMenu(View anchor) {
            final PopupMenu popupMenu = new PopupMenu(getContext(), anchor);
            popupMenu.inflate(R.menu.add_photos_menu);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.getMenu().findItem(R.id.menu_take_photo).setEnabled(hasCamera());
            popupMenu.show();
        }

        /**
         * Load a Bitmap in the background.
         *
         * @param view The ImageView to hold the Bitmap
         * @param path The path to the photo to load
         */
        private void loadImage(ImageView view, String path) {
            final Bitmap bitmap = mCache.get(path);
            view.setImageBitmap(bitmap);
            if(bitmap == null) {
                new ImageLoader(view, mFrameSize, mFrameSize, path, mCache).execute();
            }
        }
    }

    /**
     * Holder for View references
     */
    private class Holder {
        public ImageView image;
        public View removeButton;
    }
}
