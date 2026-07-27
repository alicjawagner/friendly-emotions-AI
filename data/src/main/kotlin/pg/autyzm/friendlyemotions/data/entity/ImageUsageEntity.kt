package pg.autyzm.friendlyemotions.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Row for [pg.autyzm.friendlyemotions.domain.model.session.ImageUsage], the join between a
 * [LearningStepEntity] and an [EmotionImageEntity]. Both foreign keys cascade on delete, so removing
 * a step or an image automatically removes its usage rows (ADR-009).
 */
@Entity(
    tableName = "image_usages",
    primaryKeys = ["stepId", "imageId"],
    foreignKeys = [
        ForeignKey(
            entity = LearningStepEntity::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EmotionImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["imageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("imageId")],
)
data class ImageUsageEntity(
    val stepId: String,
    val imageId: String,
    val inLearning: Boolean,
    val inTest: Boolean,
)
