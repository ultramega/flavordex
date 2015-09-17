package com.ultramegasoft.flavordex2.widget;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * @author Steve Guidetti
 */
public class EntryHolder implements Parcelable {
    public static final Creator<EntryHolder> CREATOR = new Creator<EntryHolder>() {
        @Override
        public EntryHolder createFromParcel(Parcel in) {
            return new EntryHolder(in);
        }

        @Override
        public EntryHolder[] newArray(int size) {
            return new EntryHolder[size];
        }
    };

    /**
     * Fields from the entries table
     */
    public long id;
    public String title;
    public long catId;
    public String catName;
    public long makerId;
    public String maker;
    public String origin;
    public String price;
    public String location;
    public long date;
    public float rating;
    public String notes;

    /**
     * List of extra fields for this entry
     */
    private final ArrayList<ExtraFieldHolder> mExtras;

    /**
     * List of flavors for this entry
     */
    private final ArrayList<RadarHolder> mFlavors;

    /**
     * List of photos for this entry
     */
    private final ArrayList<PhotoHolder> mPhotos;

    public EntryHolder() {
        mExtras = new ArrayList<>();
        mFlavors = new ArrayList<>();
        mPhotos = new ArrayList<>();
    }

    private EntryHolder(Parcel in) {
        id = in.readLong();
        title = in.readString();
        catId = in.readLong();
        catName = in.readString();
        makerId = in.readLong();
        maker = in.readString();
        origin = in.readString();
        price = in.readString();
        location = in.readString();
        date = in.readLong();
        rating = in.readFloat();
        notes = in.readString();

        mExtras = in.createTypedArrayList(ExtraFieldHolder.CREATOR);
        mFlavors = in.createTypedArrayList(RadarHolder.CREATOR);
        mPhotos = in.createTypedArrayList(PhotoHolder.CREATOR);
    }

    /**
     * Add an extra field to this entry.
     *
     * @param id     The database ID of the extra
     * @param name   The name of the field
     * @param preset Whether this is a preset extra
     * @param value  The value of the field
     */
    public void addExtra(long id, String name, boolean preset, String value) {
        mExtras.add(new ExtraFieldHolder(id, name, preset, value));
    }

    /**
     * Get the list of extra fields for this entry.
     *
     * @return The list of extra fields
     */
    public ArrayList<ExtraFieldHolder> getExtras() {
        return mExtras;
    }

    /**
     * Add a flavor to this entry.
     *
     * @param name  The name of this flavor
     * @param value The value of this flavor
     */
    public void addFlavor(String name, int value) {
        mFlavors.add(new RadarHolder(name, value));
    }

    /**
     * Get the list of flavors for this entry.
     *
     * @return The list of flavors
     */
    public ArrayList<RadarHolder> getFlavors() {
        return mFlavors;
    }

    /**
     * Add a photo to this entry.
     *
     * @param id   The database ID for this photo
     * @param path The path to the photo file
     */
    public void addPhoto(long id, String path) {
        mPhotos.add(new PhotoHolder(id, path));
    }

    /**
     * Get the list of photos for this entry.
     *
     * @return The list of photos
     */
    public ArrayList<PhotoHolder> getPhotos() {
        return mPhotos;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeLong(catId);
        dest.writeString(catName);
        dest.writeLong(makerId);
        dest.writeString(maker);
        dest.writeString(origin);
        dest.writeString(price);
        dest.writeString(location);
        dest.writeLong(date);
        dest.writeFloat(rating);
        dest.writeString(notes);

        dest.writeTypedList(mExtras);
        dest.writeTypedList(mFlavors);
        dest.writeTypedList(mPhotos);
    }
}
