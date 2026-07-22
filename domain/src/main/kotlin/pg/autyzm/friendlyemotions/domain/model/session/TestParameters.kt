package pg.autyzm.friendlyemotions.domain.model.session

/**
 * Controls test-mode session behavior (target-domain.md §4.10). When [overridesLearning] is `false`,
 * all other fields are kept mirroring the active `LearningParameters` — that mirroring is performed by
 * `DeriveTestParametersUseCase` (session 2.7), not by this class.
 *
 * There is no separate answer time limit field: the test-mode timer uses `LearningParameters.hintDelaySeconds`.
 */
data class TestParameters(
    val overridesLearning: Boolean = false,
    val displayedImageCount: Int = LearningParameters.DEFAULT_DISPLAYED_IMAGE_COUNT,
    val repetitionsPerEmotion: Int = LearningParameters.DEFAULT_REPETITIONS_PER_EMOTION,
    val promptTemplate: PromptTemplate = PromptTemplate.EMOTION_ONLY,
    val ttsEnabled: Boolean = false,
    val captionsEnabled: Boolean = false,
    val mixedGenderInAnswers: Boolean = true,
) {
    init {
        require(displayedImageCount in LearningParameters.DISPLAYED_IMAGE_COUNT_RANGE) {
            "displayedImageCount must be in ${LearningParameters.DISPLAYED_IMAGE_COUNT_RANGE}, was $displayedImageCount"
        }
        require(repetitionsPerEmotion in LearningParameters.REPETITIONS_PER_EMOTION_RANGE) {
            "repetitionsPerEmotion must be in " +
                "${LearningParameters.REPETITIONS_PER_EMOTION_RANGE}, was $repetitionsPerEmotion"
        }
    }
}
