package pg.autyzm.friendlyemotions.domain.model.runtime

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId

/** One selectable image within a trial; never persisted (target-domain.md §4.14). */
data class TrialOption(
    val imageId: ImageId,
    val imagePath: String,
    val emotionId: EmotionId,
    val gender: GrammaticalGender,
)
