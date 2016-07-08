package com.ultramegasoft.flavordex2.backend.model;

import java.util.HashMap;

/**
 * Model containing lists of deleted and updated categories and entries.
 *
 * @author Steve Guidetti
 */
public class SyncRecord extends Model {
    public HashMap<String, Long> deletedCats;
    public HashMap<String, Long> updatedCats;
    public HashMap<String, Long> deletedEntries;
    public HashMap<String, Long> updatedEntries;
}
