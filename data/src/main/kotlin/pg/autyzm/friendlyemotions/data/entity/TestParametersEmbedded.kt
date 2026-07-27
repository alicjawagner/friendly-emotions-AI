package pg.autyzm.friendlyemotions.data.entity

/**
 * `@Embedded` row fragment for [pg.autyzm.friendlyemotions.domain.model.session.TestParameters],
 * used inside [LearningStepEntity] with the `tp_` column prefix.
 */
data class TestParametersEmbedded(
    val overridesLearning: Boolean,
    val displayedImageCount: Int,
    val repetitionsPerEmotion: Int,
    val promptTemplate: String,
    val ttsEnabled: Boolean,
    val captionsEnabled: Boolean,
    val mixedGenderInAnswers: Boolean,
)
