package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

/**
 * The parent fragment for the entry creation pages.
 *
 * @author Steve Guidetti
 */
public class AddEntryFragment extends Fragment {
    /**
     * Keys for the fragment arguments
     */
    public static final String ARG_TYPE_ID = "type_id";

    /**
     * The type id from the arguments
     */
    private long mTypeId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTypeId = getArguments().getLong(ARG_TYPE_ID, 0);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                final Fragment fragment = new TypeListFragment();
                getFragmentManager().beginTransaction().replace(android.R.id.content, fragment)
                        .commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
