package com.ultramegasoft.flavordex2.util;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

/**
 * Compatibility layer for parsing HTML.
 *
 * @author Steve Guidetti
 */
public class HtmlCompat {
    /**
     * Parse an HTML string into formatted text.
     *
     * @param source The HTML string
     * @return The formatted text
     */
    public static Spanned fromHtml(String source) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, 0);
        }
        //noinspection deprecation
        return Html.fromHtml(source);
    }
}
