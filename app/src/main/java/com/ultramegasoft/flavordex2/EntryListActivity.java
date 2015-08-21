package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


/**
 * The main application activity. This shows a list of all the journal entries. On narrow screens,
 * selecting an entry launches a new activity to show details. On wide screens, selecting an entry
 * shows details in a fragment in this activity.
 *
 * @author Steve Guidetti
 */
public class EntryListActivity extends AppCompatActivity implements EntryListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_list);

        if(findViewById(R.id.entry_detail_container) != null) {
            mTwoPane = true;

            ((EntryListFragment)getSupportFragmentManager().findFragmentById(R.id.entry_list))
                    .setActivateOnItemClick(true);
        }
    }

    @Override
    public void onItemSelected(long id, String type) {
        if(mTwoPane) {
            final Bundle arguments = new Bundle();
            arguments.putLong(EntryDetailFragment.ARG_ITEM_ID, id);
            arguments.putString(EntryDetailFragment.ARG_ITEM_TYPE, type);

            final EntryDetailFragment fragment = new EntryDetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.entry_detail_container, fragment).commit();

        } else {
            final Intent detailIntent = new Intent(this, EntryDetailActivity.class);
            detailIntent.putExtra(EntryDetailFragment.ARG_ITEM_ID, id);
            detailIntent.putExtra(EntryDetailFragment.ARG_ITEM_TYPE, type);
            startActivity(detailIntent);
        }
    }
}
