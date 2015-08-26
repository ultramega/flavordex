package com.ultramegasoft.flavordex2;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * @author Steve Guidetti
 */
public class EditTypeFragment extends Fragment {
    public static final String ARG_TYPE_ID = "type_id";

    private long mTypeId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTypeId = getArguments().getLong(ARG_TYPE_ID);
    }
}
