package pg.autyzm.friendlyemotions.domain.model.emotion

/**
 * A therapist-managed grouping of images within exactly one emotion (target-domain.md §3.2).
 * [genderPolicy] is immutable after creation; this class cannot enforce that across time, so it is
 * the repository implementation's responsibility to reject updates that modify it (§6.2).
 */
data class EmotionFolder(
    val id: FolderId,
    val emotionId: EmotionId,
    val name: String,
    val genderPolicy: FolderGenderPolicy,
    val isExample: Boolean,
)
