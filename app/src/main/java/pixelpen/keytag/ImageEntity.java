package pixelpen.keytag.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "images",
        indices = {@Index(value = {"uri"}, unique = true)}
)
public class ImageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String uri;

    public long dateTagged;

    public int qualityLevel;   // 0–3 rating (default 0)

    public Long mediaStoreId;

    public ImageEntity(String uri, long dateTagged) {
        this.uri = uri;
        this.dateTagged = dateTagged;
        this.qualityLevel = 0;
        this.mediaStoreId = null;
    }
}