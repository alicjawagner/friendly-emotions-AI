package pg.autyzm.friendlyemotions.data.entity

import androidx.room.ColumnInfo

/**
 * `@Embedded` row fragment for [pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings],
 * used inside [LearningStepEntity] with the `rs_` column prefix. `enabledPraiseWords` and
 * `enabledAnimationThemes` are comma-joined; this is safe since theme keys and praise words never
 * contain commas.
 */
data class ReinforcementSettingsEmbedded(
    val enabledPraiseWords: String,
    @ColumnInfo(defaultValue = "'flowers,butterflies,balloons,cars,balls'")
    val enabledAnimationThemes: String,
    val animationsEnabled: Boolean,
    val endSessionAnimationEnabled: Boolean,
    val endSessionFanfareEnabled: Boolean,
)
