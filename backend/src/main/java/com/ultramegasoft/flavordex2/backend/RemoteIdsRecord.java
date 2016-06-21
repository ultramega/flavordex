package com.ultramegasoft.flavordex2.backend;

import java.util.HashMap;

/**
 * Model containing a map of entry UUIs to remote IDs.
 *
 * @author Steve Guidetti
 */
public class RemoteIdsRecord {
    private HashMap<String, Long> entryIds;

    public HashMap<String, Long> getEntryIds() {
        return entryIds;
    }

    public void setEntryIds(HashMap<String, Long> entryIds) {
        this.entryIds = entryIds;
    }
}
