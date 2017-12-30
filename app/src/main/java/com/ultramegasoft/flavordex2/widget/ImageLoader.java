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
package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.ultramegasoft.flavordex2.util.BitmapCache;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.lang.ref.WeakReference;

/**
 * Task for loading images in the background.
 *
 * @author Steve Guidetti
 */
public class ImageLoader extends AsyncTask<Void, Void, Bitmap> {
    /**
     * Reference to the ImageView
     */
    @NonNull
    private final WeakReference<ImageView> mImageViewReference;

    /**
     * The width of the container
     */
    private final int mWidth;

    /**
     * The height of the container
     */
    private final int mHeight;

    /**
     * The Uri to the photo file to load from disk
     */
    @NonNull
    private final Uri mUri;

    /**
     * The cache to store loaded Bitmaps
     */
    @Nullable
    private final BitmapCache mCache;

    /**
     * The Context reference
     */
    @NonNull
    private final WeakReference<Context> mContext;

    /**
     * @param imageView The ImageView to hold the image
     * @param width     The width of the container
     * @param height    The height of the container
     * @param uri       The Uri to the photo file to load from disk
     * @param cache     The cache to store loaded Bitmaps
     */
    public ImageLoader(@NonNull ImageView imageView, int width, int height, @NonNull Uri uri,
                       @Nullable BitmapCache cache) {
        mImageViewReference = new WeakReference<>(imageView);
        mWidth = width;
        mHeight = height;
        mUri = uri;
        mCache = cache;
        mContext = new WeakReference<>(imageView.getContext().getApplicationContext());
    }

    @Override
    protected Bitmap doInBackground(Void... args) {
        final Context context = mContext.get();
        if(context == null) {
            return null;
        }

        final Bitmap bitmap = PhotoUtils.loadBitmap(context, mUri, mWidth, mHeight);
        if(mCache != null && bitmap != null) {
            mCache.put(mUri, bitmap);
        }

        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);

        if(result != null) {
            final ImageView imageView = mImageViewReference.get();
            if(imageView != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
