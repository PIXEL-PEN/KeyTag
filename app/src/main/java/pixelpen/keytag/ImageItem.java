package pixelpen.keytag;

import android.net.Uri;

public class ImageItem {
    public long id;
    public Uri uri;
    public boolean isSelected = false;
    public long dateTaken = 0;

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