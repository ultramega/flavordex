package com.ultramegasoft.flavordex2;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.ultramegasoft.flavordex2.beer.EditBeerInfoFragment;
import com.ultramegasoft.flavordex2.coffee.EditCoffeeInfoFragment;
import com.ultramegasoft.flavordex2.fragment.EditInfoFragment;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.whiskey.EditWhiskeyInfoFragment;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.wine.EditWineInfoFragment;

/**
 * Activity for editing a journal entry.
 *
 * @author Steve Guidetti
 */
public class EditEntryActivity extends AppCompatActivity {
    /**
     * Keys for the Intent extras
     */
    private static final String EXTRA_ENTRY_ID = "entry_id";
    private static final String EXTRA_ENTRY_CAT = "entry_cat";

    /**
     * The ID for the entry being edited
     */
    private long mEntryId;

    /**
     * Start the Activity to edit an entry.
     *
     * @param context  The Context
     * @param entryId  The ID for the entry to edit
     * @param entryCat The name of the entry category
     */
    public static void startActivity(Context context, long entryId, String entryCat) {
        final Intent intent = new Intent(context, EditEntryActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(EXTRA_ENTRY_CAT, entryCat);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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
        getMenuInflater().inflate(R.menu.entry_edit_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final Fragment fragment =
                getSupportFragmentManager().findFragmentById(android.R.id.content);
        if(fragment instanceof EditInfoFragment) {
            menu.findItem(R.id.menu_save).setEnabled(!((EditInfoFragment)fragment).isLoading());
        }
        return super.onPrepareOptionsMenu(menu);
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
     * Get the Fragment based on the entry category.
     *
     * @return The Fragment object
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

        final EntryHolder entry = new EntryHolder();
        fragment.getData(entry);
        new DataSaver(getContentResolver(), entry).execute();
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
         * The entry to save
         */
        private final EntryHolder mEntry;

        /**
         * @param cr    The ContentResolver to use
         * @param entry The entry to save
         */
        public DataSaver(ContentResolver cr, EntryHolder entry) {
            mResolver = cr;
            mEntry = entry;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Uri uri =
                    ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, mEntry.id);

            final ContentValues values = new ContentValues();
            values.put(Tables.Entries.TITLE, mEntry.title);
            values.put(Tables.Entries.MAKER, mEntry.maker);
            values.put(Tables.Entries.ORIGIN, mEntry.origin);
            values.put(Tables.Entries.PRICE, mEntry.price);
            values.put(Tables.Entries.LOCATION, mEntry.location);
            values.put(Tables.Entries.DATE, mEntry.date);
            values.put(Tables.Entries.RATING, mEntry.rating);
            values.put(Tables.Entries.NOTES, mEntry.notes);
            mResolver.update(uri, values, null, null);

            updateExtras(uri);
            return null;
        }

        /**
         * Update the entry extra fields.
         *
         * @param entryUri The Uri for the entry
         */
        private void updateExtras(Uri entryUri) {
            final Uri uri = Uri.withAppendedPath(entryUri, "extras");
            final ContentValues values = new ContentValues();
            for(ExtraFieldHolder extra : mEntry.getExtras()) {
                if(!extra.preset && TextUtils.isEmpty(extra.value)) {
                    mResolver.delete(uri, Tables.EntriesExtras.EXTRA + " = " + extra.id, null);
                    continue;
                }
                values.put(Tables.EntriesExtras.EXTRA, extra.id);
                values.put(Tables.EntriesExtras.VALUE, extra.value);
                mResolver.insert(uri, values);
            }
        }
    }
}
