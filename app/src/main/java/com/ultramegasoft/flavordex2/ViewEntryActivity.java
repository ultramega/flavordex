package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * This activity holds the entry details on narrow screen devices.
 *
 * @author Steve Guidetti
 */
public class ViewEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState == null) {
            final Intent intent = getIntent();
            final Bundle arguments = new Bundle();
            arguments.putLong(ViewEntryFragment.ARG_ITEM_ID,
                    intent.getLongExtra(ViewEntryFragment.ARG_ITEM_ID, 0));
            arguments.putString(ViewEntryFragment.ARG_ITEM_CAT,
                    intent.getStringExtra(ViewEntryFragment.ARG_ITEM_CAT));

            final ViewEntryFragment fragment = new ViewEntryFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.entry_detail_container, fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, EntryListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
