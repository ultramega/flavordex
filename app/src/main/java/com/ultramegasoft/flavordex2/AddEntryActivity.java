package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity for adding a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddEntryActivity extends AppCompatActivity {
    /**
     * Intent extra for the category id parameter
     */
    public static final String EXTRA_CAT_ID = "cat_id";

    /**
     * Intent extras for the resulting entry to send to the calling activity
     */
    public static final String EXTRA_ENTRY_ID = "entry_id";
    public static final String EXTRA_ENTRY_CAT = "entry_cat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final long catId = getIntent().getLongExtra(EXTRA_CAT_ID, 0);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState == null) {
            if(catId > 0) {
                final Fragment fragment = new AddEntryFragment();
                final Bundle args = new Bundle();
                args.putLong(AddEntryFragment.ARG_CAT_ID, catId);
                fragment.setArguments(args);
                getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                        .commit();
            } else {
                final Fragment fragment = new CatListFragment();
                getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                        .commit();
            }
        }
    }

    /**
     * Send the id of the new entry to the calling activity.
     *
     * @param entryId The id of the newly created entry
     */
    public void publishResult(long entryId, String entryCat) {
        final Intent data = new Intent();
        data.putExtra(EXTRA_ENTRY_ID, entryId);
        data.putExtra(EXTRA_ENTRY_CAT, entryCat);

        setResult(RESULT_OK, data);
        finish();
    }
}
