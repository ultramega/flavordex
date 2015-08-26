package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * Activity for editing or creating an entry type.
 *
 * @author Steve Guidetti
 */
public class EditTypeActivity extends AppCompatActivity {
    /**
     * Intent extra for the type id
     */
    public static final String EXTRA_TYPE_ID = "type_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState == null) {
            final Intent intent = getIntent();
            final Bundle arguments = new Bundle();
            arguments.putLong(EditTypeFragment.ARG_TYPE_ID, intent.getLongExtra(EXTRA_TYPE_ID, 0));

            final EditTypeFragment fragment = new EditTypeFragment();
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
