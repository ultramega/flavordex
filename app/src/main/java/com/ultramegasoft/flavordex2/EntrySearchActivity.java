package com.ultramegasoft.flavordex2;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.ultramegasoft.flavordex2.fragment.EntrySearchFragment;

/**
 * Activity to contain the entry search Fragment.
 *
 * @author Steve Guidetti
 */
public class EntrySearchActivity extends AppCompatActivity {
    /**
     * Keys for the Intent extras
     */
    public static final String EXTRA_FILTERS = "filters";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            final ContentValues filters = getIntent().getParcelableExtra(EXTRA_FILTERS);
            final Bundle args = new Bundle();
            args.putParcelable(EntrySearchFragment.ARG_FILTER_VALUES, filters);
            final Fragment fragment = new EntrySearchFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                    .commit();
        }
    }

    /**
     * Send the result from the Fragment to the calling Activity.
     *
     * @param data The data to return
     */
    public void publishResult(Intent data) {
        setResult(RESULT_OK, data);
        finish();
    }
}
