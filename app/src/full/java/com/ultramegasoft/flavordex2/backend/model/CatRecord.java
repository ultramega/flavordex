package com.ultramegasoft.flavordex2.backend.model;

import java.util.ArrayList;

/**
 * Model for a category record.
 *
 * @author Steve Guidetti
 */
public class CatRecord {
    public long id;
    public String uuid;
    public String name;
    public long updated;
    public boolean deleted;

    public ArrayList<ExtraRecord> extras;
    public ArrayList<FlavorRecord> flavors;
}
