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
package com.ultramegasoft.flavordex2.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.util.csv.CSVReader;
import com.ultramegasoft.flavordex2.util.csv.CSVWriter;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;
import com.ultramegasoft.radarchart.RadarHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Helpers for importing and exporting CSV files.
 *
 * @author Steve Guidetti
 */
public class CSVUtils {
    private static final String TAG = "CSVUtils";

    private static final List<String> LEGACY_FIELDS_COMMON = Arrays.asList(
            "title",
            "maker",
            "origin",
            "location",
            "date",
            "price",
            "rating",
            "notes",
            "flavors",
            "photos"
    );

    private static final List<String> LEGACY_FIELDS_BEER = Arrays.asList(
            "style",
            "serving",
            "stats_ibu",
            "stats_abv",
            "stats_og",
            "stats_fg"
    );

    private static final List<String> LEGACY_FIELDS_COFFEE = Arrays.asList(
            "roaster",
            "roast_date",
            "grind",
            "brew_method",
            "stats_dose",
            "stats_mass",
            "stats_temp",
            "stats_extime",
            "stats_tds",
            "stats_yield"
    );

    private static final List<String> LEGACY_FIELDS_WHISKEY = Arrays.asList(
            "style",
            "stats_age",
            "stats_abv"
    );

    private static final List<String> LEGACY_FIELDS_WINE = Arrays.asList(
            "varietal",
            "stats_vintage",
            "stats_abv"
    );

    /**
     * Formatter for dates in CSV files
     */
    private static final SimpleDateFormat sDateFormatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US) {
                {
                    setTimeZone(TimeZone.getTimeZone("UTC"));
                }
            };

    /**
     * Write the header row to a CSV file.
     *
     * @param writer An open CSVWriter
     */
    public static void writeCSVHeader(@NonNull CSVWriter writer) {
        final String[] fields = new String[] {
                Tables.Entries.UUID,
                Tables.Entries.TITLE,
                Tables.Entries.CAT,
                Tables.Entries.MAKER,
                Tables.Entries.ORIGIN,
                Tables.Entries.PRICE,
                Tables.Entries.LOCATION,
                Tables.Entries.DATE,
                Tables.Entries.RATING,
                Tables.Entries.NOTES,
                Tables.Extras.TABLE_NAME,
                Tables.Flavors.TABLE_NAME,
                Tables.Photos.TABLE_NAME
        };
        writer.writeNext(fields);
    }

    /**
     * Write an entry to the CSV file.
     *
     * @param writer An open CSVWriter
     * @param entry  The entry
     */
    public static void writeEntry(@NonNull CSVWriter writer, @NonNull EntryHolder entry) {
        final ArrayList<String> fields = new ArrayList<>();

        fields.add(entry.uuid);
        fields.add(entry.title);
        fields.add(entry.catName);
        fields.add(entry.maker);
        fields.add(entry.origin);
        fields.add(entry.price);
        fields.add(entry.location);
        fields.add(sDateFormatter.format(new Date(entry.date)));
        fields.add(String.valueOf(entry.rating));
        fields.add(entry.notes);

        addExtras(fields, entry);
        addFlavors(fields, entry);
        addPhotos(fields, entry);

        writer.writeNext(fields.toArray(new String[fields.size()]));
    }

    /**
     * Add the extras field to the list of row fields.
     *
     * @param fields The list of fields for the row
     * @param entry  The entry
     */
    private static void addExtras(@NonNull ArrayList<String> fields, @NonNull EntryHolder entry) {
        final JSONObject object = new JSONObject();
        for(ExtraFieldHolder extra : entry.getExtras()) {
            if(extra.preset || !TextUtils.isEmpty(extra.value)) {
                try {
                    object.put(extra.name, extra.value);
                } catch(JSONException ignored) {
                }
            }
        }
        fields.add(object.toString());
    }

    /**
     * Add the flavors field to the list of row fields.
     *
     * @param fields The list of fields for the row
     * @param entry  The entry
     */
    private static void addFlavors(@NonNull ArrayList<String> fields, @NonNull EntryHolder entry) {
        final JSONObject object = new JSONObject();
        for(RadarHolder flavor : entry.getFlavors()) {
            try {
                object.put(flavor.name, flavor.value);
            } catch(JSONException ignored) {
            }
        }
        fields.add(object.toString());
    }

    /**
     * Add the photos field to the list of row fields.
     *
     * @param fields The list of fields for the row
     * @param entry  The entry
     */
    private static void addPhotos(@NonNull ArrayList<String> fields, @NonNull EntryHolder entry) {
        final JSONArray array = new JSONArray();
        for(PhotoHolder photo : entry.getPhotos()) {
            array.put(PhotoUtils.getPathString(photo.uri));
        }
        fields.add(array.toString());
    }

    /**
     * Load and parse a CSV file.
     *
     * @param context The Context
     * @param file    The CSV File
     * @return The data from the CSV file
     */
    @Nullable
    public static CSVHolder importCSV(@NonNull Context context, @NonNull File file) {
        CSVHolder data = null;
        try {
            final CSVReader reader = new CSVReader(new FileReader(file));
            String[] line;
            if((line = reader.readNext()) != null) {
                data = new CSVHolder();
                final List<String> fields = Arrays.asList(line);
                if(!fields.contains(Tables.Entries.TITLE)) {
                    return data;
                }
                data.hasCategory = fields.contains(Tables.Entries.CAT);
                detectFormat(data, fields);

                final HashMap<String, String> rowMap = new HashMap<>();
                while((line = reader.readNext()) != null) {
                    rowMap.clear();
                    for(int i = 0; i < line.length; i++) {
                        rowMap.put(fields.get(i), line[i]);
                    }
                    readCSVRow(context, data, rowMap);
                }
            }
            reader.close();
        } catch(IOException e) {
            Log.e(TAG, "Failed to read from file", e);
        }

        return data;
    }

    /**
     * Determine the format of the CSV file.
     *
     * @param holder The CSVHolder
     * @param fields The list of field names
     */
    private static void detectFormat(@NonNull CSVHolder holder, @NonNull List<String> fields) {
        if(fields.containsAll(LEGACY_FIELDS_COMMON)) {
            if(fields.containsAll(LEGACY_FIELDS_BEER)) {
                holder.legacyFormat = FlavordexApp.CAT_BEER;
            } else if(fields.containsAll(LEGACY_FIELDS_COFFEE)) {
                holder.legacyFormat = FlavordexApp.CAT_COFFEE;
            } else if(fields.containsAll(LEGACY_FIELDS_WHISKEY)) {
                holder.legacyFormat = FlavordexApp.CAT_WHISKEY;
            } else if(fields.containsAll(LEGACY_FIELDS_WINE)) {
                holder.legacyFormat = FlavordexApp.CAT_WINE;
            } else {
                return;
            }
            holder.hasCategory = true;
        }
    }

    /**
     * Read a row from a CSV file into an EntryHolder object.
     *
     * @param context The Context
     * @param holder  The CSVHolder
     * @param rowMap  A map of column names to values
     */
    private static void readCSVRow(@NonNull Context context, @NonNull CSVHolder holder,
                                   @NonNull HashMap<String, String> rowMap) {
        final EntryHolder entry = new EntryHolder();

        entry.title = rowMap.get(Tables.Entries.TITLE);
        if(holder.legacyFormat == null) {
            entry.uuid = rowMap.get(Tables.Entries.UUID);
            entry.catName = rowMap.get(Tables.Entries.CAT);
            if(TextUtils.isEmpty(entry.title)
                    || (holder.hasCategory && TextUtils.isEmpty(entry.catName))) {
                return;
            }
        } else {
            entry.catName = holder.legacyFormat;
        }
        entry.maker = rowMap.get(Tables.Entries.MAKER);
        entry.origin = rowMap.get(Tables.Entries.ORIGIN);
        entry.price = rowMap.get(Tables.Entries.PRICE);
        entry.location = rowMap.get(Tables.Entries.LOCATION);

        final String dateString = rowMap.get(Tables.Entries.DATE);
        if(dateString != null) {
            try {
                entry.date = sDateFormatter.parse(dateString).getTime();
            } catch(ParseException e) {
                entry.date = System.currentTimeMillis();
            }
        }

        final String ratingString = rowMap.get(Tables.Entries.RATING);
        if(ratingString != null) {
            try {
                entry.rating = Float.valueOf(ratingString);
                entry.rating = entry.rating < 0 ? 0 : entry.rating;
                entry.rating = entry.rating > 5 ? 5 : entry.rating;
            } catch(NumberFormatException ignored) {
            }
        }

        entry.notes = rowMap.get(Tables.Entries.NOTES);

        if(holder.legacyFormat != null) {
            readLegacyExtras(entry, rowMap, holder.legacyFormat);
            readLegacyFlavors(context, entry, rowMap, holder.legacyFormat);
            readLegacyPhotos(context, entry, rowMap);
        } else {
            readFlavors(entry, rowMap);
            readPhotos(entry, rowMap);
        }
        readExtras(entry, rowMap);

        holder.addEntry(entry, isDuplicate(context, entry));
    }

    /**
     * Parse the extras field from the CSV row.
     *
     * @param entry  The entry
     * @param rowMap The map of fields from the row
     */
    private static void readExtras(@NonNull EntryHolder entry,
                                   @NonNull HashMap<String, String> rowMap) {
        final String extraField = rowMap.get(Tables.Extras.TABLE_NAME);
        if(TextUtils.isEmpty(extraField)) {
            return;
        }

        try {
            final JSONObject object = new JSONObject(extraField);
            final Iterator<String> iterator = object.keys();
            String name;
            while(iterator.hasNext()) {
                name = iterator.next();
                entry.addExtra(0, name, false, object.optString(name));
            }
        } catch(JSONException e) {
            Log.w(TAG, "Failed to parse extra fields for: " + entry.title, e);
        }
    }

    /**
     * Read the columns of a legacy format into extra fields.
     *
     * @param entry        The entry
     * @param rowMap       The map of fields from the row
     * @param legacyFormat The legacy format
     */
    private static void readLegacyExtras(@NonNull EntryHolder entry,
                                         @NonNull HashMap<String, String> rowMap,
                                         @NonNull String legacyFormat) {
        for(Map.Entry<String, String> field : rowMap.entrySet()) {
            if(LEGACY_FIELDS_COMMON.contains(field.getKey())) {
                continue;
            }
            switch(legacyFormat) {
                case FlavordexApp.CAT_BEER:
                    if(!LEGACY_FIELDS_BEER.contains(field.getKey())) {
                        continue;
                    }
                    break;
                case FlavordexApp.CAT_COFFEE:
                    if(!LEGACY_FIELDS_COFFEE.contains(field.getKey())) {
                        continue;
                    }
                    break;
                case FlavordexApp.CAT_WHISKEY:
                    if(!LEGACY_FIELDS_WHISKEY.contains(field.getKey())) {
                        continue;
                    }
                    break;
                case FlavordexApp.CAT_WINE:
                    if(!LEGACY_FIELDS_WINE.contains(field.getKey())) {
                        continue;
                    }
                    break;
                default:
                    return;
            }
            entry.addExtra(0, "_" + field.getKey(), true, field.getValue());
        }
    }

    /**
     * Parse the flavors field from the CSV row.
     *
     * @param entry  The entry
     * @param rowMap The map of fields from the row
     */
    private static void readFlavors(@NonNull EntryHolder entry,
                                    @NonNull HashMap<String, String> rowMap) {
        final String flavorsField = rowMap.get(Tables.Flavors.TABLE_NAME);
        if(TextUtils.isEmpty(flavorsField)) {
            return;
        }

        try {
            final JSONObject object = new JSONObject(flavorsField);
            final Iterator<String> iterator = object.keys();
            String name;
            int value;
            while(iterator.hasNext()) {
                name = iterator.next();
                value = object.optInt(name);
                value = value < 0 ? 0 : value;
                value = value > 5 ? 5 : value;
                entry.addFlavor(name, value);
            }
        } catch(JSONException e) {
            Log.w(TAG, "Failed to parse flavors for: " + entry.title);
        }
    }

    /**
     * Read the flavors from a legacy formatted CSV row.
     *
     * @param context      The Context
     * @param entry        The entry
     * @param rowMap       The map of fields from the row
     * @param legacyFormat The legacy format
     */
    private static void readLegacyFlavors(@NonNull Context context, @NonNull EntryHolder entry,
                                          @NonNull HashMap<String, String> rowMap,
                                          @NonNull String legacyFormat) {
        final String flavorsField = rowMap.get(Tables.Flavors.TABLE_NAME);
        if(TextUtils.isEmpty(flavorsField)) {
            return;
        }

        final String[] flavorNames;
        switch(legacyFormat) {
            case FlavordexApp.CAT_BEER:
                flavorNames = context.getResources().getStringArray(R.array.beer_flavor_names);
                break;
            case FlavordexApp.CAT_COFFEE:
                flavorNames = context.getResources().getStringArray(R.array.coffee_flavor_names);
                break;
            case FlavordexApp.CAT_WHISKEY:
                flavorNames = context.getResources().getStringArray(R.array.whiskey_flavor_names);
                break;
            case FlavordexApp.CAT_WINE:
                flavorNames = context.getResources().getStringArray(R.array.wine_flavor_names);
                break;
            default:
                return;
        }

        final String[] flavors = flavorsField.split(",");
        if(flavors.length != flavorNames.length) {
            return;
        }

        for(int i = 0; i < flavors.length; i++) {
            int value = Integer.valueOf(flavors[i].trim());
            value = value < 0 ? 0 : value;
            value = value > 5 ? 5 : value;
            entry.addFlavor(flavorNames[i], value);
        }
    }

    /**
     * Parse the photos field from the CSV row.
     *
     * @param entry  The entry
     * @param rowMap The map of fields from the row
     */
    private static void readPhotos(@NonNull EntryHolder entry,
                                   @NonNull HashMap<String, String> rowMap) {
        final String photosField = rowMap.get(Tables.Photos.TABLE_NAME);
        if(TextUtils.isEmpty(photosField)) {
            return;
        }

        try {
            final JSONArray array = new JSONArray(photosField);
            Uri uri;
            for(int i = 0; i < array.length(); i++) {
                uri = PhotoUtils.parsePath(array.optString(i));
                if(uri != null) {
                    entry.addPhoto(0, null, uri);
                }
            }
        } catch(JSONException e) {
            Log.w(TAG, "Failed to parse photos for: " + entry.title);
        }
    }

    /**
     * Read the photos from a legacy formatted CSV row.
     *
     * @param context The Context
     * @param entry   The entry
     * @param rowMap  The map of fields from the row
     */
    private static void readLegacyPhotos(@NonNull Context context, @NonNull EntryHolder entry,
                                         @NonNull HashMap<String, String> rowMap) {
        final String photosField = rowMap.get(Tables.Photos.TABLE_NAME);
        if(TextUtils.isEmpty(photosField)) {
            return;
        }

        final ContentResolver cr = context.getContentResolver();
        String path;
        Uri uri;
        String hash;
        for(String photo : photosField.split(",")) {
            path = photo.trim().substring(0, photo.indexOf('|'));
            uri = PhotoUtils.parsePath(path);
            if(uri == null) {
                continue;
            }
            hash = PhotoUtils.getMD5Hash(cr, uri);
            if(hash != null) {
                entry.addPhoto(0, hash, uri);
            }
        }
    }

    /**
     * Is the item a duplicate? Checks the database for an entry with the same UUID.
     *
     * @param context The Context
     * @param entry   The entry
     * @return Whether the entry is a duplicate
     */
    private static boolean isDuplicate(@NonNull Context context, @NonNull EntryHolder entry) {
        if(entry.uuid == null) {
            return false;
        }
        final ContentResolver cr = context.getContentResolver();
        final String[] projection = new String[] {Tables.Entries._ID};
        final String where = Tables.Entries.UUID + " = ?";
        final String[] whereArgs = new String[] {entry.uuid};
        final Cursor cursor =
                cr.query(Tables.Entries.CONTENT_URI, projection, where, whereArgs, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    entry.uuid = null;
                    return true;
                }
            } finally {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Holds data for a CSV file.
     */
    public static class CSVHolder implements Parcelable {
        public static final Creator<CSVHolder> CREATOR = new Creator<CSVHolder>() {
            @Override
            public CSVHolder createFromParcel(Parcel in) {
                return new CSVHolder(in);
            }

            @Override
            public CSVHolder[] newArray(int size) {
                return new CSVHolder[size];
            }
        };

        /**
         * The list of entries
         */
        @NonNull
        public final ArrayList<EntryHolder> entries;

        /**
         * List of entries that are possible duplicate
         */
        @NonNull
        public final ArrayList<EntryHolder> duplicates;

        /**
         * Whether the CSV file has a category column
         */
        public boolean hasCategory;

        /**
         * The legacy format if detected
         */
        @Nullable
        String legacyFormat;

        CSVHolder() {
            entries = new ArrayList<>();
            duplicates = new ArrayList<>();
        }

        private CSVHolder(Parcel in) {
            entries = in.createTypedArrayList(EntryHolder.CREATOR);
            duplicates = in.createTypedArrayList(EntryHolder.CREATOR);
            hasCategory = in.readInt() == 1;
            legacyFormat = in.readString();
        }

        /**
         * Add an entry to the list.
         *
         * @param entry     The entry
         * @param duplicate Whether this is a possible duplicate
         */
        void addEntry(@NonNull EntryHolder entry, boolean duplicate) {
            entries.add(entry);
            if(duplicate) {
                duplicates.add(entry);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedList(entries);
            dest.writeTypedList(duplicates);
            dest.writeInt(hasCategory ? 1 : 0);
            dest.writeString(legacyFormat);
        }
    }
}
