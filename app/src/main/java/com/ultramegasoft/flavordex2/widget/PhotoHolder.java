package com.ultramegasoft.flavordex2.widget;

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
     * The database ID for this photo
     */
    public long id;

    /**
     * The path to the photo file
     */
    public final String path;

    /**
     * The sort position of the photo
     */
    public int pos;

    /**
     * @param id   The database ID for this photo
     * @param path The path to the photo file
     * @param pos  The sort position of the photo
     */
    public PhotoHolder(long id, String path, int pos) {
        this.id = id;
        this.path = path;
        this.pos = pos;
    }

    /**
     * @param path The path to the photo file
     * @param pos  The sort position of the photo
     */
    public PhotoHolder(String path, int pos) {
        this(0, path, pos);
    }

    private PhotoHolder(Parcel in) {
        this(in.readLong(), in.readString(), in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(path);
        dest.writeInt(pos);
    }
}
