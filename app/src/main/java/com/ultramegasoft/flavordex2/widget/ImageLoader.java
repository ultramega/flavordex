package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
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
     * The Uri to the photo file to load from disk
     */
    private final Uri mUri;

    /**
     * The cache to store loaded Bitmaps
     */
    private final BitmapCache mCache;

    /**
     * The Context
     */
    private final Context mContext;

    /**
     * @param imageView The ImageView to hold the image
     * @param width     The width of the container
     * @param height    The height of the container
     * @param uri       The Uri to the photo file to load from disk
     * @param cache     The cache to store loaded Bitmaps
     */
    public ImageLoader(ImageView imageView, int width, int height, Uri uri, BitmapCache cache) {
        mImageViewReference = new WeakReference<>(imageView);
        mWidth = width;
        mHeight = height;
        mUri = uri;
        mCache = cache;
        mContext = imageView.getContext().getApplicationContext();
    }

    /**
     * @param imageView The ImageView to hold the image
     * @param width     The width of the container
     * @param height    The height of the container
     * @param uri       The Uri to the photo file to load from disk
     */
    public ImageLoader(ImageView imageView, int width, int height, Uri uri) {
        this(imageView, width, height, uri, null);
    }

    @Override
    protected Bitmap doInBackground(Void... args) {
        final Bitmap bitmap = PhotoUtils.loadBitmap(mContext, mUri, mWidth, mHeight);
        if(mCache != null) {
            mCache.put(mUri, bitmap);
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
