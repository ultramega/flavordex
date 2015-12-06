package com.ultramegasoft.flavordex2.dialog;

import android.view.View;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.UpgradeUtils;

/**
 * Lite implementation of the Dialog that shows information about the application. Adds a button to
 * upgrade to the full version.
 *
 * @author Steve Guidetti
 */
public class AboutDialog extends BaseAboutDialog {
    @Override
    protected View getLayout() {
        final View root = super.getLayout();

        root.findViewById(R.id.button_upgrade).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpgradeUtils.openStore(getContext());
            }
        });

        return root;
    }
}
