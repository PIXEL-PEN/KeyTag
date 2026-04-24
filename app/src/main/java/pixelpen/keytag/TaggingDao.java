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

package pixelpen.keytag.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaggingDao {

    // Insert image
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertImage(ImageEntity image);

    // Insert keyword
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
            "INNER JOIN image_keywords ik ON k.id = ik.keywordId " +
            "WHERE ik.imageId = :imageId")
    List<KeywordEntity> getKeywordsForImage(long imageId);

    // Get all keyword names
    @Query("SELECT name FROM keywords ORDER BY usageCount DESC")
    List<String> getAllKeywordNames();

    // Increment usage count
    @Query("UPDATE keywords SET usageCount = usageCount + 1 WHERE id = :keywordId")
    void incrementUsage(long keywordId);

    // Decrement usage count
    @Query("UPDATE keywords SET usageCount = usageCount - 1 WHERE id = :keywordId")
    void decrementUsage(long keywordId);

    // Get usage count
    @Query("SELECT usageCount FROM keywords WHERE id = :keywordId")
    int getUsageCount(long keywordId);

    // Remove keyword from image
    @Query("DELETE FROM image_keywords WHERE imageId = :imageId AND keywordId = :keywordId")
    void removeCrossRef(long imageId, long keywordId);

    // Delete keyword
    @Query("DELETE FROM keywords WHERE id = :keywordId")
    void deleteKeywordById(long keywordId);

    // Search images by keyword
    @Query("SELECT i.uri FROM images i " +
            "INNER JOIN image_keywords ik ON i.id = ik.imageId " +
            "INNER JOIN keywords k ON k.id = ik.keywordId " +
            "WHERE k.name = :keyword")
    List<String> getImageUrisForKeyword(String keyword);

    @Query("SELECT COUNT(*) FROM image_keywords WHERE keywordId = :keywordId")
    int getKeywordUsageFromCrossRef(long keywordId);

    // Update star rating
    @Query("UPDATE images SET qualityLevel = :level WHERE uri = :uri")
    void updateQuality(String uri, int level);

    // Get star rating
    @Query("SELECT qualityLevel FROM images WHERE uri = :uri LIMIT 1")
    Integer getQuality(String uri);

    // Star search
    @Query("SELECT uri FROM images WHERE qualityLevel = :level")
    List<String> getUrisByStarLevel(int level);

    @Query("SELECT uri FROM images WHERE qualityLevel >= :level")
    List<String> getUrisByMinimumStarLevel(int level);

    // Update by ID (safer for toggleFavorite)
    @Query("UPDATE images SET qualityLevel = :level WHERE id = :id")
    void updateQualityById(long id, int level);

    @Query("UPDATE images SET mediaStoreId = :mediaStoreId WHERE uri = :uri")
    void updateMediaStoreId(String uri, long mediaStoreId);

    @Query("SELECT qualityLevel FROM images WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    Integer getQualityByMediaStoreId(long mediaStoreId);

    @Query("SELECT * FROM images WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    ImageEntity getImageByMediaStoreId(long mediaStoreId);

    @Query("UPDATE images SET qualityLevel = :level WHERE mediaStoreId = :mediaStoreId")
    void updateQualityByMediaStoreId(long mediaStoreId, int level);

}