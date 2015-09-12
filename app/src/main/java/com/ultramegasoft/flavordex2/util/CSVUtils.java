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
import com.ultramegasoft.flavordex2.widget.ExtraFieldHolder;
import com.ultramegasoft.flavordex2.widget.RadarHolder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
    /**
     * Delimiters for field values
     */
    private static final String PHOTO_DELIM = "|";
    private static final String FIELD_DELIM = "|";
    private static final String PAIR_DELIM = ":";

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
        final ArrayList<String> extras = new ArrayList<>();
        for(ExtraFieldHolder extra : entry.getExtras()) {
            extras.add(extra.name + PAIR_DELIM + extra.value);
        }
        fields.add(TextUtils.join(FIELD_DELIM, extras));
    }

    /**
     * Add the flavors field to the list of row fields.
     *
     * @param fields The list of fields for the row
     * @param entry  The entry
     */
    private static void addFlavors(ArrayList<String> fields, EntryHolder entry) {
        final ArrayList<String> flavors = new ArrayList<>();
        for(RadarHolder flavor : entry.getFlavors()) {
            flavors.add(flavor.name + PAIR_DELIM + flavor.value);
        }
        fields.add(TextUtils.join(FIELD_DELIM, flavors));
    }

    /**
     * Add the photos field to the list of row fields.
     *
     * @param fields The list of fields for the row
     * @param entry  The entry
     */
    private static void addPhotos(ArrayList<String> fields, EntryHolder entry) {
        fields.add(TextUtils.join(PHOTO_DELIM, entry.getPhotos()));
    }

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
        for(String extra : extraField.split(FIELD_DELIM)) {
            pair = extra.split(PAIR_DELIM, 2);
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
        for(String flavor : flavorsField.split(FIELD_DELIM)) {
            pair = flavor.split(PAIR_DELIM, 2);
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

        for(String photo : photosField.split(PHOTO_DELIM)) {
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
