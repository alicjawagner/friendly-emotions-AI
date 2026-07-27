package pg.autyzm.friendlyemotions.data.entity

/**
 * `@Embedded` row fragment for [pg.autyzm.friendlyemotions.domain.model.session.LearningParameters],
 * used inside [LearningStepEntity] with the `lp_` column prefix. `activeHintTypes` is a comma-joined
 * list of `HintType` enum names.
 */
data class LearningParametersEmbedded(
    val displayedImageCount: Int,
    val repetitionsPerEmotion: Int,
    val promptTemplate: String,
    val ttsEnabled: Boolean,
    val captionsEnabled: Boolean,
    val hintDelaySeconds: Int,
    val activeHintTypes: String,
    val mixedGenderInAnswers: Boolean,
)
