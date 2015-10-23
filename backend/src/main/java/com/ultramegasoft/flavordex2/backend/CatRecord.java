package com.ultramegasoft.flavordex2.backend;

import java.util.ArrayList;

/**
 * Model for a category record.
 *
 * @author Steve Guidetti
 */
public class CatRecord {
    private long id;
    private String name;
    private long updated;
    private boolean deleted;

    private ArrayList<ExtraRecord> extras;
    private ArrayList<FlavorRecord> flavors;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public ArrayList<ExtraRecord> getExtras() {
        return extras;
    }

    public void setExtras(ArrayList<ExtraRecord> extras) {
        this.extras = extras;
    }

    public ArrayList<FlavorRecord> getFlavors() {
        return flavors;
    }

    public void setFlavors(ArrayList<FlavorRecord> flavors) {
        this.flavors = flavors;
    }
}
