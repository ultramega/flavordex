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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.fragment.ViewEntryFragment;
import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Base class for the Activity that holds the entry details on narrow screen devices.
 *
 * @author Steve Guidetti
 */
public class ViewEntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null) {
            final Intent intent = getIntent();
            final Bundle arguments = new Bundle();
            arguments.putLong(ViewEntryFragment.ARG_ENTRY_ID,
                    intent.getLongExtra(ViewEntryFragment.ARG_ENTRY_ID, 0));
            arguments.putString(ViewEntryFragment.ARG_ENTRY_CAT,
                    intent.getStringExtra(ViewEntryFragment.ARG_ENTRY_CAT));
            arguments.putLong(ViewEntryFragment.ARG_ENTRY_CAT_ID,
                    intent.getLongExtra(ViewEntryFragment.ARG_ENTRY_CAT_ID, 0));

            final ViewEntryFragment fragment = new ViewEntryFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                    .commit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
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

    @Override
    public void onBackPressed() {
        final Fragment fragment =
                getSupportFragmentManager().findFragmentById(R.id.entry_detail_container);
        if(fragment instanceof ViewEntryFragment) {
            if(((ViewEntryFragment)fragment).onBackButtonPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }
}
