package com.ultramegasoft.flavordex2.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ultramegasoft.flavordex2.FlavordexApp;
import com.ultramegasoft.flavordex2.R;

import java.io.InputStream;
import java.util.Scanner;

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
    private static final int DATABASE_VERSION = 4;

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
        StringBuilder query;
        switch(oldVersion) {
            case 1:
                query = new StringBuilder("ALTER TABLE `").append(Tables.Photos.TABLE_NAME)
                        .append("` ADD COLUMN `").append(Tables.Photos.DRIVE_ID).append("` TEXT;");
                db.execSQL(query.toString());
            case 2:
                query = new StringBuilder("DROP VIEW IF EXISTS `").append(Tables.Entries.VIEW_NAME)
                        .append("`;");
                db.execSQL(query.toString());
                query = new StringBuilder("DROP VIEW IF EXISTS `")
                        .append(Tables.EntriesExtras.VIEW_NAME).append("`;");
                db.execSQL(query.toString());

                query = new StringBuilder("ALTER TABLE `").append(Tables.Entries.TABLE_NAME)
                        .append("` ADD COLUMN `").append(Tables.Entries.UPDATED)
                        .append("` INTEGER;");
                db.execSQL(query.toString());
                query = new StringBuilder("ALTER TABLE `").append(Tables.Entries.TABLE_NAME)
                        .append("` ADD COLUMN `").append(Tables.Entries.REMOTE_ID)
                        .append("` INTEGER DEFAULT 0;");
                db.execSQL(query.toString());

                query = new StringBuilder("ALTER TABLE `").append(Tables.Extras.TABLE_NAME)
                        .append("` ADD COLUMN `").append(Tables.Extras.REMOTE_ID)
                        .append("` INTEGER DEFAULT 0;");
                db.execSQL(query.toString());

                query = new StringBuilder("ALTER TABLE `").append(Tables.Cats.TABLE_NAME)
                        .append("` ADD COLUMN `").append(Tables.Entries.UPDATED)
                        .append("` INTEGER;");
                db.execSQL(query.toString());
                query = new StringBuilder("ALTER TABLE `").append(Tables.Cats.TABLE_NAME)
                        .append("` ADD COLUMN `").append(Tables.Entries.REMOTE_ID)
                        .append("` INTEGER DEFAULT 0;");
                db.execSQL(query.toString());
            case 3:
                db.execSQL("DROP TRIGGER IF EXISTS `delete_cat`;");
                query = new StringBuilder("DROP VIEW IF EXISTS `").append(Tables.Entries.VIEW_NAME)
                        .append("`;");
                db.execSQL(query.toString());
                query = new StringBuilder("CREATE TABLE `").append(Tables.Deleted.TABLE_NAME)
                        .append("` (`").append(Tables.Deleted._ID)
                        .append("` INTEGER PRIMARY KEY, `").append(Tables.Deleted.TYPE)
                        .append("` INTEGER, `").append(Tables.Deleted.CAT)
                        .append("` INTEGER DEFAULT 0, `").append(Tables.Deleted.REMOTE_ID)
                        .append("` INTEGER);");
                db.execSQL(query.toString());
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
     * Insert a preset entry category.
     *
     * @param db        The database
     * @param name      The internal name of the category
     * @param extras    List of internal names of the extra fields
     * @param flavorRes String-array resource ID for the list of flavors
     */
    private void insertPreset(SQLiteDatabase db, String name, String[] extras, int flavorRes) {
        ContentValues values = new ContentValues();

        values.put(Tables.Cats.NAME, name);
        values.put(Tables.Cats.PRESET, 1);
        final long id = db.insert(Tables.Cats.TABLE_NAME, null, values);

        values.clear();
        values.put(Tables.Extras.CAT, id);
        values.put(Tables.Extras.PRESET, 1);
        for(String extra : extras) {
            values.put(Tables.Extras.NAME, extra);
            db.insert(Tables.Extras.TABLE_NAME, null, values);
        }

        values.clear();
        final String[] flavors = mContext.getResources().getStringArray(flavorRes);
        values.put(Tables.Flavors.CAT, id);
        for(String flavor : flavors) {
            values.put(Tables.Flavors.NAME, flavor);
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
