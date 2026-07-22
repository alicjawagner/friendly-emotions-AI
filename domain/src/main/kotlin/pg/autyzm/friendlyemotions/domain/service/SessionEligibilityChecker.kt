package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep

/**
 * Pre-flight check before the child starts a session (target-domain.md §7.4). `canPlay` is true
 * only when an active `LearningStep` exists and at least one eligible image exists for the current
 * mode — since a non-empty flat image list necessarily belongs to at least one emotion group, no
 * grouping is needed here.
 */
class SessionEligibilityChecker {
    fun canPlay(
        activeStep: LearningStep?,
        eligibleImages: List<EmotionImage>,
    ): Boolean = activeStep != null && eligibleImages.isNotEmpty()
}
