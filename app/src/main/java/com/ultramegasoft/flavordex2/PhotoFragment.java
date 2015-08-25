package com.ultramegasoft.flavordex2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.ultramegasoft.flavordex2.util.BitmapCache;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.lang.ref.WeakReference;

/**
 * Fragment for displaying a single photo.
 *
 * @author Steve Guidetti
 */
public class PhotoFragment extends Fragment implements PopupMenu.OnMenuItemClickListener {
    /**
     * Argument for the path to the image file
     */
    public static final String ARG_PATH = "path";

    /**
     * Shared memory cache for images
     */
    private static final BitmapCache sBitmapCache = new BitmapCache("photos");

    public PhotoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String path = getArguments().getString(ARG_PATH);
        if(path == null) {
            return null;
        }

        final ImageView imageView = new ImageView(getActivity());
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        final Bitmap bitmap = sBitmapCache.get(path);
        if(bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            final Display display = ((WindowManager)getActivity()
                    .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            final Point size = new Point();
            display.getSize(size);
            new ImageLoader(imageView, Math.min(size.x, size.y)).execute(path);
        }

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showMenu(v);
                return true;
            }
        });

        return imageView;
    }

    /**
     * Show the popup menu for the photo.
     *
     * @param v The view to attach the menu to
     */
    private void showMenu(View v) {
        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.photo_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_remove_photo:
                final EntryPhotosFragment target = (EntryPhotosFragment)getParentFragment();
                if(target != null) {
                    target.confirmDeletePhoto();
                }
                return true;
        }
        return false;
    }

    /**
     * Loads images in the background.
     */
    private static class ImageLoader extends AsyncTask<String, Void, Bitmap> {
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
            sBitmapCache.put(path, bitmap);
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

}
