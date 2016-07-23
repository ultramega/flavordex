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
    public String hash;

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
        this(in.readLong(), in.readString(),
                (Uri)in.readParcelable(PhotoHolder.class.getClassLoader()), in.readInt());
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
