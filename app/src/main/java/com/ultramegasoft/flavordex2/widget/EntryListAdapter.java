package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.PhotoUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Custom Adapter for the main journal entry list.
 *
 * @author Steve Guidetti
 */
public class EntryListAdapter extends CursorAdapter {
    /**
     * Background loader for thumbnails
     */
    private static final BackgroundThumbLoader<Long> sThumbLoader =
            new BackgroundThumbLoader<Long>() {
                @Override
                protected Bitmap getBitmap(Thumb thumb) {
                    return PhotoUtils.getThumb(thumb.get().getContext(), (Long)thumb.key);
                }
            };

    /**
     * Formatter for the date field
     */
    private final SimpleDateFormat mDateFormat;

    /**
     * Map of item IDs to item categories
     */
    private final HashMap<Long, String> mItemCats = new HashMap<>();

    /**
     * Map of item IDs to their position index in the list
     */
    private final HashMap<Long, Integer> mItemPositions = new HashMap<>();

    /**
     * @param context The Context
     */
    public EntryListAdapter(Context context) {
        super(context, null, true);
        mDateFormat = new SimpleDateFormat(context.getString(R.string.date_format), Locale.US);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        mItemPositions.clear();
        if(newCursor != null) {
            while(newCursor.moveToNext()) {
                mItemPositions.put(newCursor.getLong(newCursor.getColumnIndex(Tables.Entries._ID)),
                        newCursor.getPosition());
            }
        }
        return super.swapCursor(newCursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View view =
                LayoutInflater.from(context).inflate(R.layout.entry_list_item, parent, false);

        final Holder holder = new Holder();
        holder.thumb = (ImageView)view.findViewById(R.id.thumb);
        holder.title = (TextView)view.findViewById(R.id.title);
        holder.maker = (TextView)view.findViewById(R.id.maker);
        holder.rating = (RatingBar)view.findViewById(R.id.rating);
        holder.date = (TextView)view.findViewById(R.id.date);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final Holder holder = (Holder)view.getTag();

        final long id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
        final String title = cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
        final String maker = cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER));
        final float rating = cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
        final long date = cursor.getLong(cursor.getColumnIndex(Tables.Entries.DATE));

        sThumbLoader.load(holder.thumb, id);
        holder.title.setText(title);
        holder.maker.setText(maker);
        holder.rating.setRating(rating);
        if(date > 0) {
            holder.date.setText(mDateFormat.format(new Date(date)));
        } else {
            holder.date.setText(null);
        }

        mItemCats.put(id, cursor.getString(cursor.getColumnIndex(Tables.Entries.CAT)));
    }

    /**
     * Get the category of the item with the specified ID.
     *
     * @param id The row ID
     * @return The category name
     */
    public String getItemCat(long id) {
        return mItemCats.get(id);
    }

    /**
     * Get the position of an item based on ID.
     *
     * @param id The database ID of the item
     * @return The index of the item
     */
    public int getItemIndex(long id) {
        final Integer index = mItemPositions.get(id);
        if(index == null) {
            return ListView.INVALID_POSITION;
        } else {
            return index;
        }
    }

    /**
     * Holder for View references
     */
    private static class Holder {
        ImageView thumb;
        TextView title;
        TextView maker;
        RatingBar rating;
        TextView date;
    }
}
