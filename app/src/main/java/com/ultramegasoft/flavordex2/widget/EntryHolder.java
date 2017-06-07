/*
 * The MIT License (MIT)
 * Copyright © 2016 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.widget;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.ultramegasoft.radarchart.RadarHolder;

import java.util.ArrayList;

/**
 * Holds the data for a journal entry.
 *
 * @author Steve Guidetti
 */
@SuppressWarnings("WeakerAccess")
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
    public String uuid;
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
        uuid = in.readString();
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
    @SuppressWarnings("SameParameterValue")
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
     * @param id  The database ID for this photo
     * @param uri The Uri to the photo file
     */
    public void addPhoto(long id, String hash, Uri uri) {
        mPhotos.add(new PhotoHolder(id, hash, uri, 0));
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
        dest.writeString(uuid);
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
