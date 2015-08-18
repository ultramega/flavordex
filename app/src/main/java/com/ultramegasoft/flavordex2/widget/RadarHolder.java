package com.ultramegasoft.flavordex2.widget;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds the data associated with a point on a radar chart.
 *
 * @author Steve Guidetti
 */
public class RadarHolder implements Parcelable {
    public static final Creator<RadarHolder> CREATOR = new Creator<RadarHolder>() {
        @Override
        public RadarHolder createFromParcel(Parcel in) {
            return new RadarHolder(in);
        }

        @Override
        public RadarHolder[] newArray(int size) {
            return new RadarHolder[size];
        }
    };

    /**
     * The id associated with this item, suh as a database row id
     */
    public long id;

    /**
     * The label for this item
     */
    public String name;

    /**
     * The value of this item
     */
    public int value;

    /**
     * @param id    The id associated with this item
     * @param name  The name of this item to use as the label
     * @param value The value of this data point
     */
    public RadarHolder(long id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    protected RadarHolder(Parcel in) {
        this(in.readLong(), in.readString(), in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeInt(value);
    }

}
