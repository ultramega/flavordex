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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    @NonNull
    public final String name;

    /**
     * Whether this is a preset extra
     */
    public final boolean preset;

    /**
     * The value of the field
     */
    @Nullable
    public String value;

    /**
     * @param id     The database ID of the extra
     * @param name   The name of the field
     * @param preset Whether this is a preset extra
     */
    public ExtraFieldHolder(long id, @NonNull String name, boolean preset) {
        this(id, name, preset, null);
    }

    /**
     * @param id     The database ID of the extra
     * @param name   The name of the field
     * @param preset Whether this is a preset extra
     * @param value  The initial value of the field
     */
    public ExtraFieldHolder(long id, @NonNull String name, boolean preset, @Nullable String value) {
        this.id = id;
        this.name = name;
        this.preset = preset;
        this.value = value;
    }

    private ExtraFieldHolder(Parcel in) {
        id = in.readLong();
        name = in.readString();
        preset = in.readInt() == 1;
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
        dest.writeInt(preset ? 1 : 0);
        dest.writeString(value);
    }
}
