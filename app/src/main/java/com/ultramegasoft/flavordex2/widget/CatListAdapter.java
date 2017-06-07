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
import android.content.res.Resources;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Adapter for listing entry categories in a Spinner.
 *
 * @author Steve Guidetti
 */
public class CatListAdapter extends BaseAdapter implements ThemedSpinnerAdapter {
    /**
     * The Context
     */
    private final Context mContext;

    /**
     * The layout resource ID to use for each list item
     */
    private final int mResource;

    /**
     * The layout resource ID to use for dropdown items
     */
    private final int mDropDownResource;

    /**
     * Helper for the ThemedSpinnerAdapter
     */
    private final ThemedSpinnerAdapter.Helper mHelper;

    /**
     * List of categories sorted by display name
     */
    private final ArrayList<Category> mCats = new ArrayList<>();

    /**
     * Whether to show the All Categories item
     */
    private boolean mShowAllCats;

    /**
     * @param context          The Context
     * @param cursor           The Cursor from the database query
     * @param resource         The layout resource ID to use for each list item
     * @param dropDownResource The layout resource ID to use for dropdown items
     */
    public CatListAdapter(Context context, Cursor cursor, int resource, int dropDownResource) {
        mContext = context;
        mResource = resource;
        mDropDownResource = dropDownResource;
        mHelper = new Helper(context);

        swapCursor(cursor);
    }

    /**
     * @param context  The Context
     * @param cursor   The Cursor from the database query
     * @param resource The layout resource ID to use for each list item
     */
    public CatListAdapter(Context context, Cursor cursor, int resource) {
        this(context, cursor, resource, resource);
    }

    /**
     * Set the Cursor backing this Adapter.
     *
     * @param newCursor The new Cursor
     */
    public final void swapCursor(Cursor newCursor) {
        mCats.clear();

        if(newCursor != null) {
            readCursor(newCursor, mCats);
            if(mShowAllCats) {
                mShowAllCats = false;
                setShowAllCats(true);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Enable or disable the 'All Categories' list item.
     *
     * @param showAllCats Whether to show the All Categories item
     */
    @SuppressWarnings("SameParameterValue")
    public void setShowAllCats(boolean showAllCats) {
        if(showAllCats == mShowAllCats) {
            return;
        }
        if(showAllCats) {
            final String title = mContext.getString(R.string.cat_all);
            int count = 0;
            for(Category cat : mCats) {
                count += cat.numEntries;
            }
            mCats.add(0, new Category(mContext, 0, title, true, count));
        } else {
            mCats.remove(0);
        }
        mShowAllCats = showAllCats;
    }

    /**
     * Is the 'All Categories' list item enabled.
     *
     * @return Whether the All Categories item is enabled
     */
    public boolean getShowAllCats() {
        return mShowAllCats;
    }

    /**
     * Read the new Cursor into the array and sort by name.
     *
     * @param cursor The Cursor
     * @param cats   The ArrayList to add data to
     */
    protected void readCursor(Cursor cursor, ArrayList<Category> cats) {
        cursor.moveToPosition(-1);
        while(cursor.moveToNext()) {
            cats.add(readCursorRow(cursor));
        }
        Collections.sort(cats);
    }

    /**
     * Read the current row into a Category object.
     *
     * @param cursor The Cursor
     * @return A Category read from the database row
     */
    private Category readCursorRow(Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(Tables.Cats._ID));
        final String name = cursor.getString(cursor.getColumnIndex(Tables.Cats.NAME));
        final boolean preset = cursor.getInt(cursor.getColumnIndex(Tables.Cats.PRESET)) == 1;
        final int numEntries = cursor.getInt(cursor.getColumnIndex(Tables.Cats.NUM_ENTRIES));
        return new Category(mContext, id, name, preset, numEntries);
    }

    @Override
    public int getCount() {
        return mCats.size();
    }

    @Override
    public Category getItem(int position) {
        return mCats.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mCats.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(LayoutInflater.from(mContext), mResource, position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(mHelper.getDropDownViewInflater(), mDropDownResource, position,
                convertView, parent);
    }

    protected View createView(LayoutInflater inflater, int layoutId, int position, View convertView,
                              ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(layoutId, parent, false);

            final Holder holder = new Holder();
            holder.textView = (TextView)convertView.findViewById(android.R.id.text1);
            holder.textView2 = (TextView)convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        }

        final Category cat = getItem(position);

        final Holder holder = (Holder)convertView.getTag();
        holder.textView.setText(cat.realName);
        if(holder.textView2 != null) {
            final String entries =
                    mContext.getResources().getQuantityString(R.plurals.entries, cat.numEntries);
            final String numEntries =
                    mContext.getString(R.string.cat_num_entries, cat.numEntries, entries);
            holder.textView2.setText(numEntries);
        }

        return convertView;
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme) {
        mHelper.setDropDownViewTheme(theme);
    }

    @Nullable
    @Override
    public Resources.Theme getDropDownViewTheme() {
        return mHelper.getDropDownViewTheme();
    }

    /**
     * Holder for data about a category which can be compared by name.
     */
    public static class Category implements Comparable<Category> {
        /**
         * The database ID
         */
        public final long id;

        /**
         * The name of the category
         */
        public final String name;

        /**
         * The display name of the category
         */
        public final String realName;

        /**
         * Whether this is a preset category
         */
        public final boolean preset;

        /**
         * The number of entries in this category
         */
        public final int numEntries;

        /**
         * @param context    The Context
         * @param id         The database ID
         * @param name       The name of the category
         * @param preset     Whether this is a preset category
         * @param numEntries The number of entries in this category
         */
        public Category(Context context, long id, String name, boolean preset, int numEntries) {
            this.id = id;
            this.name = name;
            this.realName = FlavordexApp.getRealCatName(context, name);
            this.preset = preset;
            this.numEntries = numEntries;
        }

        @Override
        public String toString() {
            return realName;
        }

        @Override
        public int compareTo(@NonNull Category another) {
            return realName.compareTo(another.realName);
        }
    }

    /**
     * Holder for View references
     */
    private static class Holder {
        public TextView textView;
        public TextView textView2;
    }
}
