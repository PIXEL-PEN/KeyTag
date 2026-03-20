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
                    } catch (NumberFormatException ignored) {}
                }
            }

            // For exotic URIs (e.g. com.miui.gallery.open),
            // extract filename and look up in MediaStore by DISPLAY_NAME
            String lastSegment = uri.getLastPathSegment();
            if (lastSegment == null) return -1;

            // lastSegment may be a full path — extract just the filename
            String displayName = lastSegment.contains("/")
                    ? lastSegment.substring(lastSegment.lastIndexOf("/") + 1)
                    : lastSegment;

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{ MediaStore.Images.Media._ID },
                    MediaStore.Images.Media.DISPLAY_NAME + "=?",
                    new String[]{ displayName },
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

        } catch (Exception ignored) {
        }

        return -1;
    }
}