package pixelpen.keytag.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "keywords",
        indices = {@Index(value = {"name"}, unique = true)}
)
public class KeywordEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    public int usageCount;

    public KeywordEntity(String name, int usageCount) {
        this.name = name;
        this.usageCount = usageCount;
    }
}