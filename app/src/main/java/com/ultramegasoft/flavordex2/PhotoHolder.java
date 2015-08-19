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
    public final long id;

    /**
     * The path to the photo file
     */
    public final String path;

    /**
     * Whether the photo was added from the gallery
     */
    public final boolean fromGallery;

    /**
     * @param id          The database id for this photo
     * @param path        The path to the photo file
     * @param fromGallery Whether the photo was added from the gallery
     */
    public PhotoHolder(long id, String path, boolean fromGallery) {
        this.id = id;
        this.path = path;
        this.fromGallery = fromGallery;
    }

    /**
     * @param path        The path to the photo file
     * @param fromGallery Whether the photo was added from the gallery
     */
    public PhotoHolder(String path, boolean fromGallery) {
        this(0, path, fromGallery);
    }

    protected PhotoHolder(Parcel in) {
        this(in.readLong(), in.readString(), in.readInt() == 1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(path);
        dest.writeInt(fromGallery ? 1 : 0);
    }
}
