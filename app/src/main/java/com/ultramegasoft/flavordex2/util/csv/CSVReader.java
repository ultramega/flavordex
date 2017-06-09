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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple reader for CSV files.
 *
 * @author Steve Guidetti
 */
public class CSVReader implements Closeable {
    /**
     * The Reader representing the CSV file
     */
    @NonNull
    private final PushbackReader mReader;

    /**
     * @param reader The Reader representing the CSV file
     */
    public CSVReader(@NonNull Reader reader) {
        mReader = new PushbackReader(reader);
    }

    /**
     * Read the next row.
     *
     * @return The row, or null if no data was read
     */
    @Nullable
    public String[] readNext() {
        final List<String> fields = new ArrayList<>();

        String field = "";
        boolean useQuotes = false;
        boolean inValue = false;
        char character;
        try {
            while(true) {
                character = (char)mReader.read();
                if(character == '\uffff') {
                    break;
                }

                if(!inValue) {
                    if(character == '\r' || character == '\n') {
                        do {
                            character = (char)mReader.read();
                        } while(character == '\r' || character == '\n');
                        mReader.unread(character);
                        break;
                    }

                    inValue = true;

                    if(character == '"') {
                        useQuotes = true;
                        continue;
                    } else {
                        useQuotes = false;
                    }
                }

                if(character == '"') {
                    character = (char)mReader.read();
                    if(character == '"') {
                        field += character;
                        continue;
                    } else {
                        mReader.unread(character);
                        character = '"';
                    }
                }

                if((!useQuotes && (character == ',' || character == '\r' || character == '\n'))
                        || (useQuotes && character == '"')) {
                    inValue = false;
                    fields.add(field);
                    field = "";
                    if(useQuotes) {
                        character = (char)mReader.read();
                        if(character != ',') {
                            mReader.unread(character);
                        }
                    }
                    continue;
                }

                field += character;
            }
        } catch(IOException e) {
            return null;
        }

        return fields.isEmpty() ? null : fields.toArray(new String[fields.size()]);
    }

    @Override
    public void close() throws IOException {
        mReader.close();
    }
}
