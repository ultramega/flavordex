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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Array adapter that equates special characters to normal characters.
 *
 * @author Steve Guidetti
 * @see android.widget.ArrayAdapter
 */
public class SpecialArrayAdapter<T> extends BaseAdapter implements Filterable {
    private List<T> mObjects;
    private final Object mLock = new Object();

    private int mResource;
    private int mDropDownResource;

    private int mFieldId = 0;

    private boolean mNotifyOnChange = true;

    private Context mContext;

    private ArrayList<T> mOriginalValues;
    private SpecialArrayFilter mFilter;

    private LayoutInflater mInflater;

    /**
     * Map of special characters to simple characters
     */
    private static final HashMap<Character, Character> sNormalizeMap =
            new HashMap<Character, Character>() {
                {
                    put('à', 'a');
                    put('á', 'a');
                    put('â', 'a');
                    put('ã', 'a');
                    put('ä', 'a');
                    put('è', 'e');
                    put('é', 'e');
                    put('ê', 'e');
                    put('ë', 'e');
                    put('ì', 'i');
                    put('í', 'i');
                    put('î', 'i');
                    put('ï', 'i');
                    put('ñ', 'n');
                    put('ò', 'o');
                    put('ó', 'o');
                    put('ô', 'o');
                    put('õ', 'o');
                    put('ö', 'o');
                    put('ù', 'u');
                    put('ú', 'u');
                    put('û', 'u');
                    put('ü', 'u');
                    put('ý', 'y');
                    put('ÿ', 'y');
                }
            };

    public SpecialArrayAdapter(Context context, int textViewResourceId) {
        init(context, textViewResourceId, 0, new ArrayList<T>());
    }

    public SpecialArrayAdapter(Context context, int resource, int textViewResourceId) {
        init(context, resource, textViewResourceId, new ArrayList<T>());
    }

    public SpecialArrayAdapter(Context context, int textViewResourceId, T[] objects) {
        init(context, textViewResourceId, 0, Arrays.asList(objects));
    }

    public SpecialArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        init(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    public SpecialArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
        init(context, textViewResourceId, 0, objects);
    }

    public SpecialArrayAdapter(Context context, int resource, int textViewResourceId,
                               List<T> objects) {
        init(context, resource, textViewResourceId, objects);
    }

    private void init(Context context, int resource, int textViewResourceId, List<T> objects) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = mDropDownResource = resource;
        mObjects = objects;
        mFieldId = textViewResourceId;
    }

    public void add(T object) {
        if(mOriginalValues != null) {
            synchronized(mLock) {
                mOriginalValues.add(object);
            }
        } else {
            mObjects.add(object);
        }
        if(mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void insert(T object, int index) {
        if(mOriginalValues != null) {
            synchronized(mLock) {
                mOriginalValues.add(index, object);
            }
        } else {
            mObjects.add(index, object);
        }
        if(mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void remove(T object) {
        if(mOriginalValues != null) {
            synchronized(mLock) {
                mOriginalValues.remove(object);
            }
        } else {
            mObjects.remove(object);
        }
        if(mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void clear() {
        if(mOriginalValues != null) {
            synchronized(mLock) {
                mOriginalValues.clear();
            }
        } else {
            mObjects.clear();
        }
        if(mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void sort(Comparator<? super T> comparator) {
        Collections.sort(mObjects, comparator);
        if(mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }

    public Context getContext() {
        return mContext;
    }

    public int getCount() {
        return mObjects.size();
    }

    public T getItem(int position) {
        return mObjects.get(position);
    }

    public int getPosition(T item) {
        return mObjects.indexOf(item);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent,
                                        int resource) {
        final View view;
        final TextView text;

        if(convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        try {
            if(mFieldId == 0) {
                text = (TextView)view;
            } else {
                text = (TextView)view.findViewById(mFieldId);
            }
        } catch(ClassCastException e) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        text.setText(getItem(position).toString());

        return view;
    }

    public void setDropDownViewResource(int resource) {
        mDropDownResource = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mDropDownResource);
    }

    public static SpecialArrayAdapter<CharSequence> createFromResource(Context context,
                                                                       int textArrayResId,
                                                                       int textViewResId) {
        final CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
        return new SpecialArrayAdapter<>(context, textViewResId, strings);
    }

    public Filter getFilter() {
        if(mFilter == null) {
            mFilter = new SpecialArrayFilter();
        }
        return mFilter;
    }

    /**
     * Custom filter that equates special characters to simple characters.
     */
    private class SpecialArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final FilterResults results = new FilterResults();

            if(mOriginalValues == null) {
                synchronized(mLock) {
                    mOriginalValues = new ArrayList<>(mObjects);
                }
            }

            if(prefix == null || prefix.length() == 0) {
                synchronized(mLock) {
                    final ArrayList<T> list = new ArrayList<>(mOriginalValues);
                    results.values = list;
                    results.count = list.size();
                }
            } else {
                final String prefixString = normalizeString(prefix.toString());

                final ArrayList<T> values = mOriginalValues;
                final int count = values.size();

                final ArrayList<T> newValues = new ArrayList<>(count);

                for(int i = 0; i < count; i++) {
                    final T value = values.get(i);
                    final String valueText = normalizeString(value.toString());

                    if(valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = valueText.split(" ");
                        for(String word : words) {
                            if(word.startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults result) {
            mObjects = (List<T>)result.values;
            if(result.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

        /**
         * Convert special characters in the string to simple characters.
         *
         * @param original The string to convert
         * @return The converted string
         */
        private String normalizeString(String original) {
            final char[] chars = original.toLowerCase().toCharArray();
            for(int i = 0; i < chars.length; i++) {
                final Character replace = sNormalizeMap.get(chars[i]);
                if(replace != null) {
                    chars[i] = replace;
                }
            }
            return new String(chars);
        }
    }
}
