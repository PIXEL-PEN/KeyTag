/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 TST (PIXEL-PEN)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * KeyTag — Batch keyword tagging for Android
 * Copyright (C) 2026 PIXEL-PEN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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