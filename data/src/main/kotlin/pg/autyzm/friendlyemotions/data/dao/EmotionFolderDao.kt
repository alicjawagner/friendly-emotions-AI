package pg.autyzm.friendlyemotions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity

@Dao
interface EmotionFolderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(folder: EmotionFolderEntity)

    @Update
    suspend fun update(folder: EmotionFolderEntity)

    @Query("DELETE FROM emotion_folders WHERE id = :folderId")
    suspend fun deleteById(folderId: String)

    @Query("SELECT * FROM emotion_folders WHERE id = :folderId")
    suspend fun getById(folderId: String): EmotionFolderEntity?

    @Query("SELECT * FROM emotion_folders WHERE emotionId = :emotionId")
    fun observeForEmotion(emotionId: String): Flow<List<EmotionFolderEntity>>

    /** Batch lookup backing `EmotionFolderRepository.getFoldersByIds`, avoiding N+1 queries. */
    @Query("SELECT * FROM emotion_folders WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<EmotionFolderEntity>
}
