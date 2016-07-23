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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

/**
 * Fragment for displaying a single photo.
 *
 * @author Steve Guidetti
 */
public class PhotoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Bitmap>,
        PopupMenu.OnMenuItemClickListener {
    /**
     * Argument for the Uri to the image file
     */
    public static final String ARG_URI = "uri";

    /**
     * The Uri to the image file
     */
    private Uri mUri;

    /**
     * Views from the layout
     */
    private View mRootView;
    private ProgressBar mProgressBar;
    private ImageView mImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mUri = getArguments().getParcelable(ARG_URI);
        if(mUri == null) {
            return null;
        }

        mRootView = inflater.inflate(R.layout.fragment_photo, container, false);
        mProgressBar = (ProgressBar)mRootView.findViewById(R.id.progress);
        mImageView = (ImageView)mRootView.findViewById(R.id.image);

        mImageView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final int width = mImageView.getWidth();
                        if(width > 0) {
                            loadPhoto();
                            //noinspection deprecation
                            mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });

        return mRootView;
    }

    /**
     * Load the image file.
     */
    private void loadPhoto() {
        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    /**
     * Show the loaded image.
     *
     * @param bitmap The Bitmap to show
     */
    private void showPhoto(Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);

        mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showMenu(mRootView.findViewById(R.id.anchor));
                return true;
            }
        });
    }

    /**
     * Show the missing photo layout.
     */
    private void showNoPhoto() {
        final View root = ((ViewStub)mRootView.findViewById(R.id.photo_not_found)).inflate();
        ((TextView)root.findViewById(R.id.file_name)).setText(mUri.toString());
        root.findViewById(R.id.button_locate_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ViewPhotosFragment fragment = (ViewPhotosFragment)getParentFragment();
                if(fragment != null) {
                    fragment.locatePhoto();
                }
            }
        });
        root.findViewById(R.id.button_remove_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ViewPhotosFragment fragment = (ViewPhotosFragment)getParentFragment();
                if(fragment != null) {
                    fragment.deletePhoto();
                }
            }
        });
    }

    /**
     * Show the PopupMenu for the photo.
     *
     * @param v The View to attach the menu to
     */
    private void showMenu(View v) {
        final PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.photo_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_remove_photo:
                final ViewPhotosFragment target = (ViewPhotosFragment)getParentFragment();
                if(target != null) {
                    target.confirmDeletePhoto();
                }
                return true;
        }
        return false;
    }

    @Override
    public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
        return new PhotoLoader(getContext(), mUri, mImageView.getWidth(), mImageView.getHeight());
    }

    @Override
    public void onLoadFinished(Loader<Bitmap> loader, Bitmap data) {
        mProgressBar.setVisibility(View.INVISIBLE);
        if(data != null) {
            showPhoto(data);
        } else {
            showNoPhoto();
        }
    }

    @Override
    public void onLoaderReset(Loader<Bitmap> loader) {
    }

    /**
     * Custom Loader to load a Bitmap.
     */
    private static class PhotoLoader extends AsyncTaskLoader<Bitmap> {
        /**
         * The Uri to the image to load
         */
        private final Uri mUri;

        /**
         * The container width
         */
        private final int mWidth;

        /**
         * The container height
         */
        private final int mHeight;

        /**
         * @param context The Context
         * @param uri     The Uri to the image to load
         * @param width   The container width
         * @param height  The container height
         */
        public PhotoLoader(Context context, Uri uri, int width, int height) {
            super(context);
            mUri = uri;
            mWidth = width;
            mHeight = height;
        }

        @Override
        public Bitmap loadInBackground() {
            return PhotoUtils.loadBitmap(getContext(), mUri, mWidth, mHeight);
        }
    }
}
