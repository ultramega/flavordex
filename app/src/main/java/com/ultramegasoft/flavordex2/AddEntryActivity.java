package com.ultramegasoft.flavordex2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.ultramegasoft.flavordex2.util.PermissionUtils;

/**
 * Activity for adding a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddEntryActivity extends AppCompatActivity {
    /**
     * Keys for the Intent extras
     */
    private static final String EXTRA_CAT_ID = "cat_id";
    private static final String EXTRA_CAT_NAME = "cat_name";

    /**
     * Intent extras for the resulting entry to send to the calling Activity
     */
    public static final String EXTRA_ENTRY_ID = "entry_id";
    public static final String EXTRA_ENTRY_CAT = "entry_cat";
    public static final String EXTRA_ENTRY_CAT_ID = "entry_cat_id";

    /**
     * Get an Intent to start this Activity.
     *
     * @param context The Context
     * @param catId   The category ID
     * @param catName The category name
     * @return An Intent to start the Activity
     */
    public static Intent getIntent(Context context, long catId, String catName) {
        final Intent intent = new Intent(context, AddEntryActivity.class);
        intent.putExtra(EXTRA_CAT_ID, catId);
        intent.putExtra(EXTRA_CAT_NAME, catName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null) {
            final Intent intent = getIntent();
            final Bundle args = new Bundle();
            args.putLong(AddEntryFragment.ARG_CAT_ID, intent.getLongExtra(EXTRA_CAT_ID, 0));
            args.putString(AddEntryFragment.ARG_CAT_NAME, intent.getStringExtra(EXTRA_CAT_NAME));

            final Fragment fragment = new AddEntryFragment();
            fragment.setArguments(args);

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

    /**
     * Send the ID of the new entry to the calling Activity.
     *
     * @param entryId    The ID of the newly created entry
     * @param entryCat   The name of the entry category
     * @param entryCatId The ID for the entry category
     */
    public void publishResult(long entryId, String entryCat, long entryCatId) {
        final Intent data = new Intent();
        data.putExtra(EXTRA_ENTRY_ID, entryId);
        data.putExtra(EXTRA_ENTRY_CAT, entryCat);
        data.putExtra(EXTRA_CAT_ID, entryCatId);

        setResult(RESULT_OK, data);
        finish();
    }
}
