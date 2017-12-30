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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @NonNull
    private BitmapCache mCache = new BitmapCache();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!isMediaReadable()) {
            return;
        }
        if(savedInstanceState != null) {
            final BitmapCache cache = savedInstanceState.getParcelable(STATE_CACHE);
            if(cache != null) {
                mCache = cache;
            }
        }
        mAdapter = new ImageAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
            entry.addPhoto(photo.id, photo.hash, photo.uri);
        }
    }

    @Override
    protected void onPhotoAdded(@NonNull PhotoHolder photo) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPhotoRemoved(@NonNull PhotoHolder photo) {
        mCache.remove(photo.uri);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Custom Adapter for loading images with remove buttons into the GridView.
     */
    private class ImageAdapter extends BaseAdapter implements PopupMenu.OnMenuItemClickListener {
        /**
         * View types
         */
        static final int NORMAL_VIEW_TYPE = 0;
        static final int ADD_BUTTON_VIEW_TYPE = 1;

        /**
         * Empty PhotoHolder to serve as a placeholder for the add button
         */
        private final PhotoHolder mPlaceholder = new PhotoHolder(0, null, Uri.EMPTY, -1);

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

        ImageAdapter() {
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
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public int getItemViewType(int position) {
            return mData.get(position) == mPlaceholder ? ADD_BUTTON_VIEW_TYPE : NORMAL_VIEW_TYPE;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final Uri uri = mData.get(position).uri;

            if(uri == Uri.EMPTY) {
                return getAddLayout(parent);
            }

            if(convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.photo_grid_item, parent, false);

                final Holder holder = new Holder();
                holder.image = convertView.findViewById(R.id.image);
                holder.removeButton = convertView.findViewById(R.id.button_remove_photo);
                convertView.setTag(holder);
            }

            final Holder holder = (Holder)convertView.getTag();
            loadImage(holder.image, uri);

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
         * @param uri  The Uri to the photo to load
         */
        private void loadImage(ImageView view, Uri uri) {
            final Bitmap bitmap = mCache.get(uri);
            view.setImageBitmap(bitmap);
            if(bitmap == null) {
                new ImageLoader(view, mFrameSize, mFrameSize, uri, mCache).execute();
            }
        }
    }

    /**
     * Holder for View references
     */
    private class Holder {
        ImageView image;
        View removeButton;
    }
}
