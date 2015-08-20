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
     * The shared memory cache for the application
     */
    private static LruCache<String, Bitmap> sCache;

    /**
     * The prefix for keys this cache instance
     */
    private final String mPrefix;

    /**
     * Create the shared memory cache for this application
     */
    private static void initCache() {
        final long maxMem = Runtime.getRuntime().maxMemory();
        final int cacheSize = (int)maxMem / 4;
        sCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    /**
     * @param prefix A unique prefix for items in this instance
     */
    public BitmapCache(String prefix) {
        if(sCache == null) {
            initCache();
        }
        mPrefix = prefix;
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
        sCache.put(getRealKey(key), bitmap);
    }

    /**
     * Retrieve a bitmap from the cache.
     *
     * @param key The key referencing the item
     * @return The bitmap
     */
    public Bitmap get(Object key) {
        return sCache.get(getRealKey(key));
    }

    /**
     * Remove a bitmap from the cache.
     *
     * @param key The key referencing the item
     */
    public void remove(Object key) {
        sCache.remove(getRealKey(key));
    }

    /**
     * Get the internal key for an item.
     *
     * @param key An item's key
     * @return The key as a string with prefix attached
     */
    private String getRealKey(Object key) {
        return mPrefix + "_" + key.toString();
    }
}
