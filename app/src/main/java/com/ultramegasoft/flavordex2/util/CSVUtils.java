package com.ultramegasoft.flavordex2.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.ultramegasoft.flavordex2.provider.Tables;
import com.ultramegasoft.flavordex2.widget.EntryHolder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Helpers for importing and exporting CSV files.
 *
 * @author Steve Guidetti
 */
public class CSVUtils {
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
     * Load and parse a CSV file.
     *
     * @param context The Context
     * @param file    The CSV File
     * @return The data from the CSV file
     */
    public static CSVHolder importCSV(Context context, File file) {
        try {
            String[] line;
            final CSVReader reader = new CSVReader(new FileReader(file));

            if((line = reader.readNext()) != null) {
                final List<String> fields = Arrays.asList(line);
                if(!fields.contains(Tables.Entries.TITLE)) {
                    return null;
                }

                final ContentResolver cr = context.getContentResolver();
                final CSVHolder data = new CSVHolder();

                final HashMap<String, String> rowMap = new HashMap<>();
                EntryHolder entry;
                while((line = reader.readNext()) != null) {
                    rowMap.clear();
                    for(int i = 0; i < line.length; i++) {
                        rowMap.put(fields.get(i), line[i]);
                    }
                    entry = readCSVRow(rowMap);

                    if(TextUtils.isEmpty(entry.title)) {
                        continue;
                    }

                    data.addEntry(entry, isDuplicate(cr, entry));
                }

                return data;
            }
        } catch(IOException e) {
            Log.e(CSVUtils.class.getSimpleName(), e.getMessage());
        }
        return null;
    }

    /**
     * Read a row from a CSV file into an EntryHolder object.
     *
     * @param rowMap A map of column names to values
     * @return The entry
     */
    private static EntryHolder readCSVRow(HashMap<String, String> rowMap) {
        final EntryHolder entry = new EntryHolder();
        entry.title = rowMap.get(Tables.Entries.TITLE);
        entry.catName = rowMap.get(Tables.Entries.CAT);
        entry.maker = rowMap.get(Tables.Entries.MAKER);
        entry.origin = rowMap.get(Tables.Entries.ORIGIN);
        entry.location = rowMap.get(Tables.Entries.LOCATION);

        final String dateString = rowMap.get(Tables.Entries.DATE);
        if(dateString != null) {
            try {
                entry.date = sDateFormatter.parse(dateString).getTime();
            } catch(ParseException e) {
                entry.date = System.currentTimeMillis();
            }
        }

        entry.price = rowMap.get(Tables.Entries.PRICE);

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
        readPhotos(entry, rowMap);

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

        String[] pair;
        for(String extra : extraField.split(",")) {
            pair = extra.split(":", 2);
            if(pair.length == 2) {
                entry.addExtra(0, pair[0], false, pair[1]);
            }
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

        String[] pair;
        int value;
        for(String flavor : flavorsField.split(",")) {
            pair = flavor.split(":", 2);
            if(pair.length != 2) {
                continue;
            }
            try {
                value = Integer.valueOf(pair[1]);
                value = value < 0 ? 0 : value;
                value = value > 5 ? 5 : value;
            } catch(NumberFormatException e) {
                continue;
            }
            entry.addFlavor(0, pair[0], value);
        }
    }

    /**
     * Parse the photos field from the CSV row.
     *
     * @param entry  The entry
     * @param rowMap The map of fields from the row
     */
    private static void readPhotos(EntryHolder entry, HashMap<String, String> rowMap) {
        final String photosField = rowMap.get(Tables.Photos.TABLE_NAME);
        if(TextUtils.isEmpty(photosField)) {
            return;
        }

        for(String photo : photosField.split(",")) {
            if(new File(photo).canRead()) {
                entry.addPhoto(0, photo);
            }
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
        final Cursor cursor = cr.query(Tables.Entries.CONTENT_URI, projection, where, whereArgs, null);
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
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
