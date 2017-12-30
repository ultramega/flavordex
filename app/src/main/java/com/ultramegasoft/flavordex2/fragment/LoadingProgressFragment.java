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
package com.ultramegasoft.flavordex2.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_loading, container, false);
        mLayout = root.findViewById(R.id.layout);
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
    void hideLoadingIndicator(boolean animate) {
        mLoadingOverlay.setVisibility(View.GONE);
        mLayout.setVisibility(View.VISIBLE);
        if(animate) {
            mLayout.startAnimation(AnimationUtils.loadAnimation(getContext(),
                    android.R.anim.fade_in));
        }
    }
}
