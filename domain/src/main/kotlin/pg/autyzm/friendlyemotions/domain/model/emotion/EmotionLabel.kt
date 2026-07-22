package pg.autyzm.friendlyemotions.domain.model.emotion

/**
 * All gendered/locale forms of one emotion's name (target-domain.md §4.3). Polish prompts select
 * [masculine]/[feminine]/[neuter] by the correct-answer image's gender; English prompts always use
 * [neutral].
 */
data class EmotionLabel(
    val masculine: String,
    val feminine: String,
    val neuter: String,
    val neutral: String,
) {
    init {
        require(masculine.isNotBlank()) { "masculine must not be blank" }
        require(feminine.isNotBlank()) { "feminine must not be blank" }
        require(neuter.isNotBlank()) { "neuter must not be blank" }
        require(neutral.isNotBlank()) { "neutral must not be blank" }
    }
}
