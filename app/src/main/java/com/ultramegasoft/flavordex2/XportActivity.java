package com.ultramegasoft.flavordex2;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TabHost;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Activity for importing and exporting journal entries from and to CSV files.
 *
 * @author Steve Guidetti
 */
public class XportActivity extends AppCompatActivity {
    /**
     * Formatter for dates in CSV files
     */
    public static final SimpleDateFormat sDateFormatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US) {
                {
                    setTimeZone(TimeZone.getTimeZone("UTC"));
                }
            };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_layout);

        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final FragmentTabHost tabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.content);

        final Resources res = getResources();
        Drawable icon;
        TabHost.TabSpec tab;

        icon = res.getDrawable(R.drawable.ic_import);
        tab = tabHost.newTabSpec("import").setIndicator(null, icon);
        tabHost.addTab(tab, ImportFragment.class, null);

        icon = res.getDrawable(R.drawable.ic_export);
        tab = tabHost.newTabSpec("export").setIndicator(null, icon);
        tabHost.addTab(tab, ExportFragment.class, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, EntryListActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
