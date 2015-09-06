package com.ultramegasoft.flavordex2;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.beer.EditBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.EditCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.whiskey.EditWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.wine.EditWineInfoFragment;

/**
 * Activity for editing a journal entry.
 *
 * @author Steve Guidetti
 */
public class EditEntryActivity extends AppCompatActivity {
    /**
     * Keys for the intent extras
     */
    public static final String EXTRA_ENTRY_ID = "entry_id";
    public static final String EXTRA_ENTRY_CAT = "entry_cat";

    /**
     * The id for the entry being edited
     */
    private long mEntryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEntryId = getIntent().getLongExtra(EXTRA_ENTRY_ID, mEntryId);
        if(savedInstanceState == null) {
            final Bundle args = new Bundle();
            args.putLong(EditInfoFragment.ARG_ENTRY_ID, mEntryId);

            final Fragment fragment = getFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_entry_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_save:
                saveData();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the fragment based on the entry category.
     *
     * @return The fragment object
     */
    private EditInfoFragment getFragment() {
        final String cat = getIntent().getStringExtra(EXTRA_ENTRY_CAT);

        if(FlavordexApp.CAT_BEER.equals(cat)) {
            return new EditBeerInfoFragment();
        }
        if(FlavordexApp.CAT_WINE.equals(cat)) {
            return new EditWineInfoFragment();
        }
        if(FlavordexApp.CAT_WHISKEY.equals(cat)) {
            return new EditWhiskeyInfoFragment();
        }
        if(FlavordexApp.CAT_COFFEE.equals(cat)) {
            return new EditCoffeeInfoFragment();
        }

        return new EditInfoFragment();
    }

    /**
     * Save the changes for the entry.
     */
    private void saveData() {
        final EditInfoFragment fragment = (EditInfoFragment)getSupportFragmentManager()
                .findFragmentById(android.R.id.content);
        if(fragment == null || !fragment.isValid()) {
            return;
        }
        new DataSaver(getContentResolver(), mEntryId, fragment.getData(), fragment.getExtras())
                .execute();
        finish();
    }

    /**
     * Task for saving an entry in the background.
     */
    private static class DataSaver extends AsyncTask<Void, Void, Void> {
        /**
         * The ContentResolver to use
         */
        private final ContentResolver mResolver;

        /**
         * The id for the entry to save to
         */
        private final long mEntryId;

        /**
         * Values for the entries table
         */
        private final ContentValues mEntryInfo;

        /**
         * Values for the entries_extras table
         */
        private final ContentValues[] mEntryExtras;

        /**
         * @param cr          The ContentResolver to use
         * @param entryId     The id for the entry to save to
         * @param entryInfo   Values for the entries table
         * @param entryExtras Values for the entries_extras table
         */
        public DataSaver(ContentResolver cr, long entryId, ContentValues entryInfo,
                         ContentValues[] entryExtras) {
            mResolver = cr;
            mEntryId = entryId;
            mEntryInfo = entryInfo;
            mEntryExtras = entryExtras;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntryId);
            mResolver.update(uri, mEntryInfo, null, null);
            mResolver.bulkInsert(Uri.withAppendedPath(uri, "extras"), mEntryExtras);
            return null;
        }
    }
}
