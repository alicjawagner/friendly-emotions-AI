package pg.autyzm.friendlyemotions.domain.model.emotion

/**
 * An image submitted for addition to a folder, before it has been persisted and assigned an
 * [ImageId] (target-domain.md §3.3, §9.4). [gender] is nullable to represent the transient
 * `AWAITING_GENDER` state for images added to a `MIXED` folder; images added to a fixed-gender
 * folder are auto-assigned that folder's gender by `AssignImagesUseCase` (session 2.6) before
 * `EmotionImageRepository.addImages` is called, so by the repository boundary every image's
 * gender is resolved.
 */
data class NewEmotionImage(
    val filePath: String,
    val gender: GrammaticalGender?,
)
