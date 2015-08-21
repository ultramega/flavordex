package com.ultramegasoft.flavordex2;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holder to contain information about a photo.
 *
 * @author Steve Guidetti
 */
public class PhotoHolder implements Parcelable {
    public static final Creator<PhotoHolder> CREATOR = new Creator<PhotoHolder>() {
        @Override
        public PhotoHolder createFromParcel(Parcel in) {
            return new PhotoHolder(in);
        }

        @Override
        public PhotoHolder[] newArray(int size) {
            return new PhotoHolder[size];
        }
    };

    /**
     * The database id for this photo
     */
    public long id;

    /**
     * The path to the photo file
     */
    public String path;

    /**
     * @param id          The database id for this photo
     * @param path        The path to the photo file
     */
    public PhotoHolder(long id, String path) {
        this.id = id;
        this.path = path;
    }

    /**
     * @param path        The path to the photo file
     */
    public PhotoHolder(String path) {
        this(0, path);
    }

    protected PhotoHolder(Parcel in) {
        this(in.readLong(), in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(path);
    }
}
