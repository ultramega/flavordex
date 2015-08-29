package com.ultramegasoft.flavordex2.util;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Memory cache for bitmaps.
 *
 * @author Steve Guidetti
 */
public class BitmapCache {
    /**
     * The memory cache for storing data
     */
    private final LruCache<Object, Bitmap> mCache;

    public BitmapCache() {
        final long maxMem = Runtime.getRuntime().maxMemory();
        final int cacheSize = (int)maxMem / 8;
        mCache = new LruCache<Object, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Object key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    /**
     * Store a bitmap in the cache.
     *
     * @param key    The key to reference the item
     * @param bitmap The bitmap to store
     */
    public void put(Object key, Bitmap bitmap) {
        if(key == null || bitmap == null) {
            return;
        }
        mCache.put(key, bitmap);
    }

    /**
     * Retrieve a bitmap from the cache.
     *
     * @param key The key referencing the item
     * @return The bitmap
     */
    public Bitmap get(Object key) {
        return mCache.get(key);
    }

    /**
     * Remove a bitmap from the cache.
     *
     * @param key The key referencing the item
     */
    public void remove(Object key) {
        mCache.remove(key);
    }
}
