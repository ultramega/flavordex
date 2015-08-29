package com.ultramegasoft.flavordex2;

import android.content.Context;
import android.graphics.Point;
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

        final Display display = ((WindowManager)getActivity()
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        new ImageLoader(imageView, Math.min(size.x, size.y), path).execute();

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

}
