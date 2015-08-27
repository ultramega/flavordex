package com.ultramegasoft.flavordex2;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RatingBar;

import com.ultramegasoft.flavordex2.provider.Tables;

/**
 * Fragment for adding details for a new journal entry.
 *
 * @author Steve Guidetti
 */
public class AddEntryInfoFragment extends Fragment {
    /**
     * The views for the form fields
     */
    private EditText mTxtTitle;
    private AutoCompleteTextView mTxtMaker;
    private EditText mTxtOrigin;
    private EditText mTxtLocation;
    private EditText mTxtPrice;
    private RatingBar mRatingBar;
    private EditText mTxtNotes;

    public AddEntryInfoFragment() {
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(getLayoutId(), container, false);

        mTxtTitle = (EditText)root.findViewById(R.id.entry_title);
        mTxtMaker = (AutoCompleteTextView)root.findViewById(R.id.entry_maker);
        mTxtOrigin = (EditText)root.findViewById(R.id.entry_origin);
        mTxtLocation = (EditText)root.findViewById(R.id.entry_location);
        mTxtPrice = (EditText)root.findViewById(R.id.entry_price);
        mRatingBar = (RatingBar)root.findViewById(R.id.entry_rating);
        mTxtNotes = (EditText)root.findViewById(R.id.entry_notes);

        return root;
    }

    /**
     * Get the id for the layout to use.
     *
     * @return An id from R.layout
     */
    protected int getLayoutId() {
        return R.layout.fragment_add_info;
    }

    protected ContentValues readViews() {
        final ContentValues values = new ContentValues();

        values.put(Tables.Entries.TITLE, mTxtTitle.getText().toString());
        values.put(Tables.Entries.MAKER, mTxtMaker.getText().toString());
        values.put(Tables.Entries.ORIGIN, mTxtOrigin.getText().toString());
        values.put(Tables.Entries.LOCATION, mTxtLocation.getText().toString());
        values.put(Tables.Entries.PRICE, mTxtPrice.getText().toString());
        values.put(Tables.Entries.RATING, mRatingBar.getRating());
        values.put(Tables.Entries.NOTES, mTxtNotes.getText().toString());

        return values;
    }

}
