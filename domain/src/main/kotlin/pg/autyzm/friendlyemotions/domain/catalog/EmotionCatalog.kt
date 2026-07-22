package pg.autyzm.friendlyemotions.domain.catalog

import pg.autyzm.friendlyemotions.domain.model.emotion.Emotion
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionLabel

/**
 * The 6 fixed emotion constants (target-domain.md §3.1). Never created, renamed, or deleted at
 * runtime — `DatabaseInitializer` seeds matching rows from this object in `:data` (target-architecture.md §6.3).
 *
 * For the Polish locale, [EmotionLabel.neutral] mirrors [EmotionLabel.masculine]: it is never read by
 * `PromptRenderer` for Polish (selection is gender-based there), it only exists to satisfy the shared
 * [EmotionLabel] shape. Likewise, for the English locale, [EmotionLabel.masculine]/[EmotionLabel.feminine]/
 * [EmotionLabel.neuter] all mirror [EmotionLabel.neutral], since English has no grammatical gender
 * distinction to select from.
 */
object EmotionCatalog {
    const val LOCALE_POLISH = "pl"
    const val LOCALE_ENGLISH = "en"

    val HAPPY =
        polishAndEnglish(
            id = EmotionId.HAPPY,
            masculine = "wesoły",
            feminine = "wesoła",
            neuter = "wesołe",
            english = "happy",
        )

    val SAD =
        polishAndEnglish(
            id = EmotionId.SAD,
            masculine = "smutny",
            feminine = "smutna",
            neuter = "smutne",
            english = "sad",
        )

    val SURPRISED =
        polishAndEnglish(
            id = EmotionId.SURPRISED,
            masculine = "zdziwiony",
            feminine = "zdziwiona",
            neuter = "zdziwione",
            english = "surprised",
        )

    val ANGRY =
        polishAndEnglish(
            id = EmotionId.ANGRY,
            masculine = "zły",
            feminine = "zła",
            neuter = "złe",
            english = "angry",
        )

    val SCARED =
        polishAndEnglish(
            id = EmotionId.SCARED,
            masculine = "przestraszony",
            feminine = "przestraszona",
            neuter = "przestraszone",
            english = "scared",
        )

    val BORED =
        polishAndEnglish(
            id = EmotionId.BORED,
            masculine = "znudzony",
            feminine = "znudzona",
            neuter = "znudzone",
            english = "bored",
        )

    val all: List<Emotion> = listOf(HAPPY, SAD, SURPRISED, ANGRY, SCARED, BORED)

    fun get(emotionId: EmotionId): Emotion = all.first { it.id == emotionId }

    private fun polishAndEnglish(
        id: EmotionId,
        masculine: String,
        feminine: String,
        neuter: String,
        english: String,
    ): Emotion =
        Emotion(
            id = id,
            labels =
                mapOf(
                    LOCALE_POLISH to
                        EmotionLabel(
                            masculine = masculine,
                            feminine = feminine,
                            neuter = neuter,
                            neutral = masculine,
                        ),
                    LOCALE_ENGLISH to
                        EmotionLabel(
                            masculine = english,
                            feminine = english,
                            neuter = english,
                            neutral = english,
                        ),
                ),
        )
}
