package com.ultramegasoft.flavordex2.util;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.LruCache;

import java.util.Map;

/**
 * Memory cache for Bitmaps.
 *
 * @author Steve Guidetti
 */
public class BitmapCache implements Parcelable {
    public static final Creator<BitmapCache> CREATOR = new Creator<BitmapCache>() {
        @Override
        public BitmapCache createFromParcel(Parcel in) {
            return new BitmapCache(in);
        }

        @Override
        public BitmapCache[] newArray(int size) {
            return new BitmapCache[size];
        }
    };

    /**
     * The memory cache for storing data
     */
    private final LruCache<String, Bitmap> mCache;

    public BitmapCache() {
        final long maxMem = Runtime.getRuntime().maxMemory();
        final int cacheSize = (int)maxMem / 8;
        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    protected BitmapCache(Parcel in) {
        this();
        final Bundle bundle = in.readBundle();
        for(String key : bundle.keySet()) {
            put(key, (Bitmap)bundle.getParcelable(key));
        }
    }

    /**
     * Store a Bitmap in the cache.
     *
     * @param key    The key to reference the item
     * @param bitmap The Bitmap to store
     */
    public void put(Object key, Bitmap bitmap) {
        if(key == null || bitmap == null) {
            return;
        }
        mCache.put(key.toString(), bitmap);
    }

    /**
     * Retrieve a Bitmap from the cache.
     *
     * @param key The key referencing the item
     * @return The Bitmap
     */
    public Bitmap get(Object key) {
        return mCache.get(key.toString());
    }

    /**
     * Remove a Bitmap from the cache.
     *
     * @param key The key referencing the item
     */
    public void remove(Object key) {
        mCache.remove(key.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final Bundle bundle = new Bundle();
        for(Map.Entry<String, Bitmap> entry : mCache.snapshot().entrySet()) {
            bundle.putParcelable(entry.getKey(), entry.getValue());
        }
        dest.writeBundle(bundle);
    }
}
