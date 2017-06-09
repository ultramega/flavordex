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

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads thumbnails in a background thread.
 *
 * @author Steve Guidetti
 */
abstract class BackgroundThumbLoader<K> {
    /**
     * Maximum number of concurrent threads
     */
    private final int NUM_THREADS = 5;

    /**
     * Handler for communicating with the main thread
     */
    private final Handler mHandler = new Handler();

    /**
     * Thread pool to handle loading thumbnails
     */
    private final ExecutorService mPool = Executors.newFixedThreadPool(NUM_THREADS);

    /**
     * Start loading an image into aa ImageView.
     *
     * @param imageView The ImageView to hold the image
     * @param key       The key to reference the image in the cache
     */
    public void load(@NonNull ImageView imageView, @NonNull K key) {
        final Bitmap bitmap = PhotoUtils.getThumbCache().get(key);
        if(bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            final Runnable task = new LoadTask(new Thumb(key, imageView));
            mPool.execute(task);
        }
    }

    /**
     * Load a Bitmap from the source.
     *
     * @param thumb The Thumb object referencing the thumbnail
     * @return A Bitmap
     */
    @Nullable
    protected abstract Bitmap getBitmap(@NonNull Thumb thumb);

    /**
     * Set the Bitmap of an ImageView on the UI thread.
     *
     * @param imageView The ImageView
     * @param bitmap    The Bitmap
     */
    private void setBitmap(@NonNull final ImageView imageView, @Nullable final Bitmap bitmap) {
        mHandler.post(new Runnable() {
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    /**
     * A task for loading Bitmaps in the background.
     */
    private class LoadTask implements Runnable {
        /**
         * The Thumb object to load
         */
        @NonNull
        private final Thumb mThumb;

        /**
         * @param thumb The thumbnail
         */
        LoadTask(@NonNull Thumb thumb) {
            mThumb = thumb;
        }

        public void run() {
            final ImageView imageView = mThumb.get();
            if(imageView != null) {
                final Drawable drawable = imageView.getDrawable();
                if(drawable instanceof ThumbDrawable
                        && ((ThumbDrawable)drawable).key == mThumb.key) {
                    final Bitmap bitmap = getBitmap(mThumb);
                    if(bitmap != null) {
                        PhotoUtils.getThumbCache().put(mThumb.key, bitmap);
                        setBitmap(imageView, bitmap);
                    }
                }
            }
        }
    }

    /**
     * An object to store a reference to an ImageView while its Bitmap is loading.
     */
    public static class Thumb {
        /**
         * The key to reference this thumbnail
         */
        @NonNull
        public final Object key;

        /**
         * A reference to the ImageView
         */
        @NonNull
        private final WeakReference<ImageView> mReference;

        /**
         * @param key       The key to reference this thumbnail
         * @param imageView The ImageView
         */
        Thumb(@NonNull Object key, @NonNull ImageView imageView) {
            this.key = key;
            imageView.setImageDrawable(new ThumbDrawable(key));
            mReference = new WeakReference<>(imageView);
        }

        /**
         * Get the ImageView.
         *
         * @return The ImageView
         */
        @Nullable
        public ImageView get() {
            return mReference.get();
        }
    }

    /**
     * A dummy Drawable to serve as a placeholder
     */
    private static class ThumbDrawable extends BitmapDrawable {
        /**
         * The key to reference this thumbnail
         */
        @NonNull
        public final Object key;

        /**
         * @param key The key to reference this thumbnail
         */
        @SuppressWarnings("deprecation")
        ThumbDrawable(@NonNull Object key) {
            this.key = key;
        }
    }
}
