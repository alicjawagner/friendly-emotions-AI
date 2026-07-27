package pg.autyzm.friendlyemotions.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for the aggregate root [pg.autyzm.friendlyemotions.domain.model.session.LearningStep].
 * `MaterialSelection` is intentionally not embedded here — it lives in the separate [ImageUsageEntity]
 * table, since it is a one-to-many relationship. `activeMode` is nullable and must be null iff
 * `isActive` is false, mirroring the domain model's `init` invariant (enforced by the repository, not
 * by this entity).
 */
@Entity(tableName = "learning_steps")
data class LearningStepEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isActive: Boolean,
    val activeMode: String?,
    val isExample: Boolean,
    @Embedded(prefix = "lp_") val learningParameters: LearningParametersEmbedded,
    @Embedded(prefix = "tp_") val testParameters: TestParametersEmbedded,
    @Embedded(prefix = "rs_") val reinforcementSettings: ReinforcementSettingsEmbedded,
)
