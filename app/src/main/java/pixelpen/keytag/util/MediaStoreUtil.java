package pixelpen.keytag.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class MediaStoreUtil {

    public static long getMediaStoreId(Context context, Uri uri) {

        try {

            if (uri == null) return -1;

            String[] projection = { MediaStore.Images.Media._ID };

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int column = cursor.getColumnIndexOrThrow(
                                MediaStore.Images.Media._ID
                        );
                        return cursor.getLong(column);
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