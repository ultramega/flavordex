package com.ultramegasoft.flavordex2.backend;

import java.util.ArrayList;

/**
 * Model for a data update containing records that have changed.
 *
 * @author Steve Guidetti
 */
public class UpdateRecord {
    private long timestamp;
    private ArrayList<EntryRecord> entries;
    private ArrayList<CatRecord> cats;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<EntryRecord> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<EntryRecord> entries) {
        this.entries = entries;
    }

    public ArrayList<CatRecord> getCats() {
        return cats;
    }

    public void setCats(ArrayList<CatRecord> cats) {
        this.cats = cats;
    }
}
