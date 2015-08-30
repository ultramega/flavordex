package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.ultramegasoft.flavordex2.widget.ImageLoader;

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

        imageView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final int width = imageView.getWidth();
                        final int height = imageView.getHeight();
                        if(width > 0) {
                            new ImageLoader(imageView, width, height, path).execute();
                            //noinspection deprecation
                            imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });

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
                final ViewPhotosFragment target = (ViewPhotosFragment)getParentFragment();
                if(target != null) {
                    target.confirmDeletePhoto();
                }
                return true;
        }
        return false;
    }

}
