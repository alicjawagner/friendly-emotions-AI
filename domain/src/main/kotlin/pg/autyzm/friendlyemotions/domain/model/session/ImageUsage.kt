package pg.autyzm.friendlyemotions.domain.model.session

import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId

/**
 * One selected image's eligibility per mode (target-domain.md §4.8). An entry with both flags
 * `false` is effectively deselected.
 */
data class ImageUsage(
    val imageId: ImageId,
    val inLearning: Boolean,
    val inTest: Boolean,
)
