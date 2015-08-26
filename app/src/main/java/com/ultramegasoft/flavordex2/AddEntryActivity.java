package com.ultramegasoft.flavordex2;

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
     * Intent extra for the type id parameter
     */
    public static final String EXTRA_TYPE_ID = "type_id";

    /**
     * Intent extra for the resulting entry id to send to the calling activity
     */
    public static final String EXTRA_ENTRY_ID = "entry_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final long typeId = getIntent().getLongExtra(EXTRA_TYPE_ID, 0);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState == null) {
            if(typeId > 0) {
                final Fragment fragment = new AddEntryFragment();
                final Bundle args = new Bundle();
                args.putLong(AddEntryFragment.ARG_TYPE_ID, typeId);
                fragment.setArguments(args);
                getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                        .commit();
            } else {
                final Fragment fragment = new TypeListFragment();
                getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                        .commit();
            }
        }
    }
}
