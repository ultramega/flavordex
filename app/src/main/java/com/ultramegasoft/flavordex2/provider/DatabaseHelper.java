package com.ultramegasoft.flavordex2.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;

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
    private static final int DATABASE_VERSION = 3;

    /**
     * The Context
     */
    private final Context mContext;

    /**
     * @param context The Context
     */
    public DatabaseHelper(Context context) {
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

        if(FlavordexApp.DEVELOPER_MODE) {
            execRawFile(db, R.raw.testdata);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                execRawFile(db, R.raw.upgrade_v2);
                generateUuids(db);
            case 2:
                execRawFile(db, R.raw.upgrade_v3);
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
    private void execRawFile(SQLiteDatabase db, int resource) {
        final InputStream inputStream = mContext.getResources().openRawResource(resource);
        final Scanner scanner = new Scanner(inputStream).useDelimiter("\\n--");
        while(scanner.hasNext()) {
            db.execSQL(scanner.next());
        }
    }

    /**
     * Generate UUIDs for all categories, extras, and entries.
     *
     * @param db The database
     */
    private static void generateUuids(SQLiteDatabase db) {
        String[] columns = new String[] {
                Tables.Cats._ID,
                Tables.Cats.NAME,
                Tables.Cats.PRESET
        };
        Cursor cursor = db.query(Tables.Cats.TABLE_NAME, columns, null, null, null, null, null);
        if(cursor != null) {
            try {
                long id;
                String name;
                final ContentValues values = new ContentValues();
                while(cursor.moveToNext()) {
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Cats._ID));
                    name = cursor.getString(cursor.getColumnIndex(Tables.Cats.NAME));
                    if(cursor.getInt(cursor.getColumnIndex(Tables.Cats.PRESET)) == 1) {
                        values.put(Tables.Cats.UUID, name);
                    } else {
                        values.put(Tables.Cats.UUID, UUID.randomUUID().toString());
                    }
                    db.update(Tables.Cats.TABLE_NAME, values, Tables.Cats._ID + " = " + id, null);
                    generateExtraUuids(db, id, name);
                }
            } finally {
                cursor.close();
            }
        }

        columns = new String[] {Tables.Entries._ID};
        cursor = db.query(Tables.Entries.TABLE_NAME, columns, null, null, null, null, null);
        if(cursor != null) {
            try {
                long id;
                final ContentValues values = new ContentValues();
                while(cursor.moveToNext()) {
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Entries._ID));
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
     * Generate UUIDs for all the extras of a category.
     *
     * @param db      The database
     * @param catId   The database ID of the category
     * @param catName The name of the category
     */
    private static void generateExtraUuids(SQLiteDatabase db, long catId, String catName) {
        final String[] columns = new String[] {
                Tables.Extras._ID,
                Tables.Extras.NAME,
                Tables.Extras.PRESET
        };
        final String where = Tables.Extras.CAT + " = " + catId;
        final Cursor cursor =
                db.query(Tables.Extras.TABLE_NAME, columns, where, null, null, null, null);
        if(cursor != null) {
            try {
                long id;
                String name;
                final ContentValues values = new ContentValues();
                while(cursor.moveToNext()) {
                    id = cursor.getLong(cursor.getColumnIndex(Tables.Extras._ID));
                    if(cursor.getInt(cursor.getColumnIndex(Tables.Extras.PRESET)) == 1) {
                        name = cursor.getString(cursor.getColumnIndex(Tables.Extras.NAME));
                        values.put(Tables.Extras.UUID, catName + name);
                    } else {
                        values.put(Tables.Extras.UUID, UUID.randomUUID().toString());
                    }
                    db.update(Tables.Extras.TABLE_NAME, values, Tables.Extras._ID + " = " + id,
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
    private void insertPreset(SQLiteDatabase db, String name, String[] extras, int flavorRes) {
        final ContentValues values = new ContentValues();

        values.put(Tables.Cats.UUID, name);
        values.put(Tables.Cats.NAME, name);
        values.put(Tables.Cats.PRESET, 1);
        final long id = db.insert(Tables.Cats.TABLE_NAME, null, values);

        values.clear();
        values.put(Tables.Extras.CAT, id);
        values.put(Tables.Extras.PRESET, 1);
        for(int i = 0; i < extras.length; i++) {
            values.put(Tables.Extras.UUID, name + extras[i]);
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
    private void insertBeerPreset(SQLiteDatabase db) {
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
    private void insertWinePreset(SQLiteDatabase db) {
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
    private void insertWhiskeyPreset(SQLiteDatabase db) {
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
    private void insertCoffeePreset(SQLiteDatabase db) {
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
