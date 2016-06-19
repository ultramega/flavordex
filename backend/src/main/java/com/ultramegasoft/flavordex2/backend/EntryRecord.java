package com.ultramegasoft.flavordex2.backend;

import java.util.ArrayList;

/**
 * Model for an entry record.
 *
 * @author Steve Guidetti
 */
public class EntryRecord {
    private long id;
    private String uuid;
    private long cat;
    private String catUuid;
    private String title;
    private String maker;
    private String origin;
    private String price;
    private String location;
    private long date;
    private float rating;
    private String notes;
    private long updated;
    private boolean shared;
    private boolean deleted;

    private ArrayList<ExtraRecord> extras;
    private ArrayList<FlavorRecord> flavors;
    private ArrayList<PhotoRecord> photos;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getCat() {
        return cat;
    }

    public void setCat(long cat) {
        this.cat = cat;
    }

    public String getCatUuid() {
        return catUuid;
    }

    public void setCatUuid(String catUuid) {
        this.catUuid = catUuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
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

    public ArrayList<PhotoRecord> getPhotos() {
        return photos;
    }

    public void setPhotos(ArrayList<PhotoRecord> photos) {
        this.photos = photos;
    }
}
