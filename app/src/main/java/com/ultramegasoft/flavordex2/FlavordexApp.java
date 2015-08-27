package com.ultramegasoft.flavordex2;

import android.app.Application;
import android.content.Context;

import java.util.HashMap;

/**
 * Stores global runtime information needed by the application.
 *
 * @author Steve Guidetti
 */
public class FlavordexApp extends Application {
    /**
     * Preference names
     */
    public static final String PREF_DETECT_LOCATION = "pref_detect_location";
    public static final String PREF_LIST_SORT_FIELD = "pref_list_sort_field";
    public static final String PREF_LIST_SORT_REVERSED = "pref_list_sort_reversed";

    /**
     * Entry type preset names
     */
    public static final String TYPE_BEER = "_beer";
    public static final String TYPE_WINE = "_wine";
    public static final String TYPE_WHISKEY = "_whiskey";
    public static final String TYPE_COFFEE = "_coffee";

    /**
     * Map of preset type names to string resource ids
     */
    private static final HashMap<String, Integer> sTypeNameMap = new HashMap<>();

    static {
        sTypeNameMap.put(TYPE_BEER, R.string.type_beer);
        sTypeNameMap.put(TYPE_WINE, R.string.type_wine);
        sTypeNameMap.put(TYPE_WHISKEY, R.string.type_whiskey);
        sTypeNameMap.put(TYPE_COFFEE, R.string.type_coffee);
    }

    /**
     * Get the real display name of an entry type from a raw database name value, translating
     * internal names as needed.
     *
     * @param context The context
     * @param name    The name from the database
     * @return The real display name
     */
    public static String getRealTypeName(Context context, String name) {
        if(sTypeNameMap.containsKey(name)) {
            return context.getString(sTypeNameMap.get(name));
        }
        return name;
    }
}
