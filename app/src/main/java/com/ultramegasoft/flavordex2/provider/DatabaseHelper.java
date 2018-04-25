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
package com.ultramegasoft.flavordex2.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.ultramegasoft.flavordex2.BuildConfig;
import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;
import com.ultramegasoft.flavordex2.util.EntryUtils;

import java.io.InputStream;
import java.util.Scanner;
import java.util.UUID;

/**
 * Helper for creating and updating the database.
 *
 * @author Steve Guidetti
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    /**
     * The name of the database file
     */
    public static final String DATABASE_NAME = "flavordex.db";

    /**
     * The current version of the schema, incremented by 1 for each iteration
     */
    private static final int DATABASE_VERSION = 5;

    /**
     * The Context
     */
    @NonNull
    private final Context mContext;

    /**
     * @param context The Context
     */
    DatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        execRawFile(db, R.raw.schema);
        execRawFile(db, R.raw.triggers);
        execRawFile(db, R.raw.views);

        insertBeerPreset(db);
        insertWinePreset(db);
        insertWhiskeyPreset(db);
        insertCoffeePreset(db);

        if(BuildConfig.DEBUG) {
            execRawFile(db, R.raw.testdata);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                execRawFile(db, R.raw.upgrade_v2);
            case 2:
            case 3:
            case 4:
                generateUuids(db);
                execRawFile(db, R.raw.upgrade_v5);
        }

        execRawFile(db, R.raw.triggers);
        execRawFile(db, R.raw.views);
    }

    /**
     * Read and execute a SQL file from the raw resources.
     *
     * @param db       The database
     * @param resource The raw resource ID
     */
    private void execRawFile(@NonNull SQLiteDatabase db, int resource) {
        final InputStream inputStream = mContext.getResources().openRawResource(resource);
        final Scanner scanner = new Scanner(inputStream).useDelimiter("\\n--");
        while(scanner.hasNext()) {
            db.execSQL(scanner.next());
        }
    }

    /**
     * Generate UUIDs for all entries.
     *
     * @param db The database
     */
    private static void generateUuids(@NonNull SQLiteDatabase db) {
        final String[] columns = new String[] {
                Tables.Entries._ID,
                Tables.Entries.UUID
        };
        final Cursor cursor = db.query(Tables.Entries.TABLE_NAME, columns, null, null, null, null,
                null);
        if(cursor != null) {
            try {
                long id;
                String uuid;
                final ContentValues values = new ContentValues();
                while(cursor.moveToNext()) {
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
                    uuid = cursor.getString(cursor.getColumnIndex(Tables.Entries.UUID));
                    if(EntryUtils.isValidUuid(uuid)) {
                        continue;
                    }
                    values.put(Tables.Entries.UUID, UUID.randomUUID().toString());
                    db.update(Tables.Entries.TABLE_NAME, values, Tables.Entries._ID + " = " + id,
                            null);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Insert a preset entry category.
     *
     * @param db        The database
     * @param name      The internal name of the category
     * @param extras    List of internal names of the extra fields
     * @param flavorRes String-array resource ID for the list of flavors
     */
    private void insertPreset(@NonNull SQLiteDatabase db, @NonNull String name,
                              @NonNull String[] extras, int flavorRes) {
        final ContentValues values = new ContentValues();

        values.put(Tables.Cats.NAME, name);
        values.put(Tables.Cats.PRESET, 1);
        final long id = db.insert(Tables.Cats.TABLE_NAME, null, values);

        values.clear();
        values.put(Tables.Extras.CAT, id);
        values.put(Tables.Extras.PRESET, 1);
        for(int i = 0; i < extras.length; i++) {
            values.put(Tables.Extras.NAME, extras[i]);
            values.put(Tables.Extras.POS, i);
            db.insert(Tables.Extras.TABLE_NAME, null, values);
        }

        values.clear();
        final String[] flavors = mContext.getResources().getStringArray(flavorRes);
        values.put(Tables.Flavors.CAT, id);
        for(int i = 0; i < flavors.length; i++) {
            values.put(Tables.Flavors.NAME, flavors[i]);
            values.put(Tables.Flavors.POS, i);
            db.insert(Tables.Flavors.TABLE_NAME, null, values);
        }
    }

    /**
     * Insert beer category preset.
     *
     * @param db The database
     */
    private void insertBeerPreset(@NonNull SQLiteDatabase db) {
        final String[] extras = new String[] {
                Tables.Extras.Beer.STYLE,
                Tables.Extras.Beer.SERVING,
                Tables.Extras.Beer.STATS_IBU,
                Tables.Extras.Beer.STATS_ABV,
                Tables.Extras.Beer.STATS_OG,
                Tables.Extras.Beer.STATS_FG
        };
        insertPreset(db, FlavordexApp.CAT_BEER, extras, R.array.beer_flavor_names);
    }

    /**
     * Insert wine category preset.
     *
     * @param db The database
     */
    private void insertWinePreset(@NonNull SQLiteDatabase db) {
        final String[] extras = new String[] {
                Tables.Extras.Wine.VARIETAL,
                Tables.Extras.Wine.STATS_VINTAGE,
                Tables.Extras.Wine.STATS_ABV
        };
        insertPreset(db, FlavordexApp.CAT_WINE, extras, R.array.wine_flavor_names);
    }

    /**
     * Insert whiskey category preset.
     *
     * @param db The database
     */
    private void insertWhiskeyPreset(@NonNull SQLiteDatabase db) {
        final String[] extras = new String[] {
                Tables.Extras.Whiskey.STYLE,
                Tables.Extras.Whiskey.STATS_AGE,
                Tables.Extras.Whiskey.STATS_ABV
        };
        insertPreset(db, FlavordexApp.CAT_WHISKEY, extras, R.array.whiskey_flavor_names);
    }

    /**
     * Insert coffee category preset.
     *
     * @param db The database
     */
    private void insertCoffeePreset(@NonNull SQLiteDatabase db) {
        final String[] extras = new String[] {
                Tables.Extras.Coffee.ROASTER,
                Tables.Extras.Coffee.ROAST_DATE,
                Tables.Extras.Coffee.GRIND,
                Tables.Extras.Coffee.BREW_METHOD,
                Tables.Extras.Coffee.STATS_DOSE,
                Tables.Extras.Coffee.STATS_MASS,
                Tables.Extras.Coffee.STATS_TEMP,
                Tables.Extras.Coffee.STATS_EXTIME,
                Tables.Extras.Coffee.STATS_TDS,
                Tables.Extras.Coffee.STATS_YIELD
        };
        insertPreset(db, FlavordexApp.CAT_COFFEE, extras, R.array.coffee_flavor_names);
    }
}
