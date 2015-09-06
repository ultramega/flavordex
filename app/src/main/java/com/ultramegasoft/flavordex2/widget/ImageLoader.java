package com.ultramegasoft.flavordex2.widget;

import android.graphics.Bitmap;
import android.os.AsyncTask;
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
     * The path to the photo file to load from disk
     */
    private final String mPath;

    /**
     * The cache to store loaded Bitmaps
     */
    private final BitmapCache mCache;

    /**
     * @param imageView The ImageView to hold the image
     * @param width     The width of the container
     * @param height    The height of the container
     * @param path      The path to the photo file to load from disk
     * @param cache     The cache to store loaded Bitmaps
     */
    public ImageLoader(ImageView imageView, int width, int height, String path, BitmapCache cache) {
        mImageViewReference = new WeakReference<>(imageView);
        mWidth = width;
        mHeight = height;
        mPath = path;
        mCache = cache;
    }

    /**
     * @param imageView The ImageView to hold the image
     * @param width     The width of the container
     * @param height    The height of the container
     * @param path      The path to the photo file to load from disk
     */
    public ImageLoader(ImageView imageView, int width, int height, String path) {
        this(imageView, width, height, path, null);
    }

    @Override
    protected Bitmap doInBackground(Void... args) {
        final Bitmap bitmap = PhotoUtils.loadBitmap(mPath, mWidth, mHeight);
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
