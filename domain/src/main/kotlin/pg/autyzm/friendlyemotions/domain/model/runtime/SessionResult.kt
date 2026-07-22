package pg.autyzm.friendlyemotions.domain.model.runtime

import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/**
 * Test-mode session outcome: percentage + correct/total counts (functional-spec §5.7, e.g.
 * "85% (17 / 20)"). Referenced by `ChildScreen.End(result)`; never persisted (target-domain.md §4).
 */
data class SessionResult(
    val correctCount: Int,
    val totalCount: Int,
    val mode: SessionMode,
) {
    init {
        require(totalCount >= 0) { "totalCount must not be negative" }
        require(correctCount in 0..totalCount) { "correctCount must be in 0..$totalCount, was $correctCount" }
    }

    val percentage: Int
        get() = if (totalCount == 0) 0 else (correctCount * 100) / totalCount
}
