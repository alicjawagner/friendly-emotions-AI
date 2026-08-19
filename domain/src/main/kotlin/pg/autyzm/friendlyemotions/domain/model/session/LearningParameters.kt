package pg.autyzm.friendlyemotions.domain.model.session

/** Controls all aspects of learning-mode trial generation and presentation (target-domain.md §4.9). */
data class LearningParameters(
    val displayedImageCount: Int = DEFAULT_DISPLAYED_IMAGE_COUNT,
    val repetitionsPerEmotion: Int = DEFAULT_REPETITIONS_PER_EMOTION,
    val promptTemplate: PromptTemplate = PromptTemplate.EMOTION_ONLY,
    val ttsEnabled: Boolean = true,
    val captionsEnabled: Boolean = true,
    val hintDelaySeconds: Int = DEFAULT_HINT_DELAY_SECONDS,
    val activeHintTypes: Set<HintType> = setOf(HintType.DIM_INCORRECT),
    val mixedGenderInAnswers: Boolean = true,
) {
    init {
        require(displayedImageCount in DISPLAYED_IMAGE_COUNT_RANGE) {
            "displayedImageCount must be in $DISPLAYED_IMAGE_COUNT_RANGE, was $displayedImageCount"
        }
        require(repetitionsPerEmotion in REPETITIONS_PER_EMOTION_RANGE) {
            "repetitionsPerEmotion must be in $REPETITIONS_PER_EMOTION_RANGE, was $repetitionsPerEmotion"
        }
        require(hintDelaySeconds in HINT_DELAY_SECONDS_RANGE) {
            "hintDelaySeconds must be in $HINT_DELAY_SECONDS_RANGE, was $hintDelaySeconds"
        }
        require(activeHintTypes.isNotEmpty()) { "activeHintTypes must contain at least one element" }
    }

    companion object {
        const val DEFAULT_DISPLAYED_IMAGE_COUNT = 3
        const val DEFAULT_REPETITIONS_PER_EMOTION = 2
        const val DEFAULT_HINT_DELAY_SECONDS = 5
        val DISPLAYED_IMAGE_COUNT_RANGE = 1..6
        val REPETITIONS_PER_EMOTION_RANGE = 1..10
        val HINT_DELAY_SECONDS_RANGE = 3..10
    }
}
