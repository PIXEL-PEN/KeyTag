package pixelpen.keytag.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "image_keywords",
        primaryKeys = {"imageId", "keywordId"},
        foreignKeys = {
                @ForeignKey(
                        entity = ImageEntity.class,
                        parentColumns = "id",
                        childColumns = "imageId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = KeywordEntity.class,
                        parentColumns = "id",
                        childColumns = "keywordId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("imageId"),
                @Index("keywordId")
        }
)
public class ImageKeywordCrossRef {

    public long imageId;
    public long keywordId;

    public ImageKeywordCrossRef(long imageId, long keywordId) {
        this.imageId = imageId;
        this.keywordId = keywordId;
    }
}