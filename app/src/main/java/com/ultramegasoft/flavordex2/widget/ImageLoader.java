package com.ultramegasoft.flavordex2.widget;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.ultramegasoft.flavordex2.util.BitmapCache;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.lang.ref.WeakReference;

/**
 * Loads images in the background.
 *
 * @author Steve Guidetti
 */
public class ImageLoader extends AsyncTask<Void, Void, Bitmap> {
    /**
     * Reference to the ImageView
     */
    private final WeakReference<ImageView> mImageViewReference;

    /**
     * The minimum width or height of the image
     */
    private final int mMinWH;

    /**
     * The path to the photo file to load from disk
     */
    private final String mPath;

    /**
     * The cache to store loaded bitmaps
     */
    private final BitmapCache mCache;

    /**
     * @param imageView The ImageView to hold the image
     * @param minWH     The minimum width or height of the image
     * @param path      The path to the photo file to load from disk
     * @param cache     The cache to store loaded bitmaps
     */
    public ImageLoader(ImageView imageView, int minWH, String path, BitmapCache cache) {
        mImageViewReference = new WeakReference<>(imageView);
        mMinWH = minWH;
        mPath = path;
        mCache = cache;
    }

    /**
     * @param imageView The ImageView to hold the image
     * @param minWH     The minimum width or height of the image
     * @param path      The path to the photo file to load from disk
     */
    public ImageLoader(ImageView imageView, int minWH, String path) {
        this(imageView, minWH, path, null);
    }

    @Override
    protected Bitmap doInBackground(Void... args) {
        final Bitmap bitmap = PhotoUtils.loadBitmap(mPath, mMinWH);
        if(mCache != null) {
            mCache.put(mPath, bitmap);
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if(result != null) {
            final ImageView imageView = mImageViewReference.get();
            if(imageView != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
