package com.ultramegasoft.flavordex2.dialog;

import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.ListView;

import com.ultramegasoft.flavordex2.EditCatActivity;
import com.ultramegasoft.flavordex2.R;

/**
 * Full implementation of the Dialog to select a category. Adds a footer item to the list for
 * creating a category.
 *
 * @author Steve Guidetti
 */
public class CatListDialog extends BaseCatListDialog {
    @Override
    protected ListView getLayout() {
        final ListView listView = super.getLayout();
        listView.addFooterView(LayoutInflater.from(getContext())
                .inflate(R.layout.cat_add_list_item, listView, false));
        return listView;
    }

    protected void onCatSelected(int position, long id) {
        if(position == mAdapter.getCount()) {
            startActivity(new Intent(getContext(), EditCatActivity.class));
            return;
        }
        super.onCatSelected(position, id);
    }
}
