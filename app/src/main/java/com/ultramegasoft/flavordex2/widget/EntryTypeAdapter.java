package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
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
import java.util.HashMap;

/**
 * Adapter for listing entry types in a Spinner.
 *
 * @author Steve Guidetti
 */
public class EntryTypeAdapter extends BaseAdapter {
    /**
     * The context
     */
    private final Context mContext;

    /**
     * The layout resource id to use for each list item
     */
    private final int mLayoutId;

    /**
     * The id for the TextView within the layout
     */
    private final int mTextViewId;

    /**
     * Object to show as the first item in the list
     */
    private Type mHeader;

    /**
     * Object to show as the last item in the list
     */
    private Type mFooter;

    /**
     * Map of preset types to their display names
     */
    private final HashMap<String, String> mPresets = new HashMap<>();

    /**
     * List of types sorted by display name
     */
    private final ArrayList<Type> mTypes = new ArrayList<>();

    /**
     * @param context     The context
     * @param cursor      The cursor from the database query
     * @param layoutResId The layout resource id to use for each list item
     * @param textViewId  The id for the TextView within the layout
     */
    public EntryTypeAdapter(Context context, Cursor cursor, int layoutResId, int textViewId) {
        mContext = context;
        mLayoutId = layoutResId;
        mTextViewId = textViewId;

        mPresets.put(FlavordexApp.TYPE_BEER, context.getString(R.string.type_beer));
        mPresets.put(FlavordexApp.TYPE_WINE, context.getString(R.string.type_wine));
        mPresets.put(FlavordexApp.TYPE_WHISKEY, context.getString(R.string.type_whiskey));
        mPresets.put(FlavordexApp.TYPE_COFFEE, context.getString(R.string.type_coffee));

        swapCursor(cursor);
    }

    /**
     * Set the first item to display in the list.
     *
     * @param resId The string resource id for the text to display
     */
    public void setHeader(int resId) {
        setHeader(mContext.getString(resId));
    }

    /**
     * Set the first item to display in the list.
     *
     * @param header The text to display or null to remove the header
     */
    public void setHeader(String header) {
        updateHeader(new Type(0, header));
        notifyDataSetChanged();
    }

    /**
     * Set the last item to display in the list.
     *
     * @param resId The string resource id for the text to display
     */
    public void setFooter(int resId) {
        setFooter(mContext.getString(resId));
    }

    /**
     * Set the last item to display in the list.
     *
     * @param footer The text to display or null to remove the footer
     */
    public void setFooter(String footer) {
        updateFooter(new Type(0, footer));
        notifyDataSetChanged();
    }

    /**
     * Set the cursor backing this adapter.
     *
     * @param newCursor The new cursor
     */
    public void swapCursor(Cursor newCursor) {
        mTypes.clear();

        if(newCursor != null) {
            newCursor.moveToPosition(-1);
            while(newCursor.moveToNext()) {
                final long id = newCursor.getLong(newCursor.getColumnIndex(Tables.Types._ID));
                final String name = getRealName(newCursor.getString(newCursor
                        .getColumnIndex(Tables.Types.NAME)));
                mTypes.add(new Type(id, name));
            }
            Collections.sort(mTypes);
        }

        updateHeader(mHeader);
        updateFooter(mFooter);

        notifyDataSetChanged();
    }

    /**
     * Get the display name from a database name. Ths will translate the internal name of presets to
     * their display name.
     *
     * @param name The name from the database
     * @return The display name
     */
    private String getRealName(String name) {
        final String realName = mPresets.get(name);
        if(realName != null) {
            return realName;
        }
        return name;
    }

    /**
     * Add, remove, or modify the header item.
     *
     * @param header The item to show as the first item in the list or null to remove
     */
    private void updateHeader(Type header) {
        if(mHeader != null) {
            mTypes.remove(mHeader);
        }
        if(header != null) {
            mTypes.add(0, header);
        }
        mHeader = header;
    }

    /**
     * Add, remove, or modify the footer item.
     *
     * @param footer The item to show as the last item in the list or null to remove
     */
    private void updateFooter(Type footer) {
        if(mFooter != null) {
            mTypes.remove(mFooter);
        }
        if(footer != null) {
            mTypes.add(footer);
        }
        mFooter = footer;
    }

    @Override
    public int getCount() {
        return mTypes.size();
    }

    @Override
    public Object getItem(int position) {
        return mTypes.get(position).name;
    }

    @Override
    public long getItemId(int position) {
        return mTypes.get(position).id;
    }

    /**
     * Get the position of an item based on ID.
     *
     * @param id The database id of the item
     * @return The index of the item, or -1 if the item does not exist
     */
    public int getItemIndex(long id) {
        for(int i = 0; i < mTypes.size(); i++) {
            if(mTypes.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
        }

        final TextView textView = (TextView)convertView.findViewById(mTextViewId);
        textView.setText(mTypes.get(position).name);

        return convertView;
    }

    /**
     * Holder for data about a type which can be compared by name.
     */
    private static class Type implements Comparable<Type> {
        /**
         * The database id
         */
        public long id;

        /**
         * The name of the type
         */
        public String name;

        /**
         * @param id   The database id
         * @param name The name of the type
         */
        public Type(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int compareTo(@NonNull Type another) {
            return name.compareTo(another.name);
        }
    }
}
