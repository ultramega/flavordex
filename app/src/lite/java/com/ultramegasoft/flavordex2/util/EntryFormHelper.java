package com.ultramegasoft.flavordex2.util;

import android.support.v4.app.Fragment;
import android.view.View;

/**
 * Form helper for the lite version.
 *
 * @author Steve Guidetti
 */
public class EntryFormHelper extends AbsEntryFormHelper {
    /**
     * @param fragment   The Fragment using this helper object
     * @param layoutRoot The root of the layout
     */
    public EntryFormHelper(Fragment fragment, View layoutRoot) {
        super(fragment, layoutRoot);
    }
}
