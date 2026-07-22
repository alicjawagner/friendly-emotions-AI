package pg.autyzm.friendlyemotions.domain.model.emotion

/**
 * A visual stimulus stored in device storage, belonging to exactly one folder, with an assigned
 * grammatical gender that drives prompt inflection (target-domain.md §3.3).
 */
data class EmotionImage(
    val id: ImageId,
    val folderId: FolderId,
    val filePath: String,
    val gender: GrammaticalGender,
    val isExample: Boolean,
)
