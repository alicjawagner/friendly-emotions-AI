package pg.autyzm.friendlyemotions.data.entity

/**
 * `@Embedded` row fragment for [pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings],
 * used inside [LearningStepEntity] with the `rs_` column prefix. `enabledPraiseWords` is comma-joined;
 * this is safe since praise words are single Polish words that never contain commas.
 */
data class ReinforcementSettingsEmbedded(
    val enabledPraiseWords: String,
    val animationsEnabled: Boolean,
    val endSessionAnimationEnabled: Boolean,
    val endSessionFanfareEnabled: Boolean,
)
