package io.github.pxsort.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.github.pxsort.R;

/** A class containing static methods for IO operations.
 *
 * Created by George on 2016-01-16.
 */
public abstract class Media {

    public static final String TAG = Media.class.getSimpleName();
    private static final String FILE_NAME_SUFFIX = ".png";

    /**
     * Saves a Bitmap to external storage
     * @param c
     * @param bitmap
     * @param listener
     */
    public static void saveImage(final Context c, final Bitmap bitmap,
                                 final OnImageSavedListener listener) {

        // Save bitmap using an AsyncTask
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return saveImage(c, bitmap);
            }

            @Override
            protected void onPostExecute(Boolean successful) {
                if (successful) {
                    listener.onImageSaved();
                } else {
                    listener.onError();
                }
            }
        }.execute();
    }

    public interface OnImageSavedListener {

        /**
         * Called on successful comlpetion of saveImage.
         */
        void onImageSaved();

        /**
         * Called if an exception is thrown during saving.
         */
        void onError();
    }


    private static boolean saveImage(Context c, Bitmap bitmap) {
        String appName = c.getString(R.string.app_name);

        // Get the directory for the user's public pictures directory.
        File albumDir = getImageStorageDir(c);
        if (albumDir == null) {
            return false;
        }

        //Compress bitmap with PNG encoding
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

        String imgName = appName.toUpperCase() + "_" + getDateTimeString() + FILE_NAME_SUFFIX;
        File imgFile = new File(albumDir, imgName);

        // write the bitmap to the newly created file
        FileOutputStream fOut;
        try {
            if (! imgFile.createNewFile()) {
                Log.e(TAG, "File " + imgName + "could not be created. Image was not saved.");
                return false;
            }
            fOut = new FileOutputStream(imgFile);
            fOut.write(out.toByteArray());
            fOut.close();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "An error occurred while writing the image to file " +
                    "(" + imgName + "). Image was not saved.", e);
            return false;
        }

        addImageToGallery(c, Uri.fromFile(imgFile));
        return true;
    }


    public static File getImageStorageDir(Context c) {
        String appName = c.getString(R.string.app_name);

        // Get the directory for the user's public pictures directory.
        File albumDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appName);
        if (!albumDir.exists()) {
            boolean success = albumDir.mkdir();
            if (!success) {
                Log.e(TAG, "The directory " + albumDir.getPath() + " failed to be created.");
                return null;
            }
        }

        return albumDir;
    }

    public static File getNewImageFile(Context c) {
        String prefix = "IMG_" + getDateTimeString();
        File imageFile;
        try {
            imageFile = File.createTempFile(prefix, FILE_NAME_SUFFIX, getImageStorageDir(c));
            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, "The image file failed to be created.");
            return null;
        }
    }

    private static String getDateTimeString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hh:mm:ss", Locale.getDefault());
        Date date = Calendar.getInstance().getTime();

        return dateFormat.format(date);
    }

    /**
     * Add the image at the specified Uri to the media gallery.
     *
     * @param c
     * @param imgUri
     */
    public static void addImageToGallery(Context c, Uri imgUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imgUri);
        c.sendBroadcast(mediaScanIntent);
    }


    /**
     * Loads the image located within stream into a Bitmap scaled down as much as possible while
     * still meeting the provided dimensional requirements.
     *
     * @param stream the stream to read the image from
     */
    public static Bitmap loadImage(InputStream stream) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inMutable = true;

        return BitmapFactory.decodeStream(stream, null, opts);
    }


    /**
     * Loads the image located within stream into a Bitmap scaled down as much as possible while
     * still meeting the provided dimensional requirements.
     *
     * @param stream    the stream to read the image from
     * @param reqWidth  the required minimum width for the Bitmap
     * @param reqHeight the required minimum calculateBSTHeight for the Bitmap
     */
    public static Bitmap loadImage(InputStream stream, int reqWidth, int reqHeight) {
        // First calculate the maximum inSampleSize for this bitmap.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);
        int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode the sampled bitmap
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        options.inMutable = true;
        return BitmapFactory.decodeStream(stream, null, options);
    }


    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw calculateBSTHeight and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // calculateBSTHeight and width larger than the requested calculateBSTHeight and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
