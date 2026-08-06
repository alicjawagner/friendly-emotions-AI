package pg.autyzm.friendlyemotions.domain.model.session

/** Praise and animation settings (target-domain.md §4.11). */
data class ReinforcementSettings(
    val enabledPraiseWords: Set<String> = PRAISE_WORDS,
    val animationsEnabled: Boolean = true,
    val endSessionAnimationEnabled: Boolean = true,
    val endSessionFanfareEnabled: Boolean = true,
) {
    companion object {
        val PRAISE_WORDS: Set<String> =
            setOf(
                "dobrze",
                "super",
                "świetnie",
                "ekstra",
                "rewelacja",
                "brawo",
            )
        val ANIMATION_THEMES: Set<String> =
            setOf(
                "flowers",
                "butterflies",
                "balloons",
                "cars",
                "balls",
            )
    }
}
