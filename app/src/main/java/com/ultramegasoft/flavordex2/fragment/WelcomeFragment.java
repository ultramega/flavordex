package com.ultramegasoft.flavordex2.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.BuildConfig;
import com.ultramegasoft.flavordex2.R;

import java.util.Calendar;

/**
 * @author Steve Guidetti
 */
public class WelcomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_welcome, container, false);

        final TextView version = (TextView)root.findViewById(R.id.text_version);
        version.setText(getString(R.string.message_version, BuildConfig.VERSION_NAME));

        ((TextView)root.findViewById(R.id.text_copyright))
                .setText(getString(R.string.message_copyright,
                        Calendar.getInstance().get(Calendar.YEAR)));

        return root;
    }
}
