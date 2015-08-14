package com.ultramegasoft.flavordex2.util;

import android.annotation.TargetApi;
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
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.ultramegasoft.flavordex2.provider.Tables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilities for capturing and manipulating images.
 *
 * @author Steve Guidetti
 */
public class PhotoManager {
    private static final String TAG = "PhotoManager";

    private static final String CAMERA_DIR = "/DCIM/";
    private static final String ALBUM_DIR = "Flavordex";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String THUMB_FILE_PREFIX = "thumb_";

    private static final int THUMB_SIZE = 40;

    private static final BitmapCache sThumbCache = new BitmapCache("thumbs");

    private Uri mCurrentPhotoPath;

    /**
     * Get an intent to capture a photo.
     *
     * @return Image capture intent
     * @throws IOException
     */
    public Intent getTakePhotoIntent() throws IOException {
        mCurrentPhotoPath = getOutputMediaFileUri();

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoPath);
        return intent;
    }

    /**
     * Get the path to the current photo being captured.
     *
     * @return Path as a string
     */
    public String getCurrentPhotoPath() {
        return mCurrentPhotoPath.getPath();
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
     * Calculate the final sample size for an image being loaded.
     *
     * @param options   Options object containing the original dimensions
     * @param minLength The minimum width or height
     * @param maxSize   The maximum number of pixels allowed
     * @return The final sample size
     */
    private static int computeSampleSize(Options options, int minLength, int maxSize) {
        final int initialSize = computeInitialSampleSize(options, minLength, maxSize);

        int roundedSize;
        if(initialSize <= 8) {
            roundedSize = 1;
            while(roundedSize <= initialSize) {
                roundedSize <<= 1;
            }
            roundedSize >>= 1;
        } else {
            roundedSize = (initialSize - 7) / 8 * 8;
        }

        return roundedSize;
    }

    /**
     * Calculate the rough sample size for an image being loaded.
     *
     * @param options   Options object containing the original dimensions
     * @param minLength The minimum width or height
     * @param maxSize   The maximum number of pixels allowed
     * @return The rough sample size
     */
    private static int computeInitialSampleSize(Options options, int minLength, int maxSize) {
        final double w = options.outWidth;
        final double h = options.outHeight;

        int lowerBound = (int)Math.ceil(Math.sqrt(w * h / maxSize));
        int upperBound = (int)Math.min(Math.floor(w / minLength), Math.floor(h / minLength));

        if(upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }
        return upperBound;
    }

    /**
     * Rotate an image according to its EXIF data.
     *
     * @param path   The path to the image file
     * @param bitmap The bitmap to rotate
     * @return The rotated bitmap
     */
    private static Bitmap rotatePhoto(String path, Bitmap bitmap) {
        try {
            final Matrix matrix = new Matrix();

            final ExifInterface exif = new ExifInterface(path);
            final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);
        } catch(IOException e) {
            Log.w(TAG, "Failed to read EXIF data");
        }

        return bitmap;
    }

    /**
     * Load a bitmap from an image file.
     *
     * @param path  Path to the image file
     * @param minWH The minimum width or height in pixels
     * @return A bitmap
     */
    public static Bitmap loadBitmap(String path, int minWH) {
        try {
            final Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            opts.inSampleSize = computeSampleSize(opts, minWH, 3 * 1024 * 1024);
            opts.inJustDecodeBounds = false;
            opts.inDither = false;

            final Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            return rotatePhoto(path, bitmap);
        } catch(OutOfMemoryError e) {
            Log.e(TAG, "Out of memory", e);
        }
        return null;
    }

    /**
     * Generate a thumbnail image file and save it to the persistent cache. If no photos exist for
     * the entry, te current file is deleted.
     *
     * @param context The context
     * @param id      Te id for the entry
     */
    public static void generateThumb(Context context, long id) {
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return;
        }

        final ContentResolver cr = context.getContentResolver();
        final Uri uri = Uri.withAppendedPath(Tables.Entries.CONTENT_ID_URI_BASE, id + "/photos");
        final Cursor cursor = cr.query(uri, new String[] {Tables.Photos.PATH}, null, null,
                Tables.Photos._ID + " ASC");
        try {
            if(cursor.moveToFirst()) {
                generateThumb(context, cursor.getString(0), id);
            } else {
                deleteThumb(context, id);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Load a bitmap as a thumbnail and save it to the persistent cache.
     *
     * @param context The context
     * @param path    The path to the original image
     * @param id      The id of the entry the image belongs to
     */
    public static void generateThumb(Context context, String path, long id) {
        final Bitmap inputBitmap = loadBitmap(path, THUMB_SIZE);

        if(inputBitmap != null) {
            try {
                final FileOutputStream os = new FileOutputStream(getThumbFile(context, id));
                inputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                os.close();

                sThumbCache.remove(id);
            } catch(IOException e) {
                Log.w(TAG, "Error writing thumbnail bitmap", e);
            }

            inputBitmap.recycle();
        }
    }

    /**
     * Get the thumbnail for an entry, generating one as needed.
     *
     * @param context The context
     * @param id      The entry id
     * @return A bitmap
     */
    public static Bitmap getThumb(Context context, long id) {
        final File file = getThumbFile(context, id);

        if(!file.exists()) {
            generateThumb(context, id);
            if(!file.exists()) {
                return null;
            }
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
     * @param context The context
     * @param id      The entry id
     */
    public static void deleteThumb(Context context, long id) {
        final File file = getThumbFile(context, id);
        if(file.exists()) {
            file.delete();
            sThumbCache.remove(id);
        }
    }

    /**
     * Get a bitmap file for an entry.
     *
     * @param context The context
     * @param id      The entry id
     * @return A reference to the image file
     */
    private static File getThumbFile(Context context, long id) {
        final String fileName = THUMB_FILE_PREFIX + id + JPEG_FILE_SUFFIX;
        return new File(context.getCacheDir(), fileName);
    }

    /**
     * Get the output file as a Uri for a new captured image.
     *
     * @return A Uri pointing to the file
     * @throws IOException
     */
    private static Uri getOutputMediaFileUri() throws IOException {
        return Uri.fromFile(getOutputMediaFile());
    }


    /**
     * Get the output file for a new captured image.
     *
     * @return A File pointing to the file
     * @throws IOException
     */
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
     * @throws IOException
     */
    public static File getMediaStorageDir() throws IOException {
        final File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + CAMERA_DIR + ALBUM_DIR);

        if(!mediaStorageDir.exists()) {
            if(!mediaStorageDir.mkdirs()) {
                throw new IOException("Failure creating directories");
            }
        }

        return mediaStorageDir;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access Framework
     * Documents, as well as the _data field for the MediaStore and other file-based
     * ContentProviders.
     *
     * @param context The context
     * @param uri     The Uri to query
     */
    @TargetApi(19)
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if(isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if(isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if(isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if(isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other
     * file-based ContentProviders.
     *
     * @param context       The context
     * @param uri           The Uri to query
     * @param selection     (Optional) Filter used in the query
     * @param selectionArgs (Optional) Selection arguments used in the query
     * @return The value of the _data column, which is typically a file path
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if(cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
