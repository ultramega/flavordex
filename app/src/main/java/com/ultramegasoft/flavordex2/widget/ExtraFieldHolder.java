package com.ultramegasoft.flavordex2.widget;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holder for data associated with an extra field.
 *
 * @author Steve Guidetti
 */
public class ExtraFieldHolder implements Parcelable {
    public static final Creator<ExtraFieldHolder> CREATOR = new Creator<ExtraFieldHolder>() {
        @Override
        public ExtraFieldHolder createFromParcel(Parcel in) {
            return new ExtraFieldHolder(in);
        }

        @Override
        public ExtraFieldHolder[] newArray(int size) {
            return new ExtraFieldHolder[size];
        }
    };

    /**
     * The database ID of the extra
     */
    public final long id;

    /**
     * The name of the field
     */
    public final String name;

    /**
     * Whether this is a preset extra
     */
    public final boolean preset;

    /**
     * The value of the field
     */
    public String value;

    /**
     * @param id     The database ID of the extra
     * @param name   The name of the field
     * @param preset Whether this is a preset extra
     */
    public ExtraFieldHolder(long id, String name, boolean preset) {
        this(id, name, preset, null);
    }

    /**
     * @param id     The database ID of the extra
     * @param name   The name of the field
     * @param preset Whether this is a preset extra
     * @param value  The initial value of the field
     */
    public ExtraFieldHolder(long id, String name, boolean preset, String value) {
        this.id = id;
        this.name = name;
        this.preset = preset;
        this.value = value;
    }

    protected ExtraFieldHolder(Parcel in) {
        id = in.readLong();
        name = in.readString();
        final boolean[] booleans = new boolean[1];
        in.readBooleanArray(booleans);
        preset = booleans[0];
        value = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeBooleanArray(new boolean[] {preset});
        dest.writeString(value);
    }
}
