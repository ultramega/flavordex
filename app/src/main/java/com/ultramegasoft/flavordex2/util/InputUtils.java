package com.ultramegasoft.flavordex2.util;

import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;

/**
 * Helpers for managing text inputs.
 *
 * @author Steve Guidetti
 */
public class InputUtils {
    /**
     * Filter for field names
     */
    public static final InputFilter NAME_FILTER = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            if(dstart == 0 && end > 0) {
                for(int i = start; i < end; i++) {
                    if(source.charAt(i) != '_') {
                        return source.subSequence(i, end);
                    } else if(end == 1) {
                        return "";
                    }
                }
            }
            return source;
        }
    };

    /**
     * Add an InputFilter to an EditText.
     *
     * @param editText The EditText
     * @param filter   The InputFilter
     */
    public static void addFilter(EditText editText, InputFilter filter) {
        if(editText == null || filter == null) {
            return;
        }
        final InputFilter[] currentFilters = editText.getFilters();
        final InputFilter[] newFilters = new InputFilter[currentFilters.length + 1];
        newFilters[0] = filter;
        System.arraycopy(currentFilters, 0, newFilters, 1, currentFilters.length);
        editText.setFilters(newFilters);
    }
}
