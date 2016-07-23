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
