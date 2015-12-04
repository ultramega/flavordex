package com.ultramegasoft.flavordex2.widget;

import android.net.Uri;
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
     * The MD5 hash of the photo file
     */
    public final String hash;

    /**
     * The Uri to the photo file
     */
    public Uri uri;

    /**
     * The sort position of the photo
     */
    public int pos;

    /**
     * @param id   The database ID for this photo
     * @param hash The MD5 hash of the photo
     * @param uri  The Uri to the photo file
     * @param pos  The sort position of the photo
     */
    public PhotoHolder(long id, String hash, Uri uri, int pos) {
        this.id = id;
        this.hash = hash;
        this.uri = uri;
        this.pos = pos;
    }

    private PhotoHolder(Parcel in) {
        this(in.readLong(), in.readString(), (Uri)in.readParcelable(null), in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(hash);
        dest.writeParcelable(uri, 0);
        dest.writeInt(pos);
    }
}
