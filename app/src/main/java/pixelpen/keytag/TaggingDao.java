package pixelpen.keytag.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaggingDao {

    // Insert image (ignore if already exists)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertImage(ImageEntity image);

    // Insert keyword (ignore if already exists)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertKeyword(KeywordEntity keyword);

    // Insert cross reference
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCrossRef(ImageKeywordCrossRef crossRef);

    // Get image by URI
    @Query("SELECT * FROM images WHERE uri = :uri LIMIT 1")
    ImageEntity getImageByUri(String uri);

    // Get keyword by name
    @Query("SELECT * FROM keywords WHERE name = :name LIMIT 1")
    KeywordEntity getKeywordByName(String name);

    // Get keywords for an image
    @Query("SELECT k.* FROM keywords k " +
            "INNER JOIN image_keywords ik " +
            "ON k.id = ik.keywordId " +
            "WHERE ik.imageId = :imageId")
    List<KeywordEntity> getKeywordsForImage(long imageId);

    @Query("SELECT name FROM keywords ORDER BY usageCount DESC")
    List<String> getAllKeywordNames();

    @Query("UPDATE keywords SET usageCount = usageCount + 1 WHERE id = :keywordId")
    void incrementUsage(long keywordId);
    @Query("SELECT i.uri FROM images i " +
            "INNER JOIN image_keywords ik ON i.id = ik.imageId " +
            "INNER JOIN keywords k ON k.id = ik.keywordId " +
            "WHERE k.name = :keyword")
    List<String> getImageUrisForKeyword(String keyword);

}