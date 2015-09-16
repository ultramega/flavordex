package com.ultramegasoft.flavordex2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

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
    public static void startActivity(Context context, long catId, String catName) {
        final Intent intent = new Intent(context, EditCatActivity.class);
        intent.putExtra(EXTRA_CAT_ID, catId);
        intent.putExtra(EXTRA_CAT_NAME, catName);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
