package com.ultramegasoft.flavordex2.widget;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.lang.ref.WeakReference;

/**
 * Loads images in the background.
 *
 * @author Steve Guidetti
 */
public class ImageLoader extends AsyncTask<String, Void, Bitmap> {
    /**
     * Reference to the ImageView
     */
    private final WeakReference<ImageView> mImageViewReference;

    /**
     * The minimum width or height of the image
     */
    private final int mMinWH;

    /**
     * @param imageView The ImageView to hold the image
     * @param minWH     The minimum width or height of the image
     */
    public ImageLoader(ImageView imageView, int minWH) {
        mImageViewReference = new WeakReference<>(imageView);
        mMinWH = minWH;
    }

    @Override
    protected Bitmap doInBackground(String... args) {
        final String path = args[0];
        final Bitmap bitmap = PhotoUtils.loadBitmap(path, mMinWH);
        PhotoUtils.getPhotoCache().put(path, bitmap);
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
