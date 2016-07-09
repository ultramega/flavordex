package com.ultramegasoft.flavordex2.util.csv;

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
    private final PushbackReader mReader;

    /**
     * @param reader The Reader representing the CSV file
     */
    public CSVReader(Reader reader) {
        mReader = new PushbackReader(reader);
    }

    /**
     * Read the next row.
     *
     * @return The row, or null if no data was read
     */
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
