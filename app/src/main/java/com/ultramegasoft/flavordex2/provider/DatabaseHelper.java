package com.ultramegasoft.flavordex2.provider;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ultramegasoft.flavordex2.R;

import java.io.InputStream;
import java.util.Locale;
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
    public static final int DATABASE_VERSION = 1;

    private final Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final InputStream inputStream = mContext.getResources().openRawResource(R.raw.schema);
        final Scanner scanner = new Scanner(inputStream).useDelimiter("\\n--");
        while(scanner.hasNext()) {
            db.execSQL(scanner.next());
        }

        final int[] types = {
                R.array.beer_flavor_names,
                R.array.wine_flavor_names,
                R.array.whiskey_flavor_names,
                R.array.coffee_flavor_names
        };
        for(int i = 0; i < types.length; i++) {
            final String[] flavors = mContext.getResources().getStringArray(types[i]);
            for(String flavor : flavors) {
                final String query = String.format(
                        Locale.US,
                        "INSERT INTO flavors (`type`, `name`) VALUES (%d, %s);",
                        i + 1, DatabaseUtils.sqlEscapeString(flavor)
                );
                db.execSQL(query);
            }
        }

        addDummyData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Add some sample data
     *
     * @param db The database
     */
    private void addDummyData(SQLiteDatabase db) {
        final InputStream inputStream = mContext.getResources().openRawResource(R.raw.testdata);
        final Scanner scanner = new Scanner(inputStream).useDelimiter("\\n--");
        while(scanner.hasNext()) {
            db.execSQL(scanner.next());
        }
    }
}
