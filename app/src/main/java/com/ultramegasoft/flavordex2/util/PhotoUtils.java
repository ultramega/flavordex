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
package com.ultramegasoft.flavordex2.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.ultramegasoft.flavordex2.BuildConfig;
import com.ultramegasoft.flavordex2.provider.Tables;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilities for capturing and manipulating images.
 *
 * @author Steve Guidetti
 */
public class PhotoUtils {
    private static final String TAG = "PhotoUtils";

    /**
     * The name of the album to store photos taken with the camera
     */
    private static final String ALBUM_DIR = "Flavordex";

    /**
     * The prefix for photo file names
     */
    private static final String JPEG_FILE_PREFIX = "IMG_";

    /**
     * The extension to use for photo file names
     */
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    /**
     * The prefix for cached thumbnails
     */
    private static final String THUMB_FILE_PREFIX = "thumb_";

    /**
     * The width and height of thumbnail Bitmaps
     */
    private static final int THUMB_SIZE = 40;

    /**
     * The shared memory cache for thumbnails
     */
    private static final BitmapCache sThumbCache = new BitmapCache();

    /**
     * Get an Intent to capture a photo.
     *
     * @return Image capture Intent
     */
    @Nullable
    public static Intent getTakePhotoIntent(@NonNull Context context) {
        try {
            final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            final Uri uri = FileProvider.getUriForFile(context,
                    BuildConfig.APPLICATION_ID + ".fileprovider", getOutputMediaFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return intent;
        } catch(IOException e) {
            Log.e(TAG, "Failed to create new file", e);
        }
        return null;
    }

    /**
     * Get an Intent to select a photo from the gallery.
     *
     * @return Get content Intent
     */
    @NonNull
    public static Intent getSelectPhotoIntent() {
        final Intent intent;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("image/*");
        return intent;
    }

    /**
     * Get the memory cache for storing thumbnails.
     *
     * @return The thumbnail cache
     */
    @NonNull
    public static BitmapCache getThumbCache() {
        return sThumbCache;
    }

    /**
     * Calculate the sample size for an image being loaded.
     *
     * @param options   Options object containing the original dimensions
     * @param reqWidth  The requested width of the decoded Bitmap
     * @param reqHeight The requested height of the decoded Bitmap
     * @return The sample size
     */
    private static int calculateInSampleSize(@NonNull Options options, int reqWidth,
                                             int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while((halfHeight / inSampleSize) > reqHeight
                    || (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Rotate an image according to its EXIF data.
     *
     * @param context The Context
     * @param uri     The Uri to the image file
     * @param bitmap  The Bitmap to rotate
     * @return The rotated Bitmap
     */
    @NonNull
    private static Bitmap rotatePhoto(@NonNull Context context, @NonNull Uri uri,
                                      @NonNull Bitmap bitmap) {
        uri = getImageUri(context, uri);
        int rotation = 0;

        if("file".equals(uri.getScheme())) {
            try {
                final ExifInterface exif = new ExifInterface(uri.getPath());
                switch(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotation = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotation = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotation = 270;
                }
            } catch(IOException e) {
                Log.e(TAG, "Failed to read EXIF data for " + uri.toString(), e);
            }
        } else {
            final ContentResolver cr = context.getContentResolver();
            final String[] projection = new String[] {MediaStore.Images.ImageColumns.ORIENTATION};
            final Cursor cursor = cr.query(uri, projection, null, null, null);
            if(cursor != null) {
                try {
                    if(cursor.moveToFirst() && cursor.getColumnCount() > 0) {
                        rotation = cursor.getInt(0);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        if(rotation != 0) {
            final Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                    true);
        }

        return bitmap;
    }

    /**
     * Get an image media or file Uri based on its document Uri.
     *
     * @param context The Context
     * @param uri     The Uri to convert
     * @return The image or file Uri or the original Uri if it could not be converted
     */
    @NonNull
    private static Uri getImageUri(@NonNull Context context, @NonNull Uri uri) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && DocumentsContract.isDocumentUri(context, uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] parts = docId.split(":");
            if("image".equals(parts[0])) {
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, parts[1]);
            } else if("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                return Uri.fromFile(new File(Environment.getExternalStorageDirectory(), parts[1]));
            }
        }
        return uri;
    }

    /**
     * Load a Bitmap from an image file.
     *
     * @param context   The Context
     * @param uri       The Uri to the image file
     * @param reqWidth  The requested width of the decoded Bitmap
     * @param reqHeight The requested height of the decoded Bitmap
     * @return A Bitmap
     */
    @Nullable
    public static Bitmap loadBitmap(@NonNull Context context, @NonNull Uri uri, int reqWidth,
                                    int reqHeight) {
        final ContentResolver cr = context.getContentResolver();
        try {
            final ParcelFileDescriptor parcelFileDescriptor = cr.openFileDescriptor(uri, "r");
            if(parcelFileDescriptor == null) {
                Log.w(TAG, "Unable to open " + uri.toString());
                return null;
            }
            try {
                final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                final Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, opts);

                opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
                opts.inJustDecodeBounds = false;

                final Bitmap bitmap =
                        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, opts);
                if("image/jpeg".equals(opts.outMimeType)) {
                    return rotatePhoto(context, uri, bitmap);
                }
                return bitmap;
            } catch(OutOfMemoryError e) {
                Log.e(TAG, "Out of memory", e);
            } finally {
                parcelFileDescriptor.close();
            }
        } catch(FileNotFoundException e) {
            Log.w(TAG, "File not found: " + uri.toString());
        } catch(SecurityException e) {
            Log.w(TAG, "Permission denied for Uri: " + uri.toString());
        } catch(IOException e) {
            Log.e(TAG, "Failed to load bitmap", e);
        }
        return null;
    }

    /**
     * Generate a thumbnail image file and save it to the persistent cache. If no photos exist for
     * the entry, te current file is deleted.
     *
     * @param context The Context
     * @param id      Te ID for the entry
     */
    private static void generateThumb(@NonNull Context context, long id) {
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return;
        }

        final ContentResolver cr = context.getContentResolver();
        final Uri uri = Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, id + "/photos");
        final String where = Tables.Photos.PATH + " NOT NULL";
        final Cursor cursor = cr.query(uri, new String[] {Tables.Photos.PATH}, where, null,
                Tables.Photos.POS + " ASC");
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    generateThumb(context, parsePath(cursor.getString(0)), id);
                } else {
                    generateThumb(context, null, id);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Load a Bitmap as a thumbnail and save it to the persistent cache.
     *
     * @param context The Context
     * @param uri     The Uri to the original image
     * @param id      The ID of the entry the image belongs to
     */
    private static void generateThumb(@NonNull Context context, @Nullable Uri uri, long id) {
        try {
            if(uri != null) {
                final Bitmap inputBitmap = loadBitmap(context, uri, THUMB_SIZE, THUMB_SIZE);

                if(inputBitmap != null) {
                    final FileOutputStream os = new FileOutputStream(getThumbFile(context, id));
                    inputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                    os.close();

                    sThumbCache.remove(id);
                    inputBitmap.recycle();
                }
            }

            //noinspection ResultOfMethodCallIgnored
            getThumbFile(context, id).createNewFile();
        } catch(IOException e) {
            Log.e(TAG, "Error writing thumbnail bitmap", e);
        }
    }

    /**
     * Get the thumbnail for an entry, generating one as needed.
     *
     * @param context The Context
     * @param id      The entry ID
     * @return A Bitmap
     */
    @Nullable
    public static Bitmap getThumb(@NonNull Context context, long id) {
        final File file = getThumbFile(context, id);

        if(!file.exists()) {
            if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) &&
                    PermissionUtils.hasExternalStoragePerm(context)) {
                generateThumb(context, id);
            } else {
                return null;
            }
        }

        if(file.length() == 0) {
            return null;
        }

        try {
            return BitmapFactory.decodeFile(file.getPath(), null);
        } catch(OutOfMemoryError e) {
            Log.e(TAG, "Out of memory", e);
        }
        return null;
    }

    /**
     * Delete a thumbnail.
     *
     * @param context The Context
     * @param id      The entry ID
     */
    public static void deleteThumb(@NonNull Context context, long id) {
        final File file = getThumbFile(context, id);
        if(file.exists()) {
            sThumbCache.remove(id);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            final ContentResolver cr = context.getContentResolver();
            final Uri uri = ContentUris.withAppendedId(Tables.Entries.CONTENT_ID_URI_BASE, id);
            cr.notifyChange(uri, null);
        }
    }

    /**
     * Get a Bitmap file for an entry.
     *
     * @param context The Context
     * @param id      The entry ID
     * @return A reference to the image file
     */
    @NonNull
    private static File getThumbFile(@NonNull Context context, long id) {
        final String fileName = THUMB_FILE_PREFIX + id + JPEG_FILE_SUFFIX;
        return new File(context.getCacheDir(), fileName);
    }

    /**
     * Get the output file for a new captured image.
     *
     * @return A File object pointing to the file
     */
    @NonNull
    private static File getOutputMediaFile() throws IOException {
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            throw new IOException("Media storage not mounted");
        }

        final String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(getMediaStorageDir(), JPEG_FILE_PREFIX + timeStamp + JPEG_FILE_SUFFIX);
    }

    /**
     * Get the directory where captured images are stored.
     *
     * @return The media storage directory
     */
    @NonNull
    public static File getMediaStorageDir() throws IOException {
        final File mediaStorageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        final File albumDir = new File(mediaStorageDir, ALBUM_DIR);

        if(!albumDir.exists()) {
            if(!albumDir.mkdirs()) {
                throw new IOException("Failure creating directories");
            }
        }

        return albumDir;
    }

    /**
     * Get the file Uri for the given Uri.
     *
     * @param cr  The ContentResolver
     * @param uri The original Uri
     * @return The file Uri
     */
    @Nullable
    public static Uri getFileUri(@NonNull ContentResolver cr, @NonNull Uri uri) {
        final String name = getName(cr, uri);
        if(name != null) {
            final File file;
            try {
                file = new File(getMediaStorageDir(), name);
                if(file.exists()) {
                    return Uri.fromFile(file);
                }
                return savePhotoFromUri(cr, uri, file);
            } catch(IOException e) {
                Log.w(TAG, "Unable to check for existing file", e);
            }
        }
        return savePhotoFromUri(cr, uri, null);
    }

    /**
     * Save a photo from a content Uri to the external storage.
     *
     * @param cr   The ContentResolver
     * @param uri  The content Uri
     * @param file The File to write to or null to create one
     * @return The file Uri for the new file
     */
    @Nullable
    private static Uri savePhotoFromUri(@NonNull ContentResolver cr, @NonNull Uri uri,
                                        @Nullable File file) {
        try {
            if(file == null) {
                file = getOutputMediaFile();
            }
            final InputStream inputStream = cr.openInputStream(uri);
            if(inputStream != null) {
                final OutputStream outputStream = new FileOutputStream(file);
                try {
                    final byte[] buffer = new byte[8192];
                    int read;
                    while((read = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, read);
                    }
                    return Uri.fromFile(file);
                } finally {
                    inputStream.close();
                    outputStream.close();
                }
            }
        } catch(FileNotFoundException e) {
            Log.w(TAG, "File not found for Uri: " + uri.getPath());
        } catch(IOException e) {
            Log.e(TAG, "Failed to save the photo", e);
        }
        return null;
    }

    /**
     * Get the file name from a content Uri.
     *
     * @param cr  The ContentResolver
     * @param uri The Uri
     * @return The file name
     */
    @Nullable
    public static String getName(@NonNull ContentResolver cr, @NonNull Uri uri) {
        if("file".equals(uri.getScheme())) {
            return uri.getLastPathSegment();
        } else {
            final String[] projection = new String[] {MediaStore.MediaColumns.DISPLAY_NAME};
            final Cursor cursor = cr.query(uri, projection, null, null, null);
            if(cursor != null) {
                try {
                    if(cursor.moveToFirst()) {
                        return cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return null;
    }

    /**
     * Get the MD5 hash of a file as a 32 character hex string.
     *
     * @param cr  The ContentResolver
     * @param uri The Uri representing the file
     * @return The MD5 hash of the file or null on failure
     */
    @Nullable
    public static String getMD5Hash(@NonNull ContentResolver cr, @NonNull Uri uri) {
        try {
            final InputStream inputStream = cr.openInputStream(uri);
            if(inputStream == null) {
                Log.w(TAG, "Unable to open stream from " + uri.toString());
                return null;
            }
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            try {
                final byte[] buffer = new byte[8192];
                int read;
                while((read = inputStream.read(buffer)) > 0) {
                    messageDigest.update(buffer, 0, read);
                }
                final BigInteger digest = new BigInteger(1, messageDigest.digest());
                return String.format("%32s", digest.toString(16)).replace(" ", "0");
            } finally {
                inputStream.close();
            }
        } catch(FileNotFoundException e) {
            Log.i(TAG, e.getMessage());
        } catch(NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Failed to generate MD5 hash", e);
        }

        return null;
    }

    /**
     * Parse a string into a Uri.
     *
     * @param path The string to parse
     * @return The Uri
     */
    @Nullable
    public static Uri parsePath(@NonNull String path) {
        if(path.charAt(0) == '/') {
            return Uri.fromFile(new File(path));
        } else if(path.startsWith("file://") || path.startsWith("content://")) {
            return Uri.parse(path);
        }
        try {
            return Uri.fromFile(new File(getMediaStorageDir(), path));
        } catch(IOException e) {
            Log.e(TAG, "Failed to parse path: " + path, e);
        }
        return null;
    }

    /**
     * Get the simplest representation of a photo path from a Uri.
     *
     * @param uri The photo Uri
     * @return The file name or full Uri string
     */
    @NonNull
    static String getPathString(@NonNull Uri uri) {
        if("file".equals(uri.getScheme())) {
            final File file = new File(uri.getPath());
            try {
                if(file.getParentFile().equals(getMediaStorageDir())) {
                    return file.getName();
                }
            } catch(IOException e) {
                Log.w(TAG, "Unable to check file location", e);
            }
        }
        return uri.toString();
    }
}
