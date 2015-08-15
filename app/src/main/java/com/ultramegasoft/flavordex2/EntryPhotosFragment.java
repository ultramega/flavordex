package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Fragment to display the photos for a journal entry.
 *
 * @author Steve Guidetti
 */
public class EntryPhotosFragment extends Fragment {
    /**
     * The database id for this entry
     */
    private long mEntryId;

    public EntryPhotosFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntryId = getArguments().getLong(EntryDetailFragment.ARG_ITEM_ID);
    }
}
