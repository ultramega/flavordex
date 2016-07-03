package com.ultramegasoft.flavordex2.backend.model;

import java.util.ArrayList;

/**
 * Model for an entry record.
 *
 * @author Steve Guidetti
 */
public class EntryRecord extends Model {
    public long id;
    public String uuid;
    public long cat;
    public String catUuid;
    public String title;
    public String maker;
    public String origin;
    public String price;
    public String location;
    public long date;
    public float rating;
    public String notes;
    public long age;
    public boolean shared;
    public boolean deleted;

    public ArrayList<ExtraRecord> extras;
    public ArrayList<FlavorRecord> flavors;
    public ArrayList<PhotoRecord> photos;
}
