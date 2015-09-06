package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * Activity for editing or creating an entry category.
 *
 * @author Steve Guidetti
 */
public class EditCatActivity extends AppCompatActivity {
    /**
     * Intent extra for the category ID
     */
    public static final String EXTRA_CAT_ID = "cat_id";
    public static final String EXTRA_CAT_NAME = "cat_name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState == null) {
            final Intent intent = getIntent();
            final Bundle arguments = new Bundle();
            arguments.putLong(EditCatFragment.ARG_CAT_ID, intent.getLongExtra(EXTRA_CAT_ID, 0));
            arguments.putString(EditCatFragment.ARG_CAT_NAME,
                    intent.getStringExtra(EXTRA_CAT_NAME));

            final EditCatFragment fragment = new EditCatFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
