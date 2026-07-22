package pg.autyzm.friendlyemotions.domain.model.session

/** Which images a `LearningStep` includes and for which modes (target-domain.md §4.7). */
data class MaterialSelection(
    val imageUsages: List<ImageUsage>,
)
