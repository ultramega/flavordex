package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.CSVUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Custom Adapter for listing entries from a CSV file as checkable items.
 *
 * @author Steve Guidetti
 */
public class CSVListAdapter extends BaseAdapter {
    /**
     * The Context
     */
    private final Context mContext;

    /**
     * The data backing this Adapter
     */
    private final CSVUtils.CSVHolder mData;

    /**
     * Formatter for dates
     */
    private final SimpleDateFormat mDateFormat;

    /**
     * @param context The Context
     * @param data    The data backing this Adapter
     */
    public CSVListAdapter(Context context, CSVUtils.CSVHolder data) {
        mContext = context;
        mData = data;
        mDateFormat = new SimpleDateFormat(context.getString(R.string.date_format), Locale.US);
    }

    @Override
    public int getCount() {
        return mData.entries.size();
    }

    @Override
    public EntryHolder getItem(int position) {
        return mData.entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.entry_list_item_multiple_choice, parent, false);
        }

        final EntryHolder entry = getItem(position);

        final TextView titleView = (TextView)convertView.findViewById(R.id.title);
        final TextView makerView = (TextView)convertView.findViewById(R.id.maker);
        final RatingBar ratingBar = (RatingBar)convertView.findViewById(R.id.rating);
        final TextView dateView = (TextView)convertView.findViewById(R.id.date);

        titleView.setText(entry.title);
        makerView.setText(entry.maker);
        ratingBar.setRating(entry.rating);
        dateView.setText(mDateFormat.format(new Date(entry.date)));

        return convertView;
    }
}
