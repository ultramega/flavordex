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
 * Custom adapter for the main entry list.
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
     * Map of item IDs to item types
     */
    private final HashMap<Long, String> mItemTypes = new HashMap<>();

    /**
     * Map of item IDs to their position index in the list
     */
    private final HashMap<Long, Integer> mItemPositions = new HashMap<>();

    /**
     * @param context The context
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
        return LayoutInflater.from(context).inflate(R.layout.entry_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ImageView thumbView = (ImageView)view.findViewById(R.id.thumb);
        final TextView titleView = (TextView)view.findViewById(R.id.title);
        final TextView makerView = (TextView)view.findViewById(R.id.maker);
        final RatingBar ratingBar = (RatingBar)view.findViewById(R.id.rating);
        final TextView dateView = (TextView)view.findViewById(R.id.date);

        final long id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
        final String title = cursor.getString(cursor.getColumnIndex(Tables.Entries.TITLE));
        final String maker = cursor.getString(cursor.getColumnIndex(Tables.Entries.MAKER));
        final float rating = cursor.getFloat(cursor.getColumnIndex(Tables.Entries.RATING));
        final long date = cursor.getLong(cursor.getColumnIndex(Tables.Entries.DATE));

        sThumbLoader.load(thumbView, id);
        titleView.setText(title);
        makerView.setText(maker);
        ratingBar.setRating(rating);
        if(date > 0) {
            dateView.setText(mDateFormat.format(new Date(date)));
        } else {
            dateView.setText(null);
        }

        mItemTypes.put(id, cursor.getString(cursor.getColumnIndex(Tables.Entries.TYPE)));
    }

    /**
     * Get the type of the item with the specified id.
     *
     * @param id The row id
     * @return The type name
     */
    public String getItemType(long id) {
        return mItemTypes.get(id);
    }

    /**
     * Get the position of an item based on ID.
     *
     * @param id The database id of the item
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
}
