package pg.autyzm.friendlyemotions.child.end

import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/**
 * UI state for `SessionEndScreen`, sourced from [SessionEndViewModel]. The score fields are only
 * displayed in `TEST` mode (functional-spec §5.6/§5.7 — learning mode shows no score); [mode] is
 * exposed directly so the screen itself decides visibility rather than carrying a redundant
 * `showScorePanel` boolean.
 *
 * [incorrectCount]/[incorrectPercentage] are computed independently from `totalCount - correctCount`
 * (mirroring `SessionResult.percentage`'s own division) rather than as `100 - correctPercentage`, to
 * avoid an integer-rounding mismatch against the Figma example ("8/10 → 80%" / "2/10 → 20%").
 */
data class SessionEndUiState(
    val mode: SessionMode = SessionMode.LEARNING,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val correctPercentage: Int = 0,
    val incorrectCount: Int = 0,
    val incorrectPercentage: Int = 0,
    val showConfetti: Boolean = false,
)
