package com.ultramegasoft.flavordex2.util.csv;

import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple writer for CSV files.
 *
 * @author Steve Guidetti
 */
public class CSVWriter implements Closeable {
    private static final String TAG = "CSVWriter";

    /**
     * The Writer representing the CSV file
     */
    private final Writer mWriter;

    /**
     * @param writer The Writer representing the CSV file
     */
    public CSVWriter(Writer writer) {
        mWriter = writer;
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
        try {
            mWriter.write(TextUtils.join(",", fields) + "\r\n");
        } catch(IOException e) {
            Log.e(TAG, "Error writing to CSV file.", e);
        }
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
