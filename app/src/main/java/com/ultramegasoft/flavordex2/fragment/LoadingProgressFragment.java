package com.ultramegasoft.flavordex2.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.ultramegasoft.flavordex2.R;

/**
 * Fragment base for showing a loading indicator before the actual layout.
 *
 * @author Steve Guidetti
 */
public abstract class LoadingProgressFragment extends Fragment {
    /**
     * The main layout container
     */
    private ViewGroup mLayout;

    /**
     * The loading indicator
     */
    private View mLoadingOverlay;

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_loading, container, false);
        mLayout = (ViewGroup)root.findViewById(R.id.layout);
        mLoadingOverlay = root.findViewById(R.id.progress);

        inflater.inflate(getLayoutId(), mLayout);

        return root;
    }

    /**
     * Get the ID for the layout to use.
     *
     * @return An ID from R.layout
     */
    protected abstract int getLayoutId();

    /**
     * Hide the loading progress indicator and show the actual layout.
     *
     * @param animate Whether to show the fade animation
     */
    public void hideLoadingIndicator(boolean animate) {
        mLoadingOverlay.setVisibility(View.GONE);
        mLayout.setVisibility(View.VISIBLE);
        if(animate) {
            mLayout.startAnimation(AnimationUtils.loadAnimation(getContext(),
                    android.R.anim.fade_in));
        }
    }
}
