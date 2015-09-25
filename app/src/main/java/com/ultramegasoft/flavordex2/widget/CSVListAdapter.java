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
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView =
                    LayoutInflater.from(mContext).inflate(R.layout.entry_list_item, parent, false);

            convertView.findViewById(R.id.checkbox).setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.thumb).setVisibility(View.GONE);

            final Holder holder = new Holder();
            holder.title = (TextView)convertView.findViewById(R.id.title);
            holder.maker = (TextView)convertView.findViewById(R.id.maker);
            holder.rating = (RatingBar)convertView.findViewById(R.id.rating);
            holder.date = (TextView)convertView.findViewById(R.id.date);
            convertView.setTag(holder);
        }

        final EntryHolder entry = getItem(position);

        final Holder holder = (Holder)convertView.getTag();
        holder.title.setText(entry.title);
        holder.maker.setText(entry.maker);
        holder.rating.setRating(entry.rating);
        holder.date.setText(mDateFormat.format(new Date(entry.date)));

        return convertView;
    }

    /**
     * Holder for View references
     */
    private static class Holder {
        TextView title;
        TextView maker;
        RatingBar rating;
        TextView date;
    }
}
