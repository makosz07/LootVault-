package com.macoshapps.lootvault;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Decodes and encodes photos with sampling, so full-resolution bitmaps never sit in memory. */
public final class Images {
    private Images() {}

    /** Reads an image from a content URI, scales it to maxSide and returns it as Base64 JPEG. */
    public static String encodeScaled(ContentResolver resolver, Uri uri, int maxSide, int quality) throws IOException {
        Bitmap bitmap = decodeSampled(resolver, uri, maxSide);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    /** Decodes a stored Base64 image for display. Returns null if the data is unreadable. */
    public static Bitmap decode(String base64, int maxSide) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleFor(Math.max(bounds.outWidth, bounds.outHeight), maxSide);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Bitmap decodeSampled(ContentResolver resolver, Uri uri, int maxSide) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(resolver, uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Unsupported image");
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleFor(Math.max(bounds.outWidth, bounds.outHeight), maxSide);
        Bitmap decoded;
        try (InputStream in = open(resolver, uri)) {
            decoded = BitmapFactory.decodeStream(in, null, opts);
        }
        if (decoded == null) throw new IOException("Decode failed");
        return fit(decoded, maxSide);
    }

    private static InputStream open(ContentResolver resolver, Uri uri) throws IOException {
        InputStream in = resolver.openInputStream(uri);
        if (in == null) throw new IOException("Cannot open image");
        return in;
    }

    private static int sampleFor(int largestSide, int maxSide) {
        int sample = 1;
        while (largestSide / (sample * 2) >= maxSide) sample *= 2;
        return sample;
    }

    private static Bitmap fit(Bitmap bitmap, int maxSide) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int largest = Math.max(w, h);
        if (largest <= maxSide) return bitmap;
        float scale = maxSide / (float) largest;
        return Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(w * scale)),
                Math.max(1, Math.round(h * scale)), true);
    }
}
