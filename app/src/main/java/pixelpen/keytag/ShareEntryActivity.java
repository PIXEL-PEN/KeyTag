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

package pixelpen.keytag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;  // ← add this
import android.provider.MediaStore;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ShareEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> uriStrings = new ArrayList<>();

        Intent intent = getIntent();
        if (intent != null) {

            String action = intent.getAction();
            String type = intent.getType();

            if (type != null && type.startsWith("image/")) {

                if (Intent.ACTION_SEND.equals(action)) {

                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (uri != null) {
                        uriStrings.add(resolveToMediaStoreUri(uri));
                    }

                } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {

                    ArrayList<Uri> uris =
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                    if (uris != null) {
                        for (Uri uri : uris) {
                            uriStrings.add(resolveToMediaStoreUri(uri));
                        }
                    }
                }
            }
        }

        if (!uriStrings.isEmpty()) {

            Intent launch = new Intent(this, AlbumContentsActivity.class);
            launch.putStringArrayListExtra("search_results", uriStrings);
            launch.putExtra("share_mode", true);
            startActivity(launch);
        }

        finish();
    }

    private String resolveToMediaStoreUri(Uri uri) {

        try {
            if ("media".equals(uri.getAuthority())) {
                return uri.toString();
            }

            String name = uri.getLastPathSegment();

            if (name != null) {
                Cursor cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media._ID},
                        MediaStore.Images.Media.DISPLAY_NAME + "=?",
                        new String[]{name},
                        null
                );

                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            Uri mediaUri = Uri.withAppendedPath(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    String.valueOf(id)
                            );
                            return mediaUri.toString();
                        } else {
                        }
                    } finally {
                        cursor.close();
                    }
                } else {
                }
            }

        } catch (Exception e) {
        }

        return uri.toString();
    }
}