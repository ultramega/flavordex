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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final String path = getArguments().getString(ARG_PATH);
        if(path == null) {
            return null;
        }

        final View root = inflater.inflate(R.layout.fragment_photo, container, false);
        final ImageView imageView = (ImageView)root.findViewById(R.id.image);

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
                showMenu(root.findViewById(R.id.anchor));
                return true;
            }
        });

        return root;
    }

    /**
     * Show the PopupMenu for the photo.
     *
     * @param v The View to attach the menu to
     */
    private void showMenu(View v) {
        final PopupMenu popupMenu = new PopupMenu(getContext(), v);
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
