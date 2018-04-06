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
package com.ultramegasoft.flavordex2.util;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @NonNull
    private final LruCache<String, Bitmap> mCache;

    public BitmapCache() {
        final long maxMem = Runtime.getRuntime().maxMemory();
        final int cacheSize = (int)maxMem / 8;
        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @SuppressWarnings("MethodDoesntCallSuperMethod")
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    private BitmapCache(Parcel in) {
        this();
        final Bundle bundle = in.readBundle(BitmapCache.class.getClassLoader());
        for(String key : bundle.keySet()) {
            final Bitmap bitmap = bundle.getParcelable(key);
            if(bitmap != null) {
                put(key, bitmap);
            }
        }
    }

    /**
     * Store a Bitmap in the cache.
     *
     * @param key    The key to reference the item
     * @param bitmap The Bitmap to store
     */
    public void put(@NonNull Object key, @NonNull Bitmap bitmap) {
        mCache.put(key.toString(), bitmap);
    }

    /**
     * Retrieve a Bitmap from the cache.
     *
     * @param key The key referencing the item
     * @return The Bitmap
     */
    @Nullable
    public Bitmap get(@NonNull Object key) {
        return mCache.get(key.toString());
    }

    /**
     * Remove a Bitmap from the cache.
     *
     * @param key The key referencing the item
     */
    public void remove(@NonNull Object key) {
        mCache.remove(key.toString());
    }

    @SuppressWarnings("SameReturnValue")
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
