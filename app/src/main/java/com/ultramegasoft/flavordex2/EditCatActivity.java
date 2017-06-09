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
package com.ultramegasoft.flavordex2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.fragment.EditCatFragment;

/**
 * Activity for editing or creating an entry category.
 *
 * @author Steve Guidetti
 */
public class EditCatActivity extends AppCompatActivity {
    /**
     * Keys for the Intent extras
     */
    private static final String EXTRA_CAT_ID = "cat_id";
    private static final String EXTRA_CAT_NAME = "cat_name";

    /**
     * Start the Activity to edit a category.
     *
     * @param context The Context
     * @param catId   The category ID
     * @param catName The category name
     */
    public static void startActivity(@NonNull Context context, long catId,
                                     @Nullable String catName) {
        final Intent intent = new Intent(context, EditCatActivity.class);
        intent.putExtra(EXTRA_CAT_ID, catId);
        intent.putExtra(EXTRA_CAT_NAME, catName);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null) {
            final Intent intent = getIntent();
            final Bundle args = new Bundle();
            args.putLong(EditCatFragment.ARG_CAT_ID, intent.getLongExtra(EXTRA_CAT_ID, 0));
            args.putString(EditCatFragment.ARG_CAT_NAME, intent.getStringExtra(EXTRA_CAT_NAME));

            final EditCatFragment fragment = new EditCatFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
