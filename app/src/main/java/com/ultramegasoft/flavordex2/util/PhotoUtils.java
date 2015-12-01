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
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.ultramegasoft.flavordex2.provider.Tables;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * @param file File to save the captured photo
     * @return Image capture Intent
     */
    public static Intent getTakePhotoIntent(File file) {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        return intent;
    }

    /**
     * Get an Intent to select a photo from the gallery.
     *
     * @return Get content Intent
     */
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
    private static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
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
    private static Bitmap rotatePhoto(Context context, Uri uri, Bitmap bitmap) {
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
                    if(cursor.moveToFirst()) {
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
    private static Uri getImageUri(Context context, Uri uri) {
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
    public static Bitmap loadBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {
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
    public static void generateThumb(Context context, long id) {
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
    private static void generateThumb(Context context, Uri uri, long id) {
        try {
            if(uri == null) {
                //noinspection ResultOfMethodCallIgnored
                getThumbFile(context, id).createNewFile();
                return;
            }

            final Bitmap inputBitmap = loadBitmap(context, uri, THUMB_SIZE, THUMB_SIZE);

            if(inputBitmap != null) {
                final FileOutputStream os = new FileOutputStream(getThumbFile(context, id));
                inputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                os.close();

                sThumbCache.remove(id);
                inputBitmap.recycle();
            }
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
    public static Bitmap getThumb(Context context, long id) {
        final File file = getThumbFile(context, id);

        if(!file.exists()) {
            generateThumb(context, id);
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
    public static void deleteThumb(Context context, long id) {
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
    private static File getThumbFile(Context context, long id) {
        final String fileName = THUMB_FILE_PREFIX + id + JPEG_FILE_SUFFIX;
        return new File(context.getCacheDir(), fileName);
    }

    /**
     * Get the output file for a new captured image.
     *
     * @return A File object pointing to the file
     * @throws IOException
     */
    public static File getOutputMediaFile() throws IOException {
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
     * @throws IOException
     */
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

    public static String getName(ContentResolver cr, Uri uri) {
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
    public static String getMD5Hash(ContentResolver cr, Uri uri) {
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
    public static Uri parsePath(String path) {
        if(path.charAt(0) == '/') {
            return Uri.fromFile(new File(path));
        }
        return Uri.parse(path);
    }
}
