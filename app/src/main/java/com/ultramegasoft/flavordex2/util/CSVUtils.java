package com.ultramegasoft.flavordex2.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryHolder;
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.PhotoHolder;
import com.ultramegasoft.flavordex2.widget.RadarHolder;

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
import java.util.TimeZone;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Helpers for importing and exporting CSV files.
 *
 * @author Steve Guidetti
 */
public class CSVUtils {
    private static final String TAG = "CSVUtils";

    /**
     * Formatter for dates in CSV files
     */
    public static final SimpleDateFormat sDateFormatter =
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
    public static void writeCSVHeader(CSVWriter writer) {
        final String[] fields = new String[] {
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
    public static void writeEntry(CSVWriter writer, EntryHolder entry) {
        final ArrayList<String> fields = new ArrayList<>();

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
    private static void addExtras(ArrayList<String> fields, EntryHolder entry) {
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
    private static void addFlavors(ArrayList<String> fields, EntryHolder entry) {
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
    private static void addPhotos(ArrayList<String> fields, EntryHolder entry) {
        final JSONArray array = new JSONArray();
        for(PhotoHolder photo : entry.getPhotos()) {
            array.put(photo.uri.toString());
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
    public static CSVHolder importCSV(Context context, File file) {
        CSVHolder data = null;
        try {
            final CSVReader reader = new CSVReader(new FileReader(file));
            String[] line;
            if((line = reader.readNext()) != null) {
                data = new CSVHolder();
                final List<String> fields = Arrays.asList(line);
                if(!fields.contains(Tables.Entries.TITLE) || !fields.contains(Tables.Entries.CAT)) {
                    return data;
                }

                final ContentResolver cr = context.getContentResolver();

                final HashMap<String, String> rowMap = new HashMap<>();
                EntryHolder entry;
                while((line = reader.readNext()) != null) {
                    rowMap.clear();
                    for(int i = 0; i < line.length; i++) {
                        rowMap.put(fields.get(i), line[i]);
                    }
                    entry = readCSVRow(cr, rowMap);

                    if(TextUtils.isEmpty(entry.title) || TextUtils.isEmpty(entry.catName)) {
                        continue;
                    }

                    data.addEntry(entry, isDuplicate(cr, entry));
                }
            }
            reader.close();
        } catch(IOException e) {
            Log.e(TAG, "Failed to read from file", e);
        }

        return data;
    }

    /**
     * Read a row from a CSV file into an EntryHolder object.
     *
     * @param cr     The ContentResolver
     * @param rowMap A map of column names to values
     * @return The entry
     */
    private static EntryHolder readCSVRow(ContentResolver cr, HashMap<String, String> rowMap) {
        final EntryHolder entry = new EntryHolder();
        entry.title = rowMap.get(Tables.Entries.TITLE);
        entry.catName = rowMap.get(Tables.Entries.CAT);
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

        readExtras(entry, rowMap);
        readFlavors(entry, rowMap);
        readPhotos(cr, entry, rowMap);

        return entry;
    }

    /**
     * Parse the extras field from the CSV row.
     *
     * @param entry  The entry
     * @param rowMap The map of fields from the row
     */
    private static void readExtras(EntryHolder entry, HashMap<String, String> rowMap) {
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
     * Parse the flavors field from the CSV row.
     *
     * @param entry  The entry
     * @param rowMap The map of fields from the row
     */
    private static void readFlavors(EntryHolder entry, HashMap<String, String> rowMap) {
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
     * Parse the photos field from the CSV row.
     *
     * @param cr     The ContentResolver
     * @param entry  The entry
     * @param rowMap The map of fields from the row
     */
    private static void readPhotos(ContentResolver cr, EntryHolder entry,
                                   HashMap<String, String> rowMap) {
        final String photosField = rowMap.get(Tables.Photos.TABLE_NAME);
        if(TextUtils.isEmpty(photosField)) {
            return;
        }

        try {
            final JSONArray array = new JSONArray(photosField);
            Uri uri;
            String hash;
            for(int i = 0; i < array.length(); i++) {
                uri = PhotoUtils.parsePath(array.optString(i));
                hash = PhotoUtils.getMD5Hash(cr, uri);
                if(hash != null) {
                    entry.addPhoto(0, hash, uri);
                }
            }
        } catch(JSONException e) {
            Log.w(TAG, "Failed to parse photos for: " + entry.title);
        }
    }

    /**
     * Is the item a duplicate? Checks the database for an entry with the same name.
     *
     * @param cr    The ContentResolver to use
     * @param entry The entry
     * @return Whether the entry is a possible duplicate
     */
    private static boolean isDuplicate(ContentResolver cr, EntryHolder entry) {
        final String[] projection = new String[] {Tables.Entries._ID};
        final String where = Tables.Entries.TITLE + " = ?";
        final String[] whereArgs = new String[] {entry.title};
        final Cursor cursor =
                cr.query(Tables.Entries.CONTENT_URI, projection, where, whereArgs, null);
        if(cursor != null) {
            try {
                return cursor.moveToFirst();
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
        public final ArrayList<EntryHolder> entries;

        /**
         * List of entries that are possible duplicate
         */
        public final ArrayList<EntryHolder> duplicates;

        public CSVHolder() {
            entries = new ArrayList<>();
            duplicates = new ArrayList<>();
        }

        private CSVHolder(Parcel in) {
            entries = in.createTypedArrayList(EntryHolder.CREATOR);
            duplicates = in.createTypedArrayList(EntryHolder.CREATOR);
        }

        /**
         * Add an entry to the list.
         *
         * @param entry     The entry
         * @param duplicate Whether this is a possible duplicate
         */
        public void addEntry(EntryHolder entry, boolean duplicate) {
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
        }
    }
}
