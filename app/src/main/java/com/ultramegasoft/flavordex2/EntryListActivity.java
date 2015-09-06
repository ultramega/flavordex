package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


/**
 * The main application Activity. This shows a list of all the journal entries. On narrow screens,
 * selecting an entry launches a new Activity to show details. On wide screens, selecting an entry
 * shows details in a Fragment in this Activity.
 *
 * @author Steve Guidetti
 */
public class EntryListActivity extends AppCompatActivity implements EntryListFragment.Callbacks {

    /**
     * Whether the Activity is in two-pane mode
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
    public void onItemSelected(long id, String cat) {
        if(mTwoPane) {
            final Bundle arguments = new Bundle();
            arguments.putLong(ViewEntryFragment.ARG_ITEM_ID, id);
            arguments.putString(ViewEntryFragment.ARG_ITEM_CAT, cat);

            final ViewEntryFragment fragment = new ViewEntryFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.entry_detail_container, fragment).commit();

        } else {
            final Intent detailIntent = new Intent(this, ViewEntryActivity.class);
            detailIntent.putExtra(ViewEntryFragment.ARG_ITEM_ID, id);
            detailIntent.putExtra(ViewEntryFragment.ARG_ITEM_CAT, cat);
            startActivity(detailIntent);
        }
    }
}
