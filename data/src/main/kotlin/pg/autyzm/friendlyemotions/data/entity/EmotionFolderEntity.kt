package pg.autyzm.friendlyemotions.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for [pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder]. `emotionId` and
 * `genderPolicy` are stored as their enum `.name` via the DAO/mapper boundary rather than a
 * `@TypeConverter`, since both are plain, non-nullable strings here.
 */
@Entity(tableName = "emotion_folders")
data class EmotionFolderEntity(
    @PrimaryKey val id: String,
    val emotionId: String,
    val name: String,
    val genderPolicy: String,
    val isExample: Boolean,
)
