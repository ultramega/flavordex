package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Fragment for editing or creating an entry type.
 *
 * @author Steve Guidetti
 */
public class EditTypeFragment extends Fragment {
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
        mTypeId = getArguments().getLong(ARG_TYPE_ID);
    }
}
