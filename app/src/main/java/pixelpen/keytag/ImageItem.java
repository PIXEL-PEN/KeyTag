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

import android.net.Uri;

public class ImageItem {
    public long id;
    public Uri uri;
    public boolean isSelected = false;
    public long dateTaken = 0;
    public boolean isVideo = false;
    public long duration = 0;
    public String bucketName = null;

    // Header support
    public boolean isHeader = false;
    public String headerLabel = null;

    // Regular image constructor
    public ImageItem(long id, Uri uri) {
        this.id = id;
        this.uri = uri;
    }

    // Header constructor
    public static ImageItem asHeader(String label) {
        ImageItem item = new ImageItem(0, null);
        item.isHeader = true;
        item.headerLabel = label;
        return item;
    }
}