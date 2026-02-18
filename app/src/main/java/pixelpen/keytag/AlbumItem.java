package pixelpen.keytag;

import android.net.Uri;

public class AlbumItem {

    public long bucketId;
    public String bucketName;
    public Uri coverUri;
    public int itemCount;

    public AlbumItem(long bucketId, String bucketName, Uri coverUri, int itemCount) {
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.coverUri = coverUri;
        this.itemCount = itemCount;
    }
}
