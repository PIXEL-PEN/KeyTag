package pixelpen.keytag.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class MediaStoreUtil {

    public static long getMediaStoreId(Context context, Uri uri) {

        try {
            if (uri == null) return -1;

            // If it's already a MediaStore URI, extract ID directly from path
            if ("media".equals(uri.getAuthority())) {
                String lastSegment = uri.getLastPathSegment();
                if (lastSegment != null) {
                    try {
                        return Long.parseLong(lastSegment);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // For exotic URIs extract filename and look up by DISPLAY_NAME
            String lastSegment = uri.getLastPathSegment();
            if (lastSegment == null) return -1;

            String displayName = lastSegment.contains("/")
                    ? lastSegment.substring(lastSegment.lastIndexOf("/") + 1)
                    : lastSegment;

            // Try images first
            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID},
                    MediaStore.Images.Media.DISPLAY_NAME + "=?",
                    new String[]{displayName},
                    MediaStore.Images.Media.DATE_TAKEN + " DESC"
            );

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0);
                    }
                } finally {
                    cursor.close();
                }
            }

            // Try videos as fallback
            Cursor videoCursor = context.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.Media._ID},
                    MediaStore.Video.Media.DISPLAY_NAME + "=?",
                    new String[]{displayName},
                    MediaStore.Video.Media.DATE_TAKEN + " DESC"
            );

            if (videoCursor != null) {
                try {
                    if (videoCursor.moveToFirst()) {
                        return videoCursor.getLong(0);
                    }
                } finally {
                    videoCursor.close();
                }
            }

        } catch (Exception ignored) {
        }

        return -1;
    }

}