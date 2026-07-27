package pg.autyzm.friendlyemotions.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for [pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage]. `gender` is stored
 * as the enum `.name` and is never null here — per `NewEmotionImage`'s KDoc, gender resolution
 * happens before persistence, so this table only ever holds resolved images.
 */
@Entity(
    tableName = "emotion_images",
    foreignKeys = [
        ForeignKey(
            entity = EmotionFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderId")],
)
data class EmotionImageEntity(
    @PrimaryKey val id: String,
    val folderId: String,
    val filePath: String,
    val gender: String,
    val isExample: Boolean,
)
