package com.ultramegasoft.flavordex2.fragment;

import android.view.MenuItem;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.UpgradeUtils;

/**
 * Lite implementation of the Fragment for showing the list of categories.
 *
 * @author Steve Guidetti
 */
public class CatListFragment extends BaseCatListFragment {
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_add_cat:
                UpgradeUtils.showDialog(getFragmentManager());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
