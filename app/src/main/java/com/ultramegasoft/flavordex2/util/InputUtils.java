package com.ultramegasoft.flavordex2.util;

import android.text.InputFilter;
import android.text.LoginFilter;
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
    public static final InputFilter NAME_FILTER = new LoginFilter.UsernameFilterGeneric() {
        private static final String sReservedCharacters = "_|:";

        @Override
        public boolean isAllowed(char c) {
            return sReservedCharacters.indexOf(c) == -1;
        }
    };

    /**
     * Filter for field values
     */
    public static final InputFilter VALUE_FILTER = new LoginFilter.UsernameFilterGeneric() {
        private static final String sReservedCharacters = "|:";

        @Override
        public boolean isAllowed(char c) {
            return sReservedCharacters.indexOf(c) == -1;
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
