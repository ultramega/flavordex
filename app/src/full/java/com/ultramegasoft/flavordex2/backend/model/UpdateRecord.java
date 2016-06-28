package com.ultramegasoft.flavordex2.backend.model;

import java.util.ArrayList;

/**
 * Model for a data update containing records that have changed.
 *
 * @author Steve Guidetti
 */
public class UpdateRecord extends Model {
    public long timestamp;
    public ArrayList<EntryRecord> entries;
    public ArrayList<CatRecord> cats;
}
