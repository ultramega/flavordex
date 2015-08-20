package com.ultramegasoft.flavordex2.widget;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

import com.ultramegasoft.flavordex2.util.BitmapCache;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads thumbnails in a background thread.
 *
 * @author Steve Guidetti
 */
public abstract class BackgroundThumbLoader<K> {
    /**
     * Maximum number of concurrent threads
     */
    private final int NUM_THREADS = 5;

    /**
     * The memory cache for storing thumbnails
     */
    private final BitmapCache mBitmapCache = PhotoUtils.getThumbCache();

    private final Handler mHandler = new Handler();
    private final ExecutorService mPool = Executors.newFixedThreadPool(NUM_THREADS);

    /**
     * Start loading an image into a view.
     *
     * @param imageView The view to hold the image
     * @param key       The key to reference the image in the cache
     */
    public void load(ImageView imageView, K key) {
        final Bitmap bitmap = mBitmapCache.get(key);
        if(bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            final Runnable task = new LoadTask(new Thumb(key, imageView));
            mPool.execute(task);
        }
    }

    /**
     * Load a bitmap from the source.
     *
     * @param thumb The Thumb object referencing the thumbnail
     * @return A bitmap
     */
    protected abstract Bitmap getBitmap(Thumb thumb);

    /**
     * Set the bitmap of an ImageView on the UI thread.
     *
     * @param imageView The view
     * @param bitmap    The bitmap
     */
    private void setBitmap(final ImageView imageView, final Bitmap bitmap) {
        mHandler.post(new Runnable() {
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    /**
     * A task for loading bitmaps in the background.
     */
    private class LoadTask implements Runnable {
        private final Thumb mThumb;

        /**
         * @param thumb The thumbnail
         */
        public LoadTask(Thumb thumb) {
            mThumb = thumb;
        }

        public void run() {
            final ImageView imageView = mThumb.get();
            if(imageView != null) {
                final Drawable drawable = imageView.getDrawable();
                if(drawable instanceof ThumbDrawable
                        && ((ThumbDrawable)drawable).key == mThumb.key) {
                    final Bitmap bitmap = getBitmap(mThumb);
                    mBitmapCache.put(mThumb.key, bitmap);
                    setBitmap(imageView, bitmap);
                }
            }
        }
    }

    /**
     * An object to store a reference to an ImageView while its bitmap is loading.
     */
    public static class Thumb {
        public final Object key;
        private final WeakReference<ImageView> mReference;

        /**
         * @param key       The key to reference this thumbnail
         * @param imageView The view
         */
        public Thumb(Object key, ImageView imageView) {
            this.key = key;
            imageView.setImageDrawable(new ThumbDrawable(key));
            mReference = new WeakReference<>(imageView);
        }

        /**
         * Get the ImageView.
         *
         * @return The view
         */
        public ImageView get() {
            return mReference.get();
        }
    }

    /**
     * A dummy Drawable to serve as a placeholder
     */
    private static class ThumbDrawable extends BitmapDrawable {
        public final Object key;

        /**
         * @param key The key to reference this thumbnail
         */
        @SuppressWarnings("deprecation")
        public ThumbDrawable(Object key) {
            this.key = key;
        }
    }
}
