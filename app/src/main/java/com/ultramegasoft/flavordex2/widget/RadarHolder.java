package com.ultramegasoft.flavordex2.widget;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds the data associated with a data point on a RadarView.
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
     * The label for this item
     */
    public final String name;

    /**
     * The value of this item
     */
    public int value;

    /**
     * @param name  The name of this item to use as the label
     * @param value The value of this data point
     */
    public RadarHolder(String name, int value) {
        this.name = name;
        this.value = value;
    }

    private RadarHolder(Parcel in) {
        this(in.readString(), in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(value);
    }

}
