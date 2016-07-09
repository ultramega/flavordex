package com.ultramegasoft.flavordex2.util.csv;

import android.text.TextUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple writer for CSV files.
 *
 * @author Steve Guidetti
 */
public class CSVWriter implements Closeable {
    /**
     * The Writer representing the CSV file
     */
    private final PrintWriter mWriter;

    /**
     * @param writer The Writer representing the CSV file
     */
    public CSVWriter(Writer writer) {
        mWriter = new PrintWriter(writer);
    }

    /**
     * Write a row to the CSV file.
     *
     * @param values The data to write
     */
    public void writeNext(String[] values) {
        final List<String> fields = new ArrayList<>();
        for(Object field : values) {
            fields.add(prepareValue(field));
        }
        mWriter.println(TextUtils.join(",", fields));
    }

    /**
     * Quote and escape a value to be placed in a field.
     *
     * @param value The value to quote and escape
     * @return The value as a quoted and escaped string
     */
    private static String prepareValue(Object value) {
        return '"' + value.toString().replace("\"", "\"\"") + '"';
    }

    @Override
    public void close() throws IOException {
        mWriter.close();
    }
}
