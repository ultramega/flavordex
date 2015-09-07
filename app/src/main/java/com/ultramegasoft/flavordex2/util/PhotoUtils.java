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
public class PhotoUtils {
    /**
     * The tag to use for logging
     */
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
     * @param outputPath Path to save the captured photo
     * @return Image capture Intent
     */
    public static Intent getTakePhotoIntent(Uri outputPath) {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputPath);
        return intent;
    }

    /**
     * Get an Intent to select a photo from the gallery.
     *
     * @return Get content Intent
     */
    public static Intent getSelectPhotoIntent() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
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
     * @param path   The path to the image file
     * @param bitmap The Bitmap to rotate
     * @return The rotated Bitmap
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
     * Load a Bitmap from an image file.
     *
     * @param path      Path to the image file
     * @param reqWidth  The requested width of the decoded Bitmap
     * @param reqHeight The requested height of the decoded Bitmap
     * @return A Bitmap
     */
    public static Bitmap loadBitmap(String path, int reqWidth, int reqHeight) {
        try {
            final Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
            opts.inJustDecodeBounds = false;

            final Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            if("image/jpeg".equals(opts.outMimeType)) {
                return rotatePhoto(path, bitmap);
            }
            return bitmap;
        } catch(OutOfMemoryError e) {
            Log.e(TAG, "Out of memory", e);
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
     * Load a Bitmap as a thumbnail and save it to the persistent cache.
     *
     * @param context The Context
     * @param path    The path to the original image
     * @param id      The ID of the entry the image belongs to
     */
    public static void generateThumb(Context context, String path, long id) {
        final Bitmap inputBitmap = loadBitmap(path, THUMB_SIZE, THUMB_SIZE);

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
     * @param context The Context
     * @param id      The entry ID
     * @return A Bitmap
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
     * @param context The Context
     * @param id      The entry ID
     * @return Whether the file was successfully deleted
     */
    public static boolean deleteThumb(Context context, long id) {
        final File file = getThumbFile(context, id);
        if(file.exists()) {
            sThumbCache.remove(id);
            return file.delete();
        }
        return false;
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
     * Get the Uri for a new captured image file.
     *
     * @return A file Uri
     * @throws IOException
     */
    public static Uri getOutputMediaUri() throws IOException {
        return Uri.fromFile(getOutputMediaFile());
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

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access Framework
     * Documents, as well as the _data field for the MediaStore and other file-based
     * ContentProviders.
     *
     * @param context The Context
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
     * @param context       The Context
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
