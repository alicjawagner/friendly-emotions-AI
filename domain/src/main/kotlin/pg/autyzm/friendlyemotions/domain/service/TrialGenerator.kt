package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import kotlin.random.Random

/**
 * Generates the ordered list of trials for one session (target-domain.md §7.1). A fresh instance
 * (with its own [ImageExhaustionTracker]) should be created per session, since the tracker's
 * per-emotion cycling state must not leak across sessions.
 */
class TrialGenerator(
    private val imageExhaustionTracker: ImageExhaustionTracker = ImageExhaustionTracker(),
    private val random: Random = Random,
) {
    /**
     * @param imagesByEmotion eligible images (already filtered by mode) grouped by the emotion they
     *   depict. Groups with no images are discarded (§7.1 step 1); the working emotion pool is the
     *   remaining non-empty groups — §7.1 step 2's capping formula reduces to "all available emotion
     *   groups", so no further reduction is applied.
     * @param displayedImageCount number of on-screen options per trial, including the correct one.
     * @param repetitionsPerEmotion number of trials generated for each emotion in the working pool.
     * @param mixedGenderInAnswers when `false`, distractors are constrained to the correct image's
     *   grammatical gender, degrading to fewer distractors if not enough such images exist (§8.8).
     */
    fun generate(
        imagesByEmotion: Map<EmotionId, List<EmotionImage>>,
        displayedImageCount: Int,
        repetitionsPerEmotion: Int,
        mixedGenderInAnswers: Boolean,
    ): List<Trial> {
        val nonEmptyGroups = imagesByEmotion.filterValues { it.isNotEmpty() }
        val workingEmotions = nonEmptyGroups.keys.shuffled(random)

        val trials = mutableListOf<Trial>()
        for (emotionId in workingEmotions) {
            val pool = nonEmptyGroups.getValue(emotionId)
            repeat(repetitionsPerEmotion) {
                trials += generateTrial(emotionId, pool, nonEmptyGroups, displayedImageCount, mixedGenderInAnswers)
            }
        }

        return trials.shuffled(random)
    }

    private fun generateTrial(
        emotionId: EmotionId,
        pool: List<EmotionImage>,
        nonEmptyGroups: Map<EmotionId, List<EmotionImage>>,
        displayedImageCount: Int,
        mixedGenderInAnswers: Boolean,
    ): Trial {
        val correctImage = imageExhaustionTracker.next(emotionId, pool)
        val promptGender = correctImage.gender
        val correctOption = correctImage.toTrialOption(emotionId)

        val distractorCandidates =
            nonEmptyGroups
                .filterKeys { it != emotionId }
                .flatMap { (otherEmotionId, images) -> images.map { otherEmotionId to it } }
                .let { candidates ->
                    if (mixedGenderInAnswers) candidates else candidates.filter { it.second.gender == promptGender }
                }
        val distractorOptions =
            distractorCandidates
                .shuffled(random)
                .take(displayedImageCount - 1)
                .map { (otherEmotionId, image) -> image.toTrialOption(otherEmotionId) }

        return Trial(
            targetEmotionId = emotionId,
            promptGender = promptGender,
            correctOption = correctOption,
            allOptions = (listOf(correctOption) + distractorOptions).shuffled(random),
        )
    }

    private fun EmotionImage.toTrialOption(emotionId: EmotionId) =
        TrialOption(
            imageId = id,
            imagePath = filePath,
            emotionId = emotionId,
            gender = gender,
        )
}
