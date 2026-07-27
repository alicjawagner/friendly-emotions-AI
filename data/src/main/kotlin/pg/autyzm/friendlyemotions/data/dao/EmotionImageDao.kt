package pg.autyzm.friendlyemotions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity

@Dao
interface EmotionImageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(image: EmotionImageEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(images: List<EmotionImageEntity>)

    @Update
    suspend fun update(image: EmotionImageEntity)

    @Query("DELETE FROM emotion_images WHERE id = :imageId")
    suspend fun deleteById(imageId: String)

    @Query("SELECT * FROM emotion_images WHERE id = :imageId")
    suspend fun getById(imageId: String): EmotionImageEntity?

    /** Batch lookup backing `LearningStepRepository.getImagesEligibleForStep`, avoiding N+1 queries. */
    @Query("SELECT * FROM emotion_images WHERE id IN (:imageIds)")
    suspend fun getByIds(imageIds: List<String>): List<EmotionImageEntity>

    @Query("SELECT * FROM emotion_images WHERE folderId = :folderId")
    fun observeForFolder(folderId: String): Flow<List<EmotionImageEntity>>

    /** One-shot variant, needed by folder-delete cascade to collect file paths before the DB delete. */
    @Query("SELECT * FROM emotion_images WHERE folderId = :folderId")
    suspend fun getForFolder(folderId: String): List<EmotionImageEntity>

    /** Needed by orphan cleanup to diff every persisted path against `filesDir/images/` contents. */
    @Query("SELECT filePath FROM emotion_images")
    suspend fun getAllFilePaths(): List<String>
}
