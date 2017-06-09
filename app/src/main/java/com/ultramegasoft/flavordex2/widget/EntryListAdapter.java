/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
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
import java.util.Locale;

/**
 * Custom Adapter for the main journal entry list.
 *
 * @author Steve Guidetti
 */
@SuppressWarnings("unused")
public class EntryListAdapter extends CursorAdapter {
    /**
     * Background loader for thumbnails
     */
    private static final BackgroundThumbLoader<Long> sThumbLoader =
            new BackgroundThumbLoader<Long>() {
                @Nullable
                @Override
                protected Bitmap getBitmap(@NonNull Thumb thumb) {
                    return PhotoUtils.getThumb(thumb.get().getContext(), (Long)thumb.key);
                }
            };

    /**
     * Formatter for the date field
     */
    @NonNull
    private final SimpleDateFormat mDateFormat;

    /**
     * Map of item IDs to their position index in the list
     */
    private final LongSparseArray<Integer> mItemPositions = new LongSparseArray<>();

    /**
     * Whether multiple choice mode is enabled
     */
    private boolean mMultiChoice = false;

    /**
     * @param context The Context
     */
    public EntryListAdapter(@NonNull Context context) {
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
    public int getItemViewType(int position) {
        return mMultiChoice ? 1 : 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
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

        if(mMultiChoice) {
            view.findViewById(R.id.checkbox).setVisibility(View.VISIBLE);
            holder.thumb.setVisibility(View.GONE);
        } else {
            ((CheckableEntryListItem)view).setMultiChoice(false);
        }

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

        if(!mMultiChoice) {
            sThumbLoader.load(holder.thumb, id);
        }
        holder.title.setText(title);
        holder.maker.setText(maker);
        holder.rating.setRating(rating);
        if(date > 0) {
            holder.date.setText(mDateFormat.format(new Date(date)));
        } else {
            holder.date.setText(null);
        }
    }

    /**
     * Is the Adapter in multiple choice mode?
     *
     * @return Whether the Adapter is in multiple choice mode
     */
    public boolean getMultiChoiceMode() {
        return mMultiChoice;
    }

    /**
     * Set multiple choice mode for the Adapter.
     *
     * @param multiChoice Whether to allow multiple selections
     */
    public void setMultiChoiceMode(boolean multiChoice) {
        if(mMultiChoice != multiChoice) {
            mMultiChoice = multiChoice;
            notifyDataSetChanged();
        }
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
