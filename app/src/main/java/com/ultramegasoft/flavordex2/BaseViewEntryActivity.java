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
public class BaseViewEntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

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

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.entry_detail_container, fragment).commit();
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
