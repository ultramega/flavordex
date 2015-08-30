package com.ultramegasoft.flavordex2;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.RadarHolder;
import com.ultramegasoft.flavordex2.widget.RadarView;

import java.util.ArrayList;

/**
 * Fragment for adding the flavor radar values for a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddEntryFlavorsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private RadarView mRadarView;

    private long mEntryId;

    public AddEntryFlavorsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntryId = getArguments().getLong(AddEntryFragment.ARG_TYPE_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_add_flavors, container, false);
        mRadarView = (RadarView)root.findViewById(R.id.radar);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(savedInstanceState == null) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            setupEditWidget();
            mRadarView.setVisibility(View.VISIBLE);
        }
    }

    private void setupEditWidget() {
        final int scale = 100 / mRadarView.getMaxValue();

        final TextView name = (TextView)getActivity().findViewById(R.id.flavor_name);

        final SeekBar slider = (SeekBar)getActivity().findViewById(R.id.slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    final int value = Math.round(progress / scale);
                    mRadarView.setSelectedValue(value);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        slider.setKeyProgressIncrement(scale);

        final ImageButton btnTurnLeft = (ImageButton)getActivity()
                .findViewById(R.id.button_turn_left);
        btnTurnLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mRadarView.turnCW();
                name.setText(mRadarView.getSelectedName());
                slider.setProgress(mRadarView.getSelectedValue() * scale);
            }
        });

        final ImageButton btnTurnRight = (ImageButton)getActivity()
                .findViewById(R.id.button_turn_right);
        btnTurnRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mRadarView.turnCCW();
                name.setText(mRadarView.getSelectedName());
                slider.setProgress(mRadarView.getSelectedValue() * scale);
            }
        });

        name.setText(mRadarView.getSelectedName());
    }

    public ContentValues[] getData() {
        final ArrayList<ContentValues> data = new ArrayList<>();
        final ArrayList<RadarHolder> radarHolders = mRadarView.getData();

        ContentValues rowValues;
        for(RadarHolder holder : radarHolders) {
            rowValues = new ContentValues();
            rowValues.put(Tables.EntriesFlavors.FLAVOR, holder.id);
            rowValues.put(Tables.EntriesFlavors.VALUE, holder.value);

            data.add(rowValues);
        }

        return data.toArray(new ContentValues[data.size()]);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = ContentUris.withAppendedId(Tables.Types.CONTENT_ID_URI_BASE, mEntryId);
        return new CursorLoader(getActivity(), Uri.withAppendedPath(uri, "flavor"), null, null,
                null, Tables.Flavors._ID + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final ArrayList<RadarHolder> holders = new ArrayList<>();
        long id;
        String name;
        while(data.moveToNext()) {
            id = data.getLong(data.getColumnIndex(Tables.Flavors._ID));
            name = data.getString(data.getColumnIndex(Tables.Flavors.NAME));
            holders.add(new RadarHolder(id, name, 3));
        }

        mRadarView.setData(holders);
        mRadarView.setEditable(true);
        setupEditWidget();
        mRadarView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
