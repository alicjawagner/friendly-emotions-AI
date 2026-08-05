package pg.autyzm.friendlyemotions.child.game

import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.service.TrialPositionRandomizer

private const val FIXED_SLOT_OPTION_LIMIT = 2

/**
 * Sequences a session's [trials] one at a time for `GameViewModel`. Per ADR-014 this is a plain
 * ViewModel delegate, not a domain service — it only sequences already-implemented domain services
 * ([TrialPositionRandomizer]), it adds no business logic of its own.
 *
 * Owns exactly one [positionRandomizer] instance for the session's lifetime and calls it exactly
 * once per trial, caching the result in [currentSlots]. [TrialPositionRandomizer]'s methods are
 * stateful and mutate their position history on every call, so recomputing on every read (rather
 * than reading the cached value) would corrupt that history.
 */
class SessionOrchestrator(
    private val trials: List<Trial>,
    private val positionRandomizer: TrialPositionRandomizer = TrialPositionRandomizer(),
) {
    private var currentIndex = 0

    var correctCount: Int = 0
        private set

    val totalCount: Int get() = trials.size

    val isComplete: Boolean get() = currentIndex >= trials.size

    /** The raw trial currently being presented — `null` once [isComplete]. */
    var currentTrial: Trial? = null
        private set

    /**
     * Presentation-ready layout for [currentTrial], chosen per target-domain.md §7.6/§8.9 and
     * functional-spec §5.2.2: 1–2 options use [TrialPositionRandomizer.assignThreeSlotPositions]
     * (exactly 3 entries, unused slots `null`); 3+ options use
     * [TrialPositionRandomizer.randomizePositions] (exactly N entries, never `null`). Computed once
     * per trial and cached — callers don't need to know which regime produced it, only whether
     * `currentSlots.size <= 3` (fixed three-slot layout) or `> 3` (flowing grid).
     */
    var currentSlots: List<TrialOption?> = emptyList()
        private set

    init {
        renderCurrentTrial()
    }

    /**
     * Compares [imageId] against [currentTrial]'s correct option. On a correct answer, increments
     * [correctCount], advances to the next trial (if any) and recomputes/caches its [currentSlots].
     * On an incorrect answer, does nothing but return `false` — the current trial stays displayed.
     */
    fun submitAnswer(imageId: ImageId): Boolean {
        val trial = currentTrial ?: return false
        val isCorrect = trial.correctOption.imageId == imageId
        if (isCorrect) {
            correctCount++
            currentIndex++
            renderCurrentTrial()
        }
        return isCorrect
    }

    fun buildResult(mode: SessionMode): SessionResult =
        SessionResult(correctCount = correctCount, totalCount = totalCount, mode = mode)

    private fun renderCurrentTrial() {
        val trial = trials.getOrNull(currentIndex)
        currentTrial = trial
        currentSlots =
            when {
                trial == null -> emptyList()
                trial.allOptions.size <= FIXED_SLOT_OPTION_LIMIT -> positionRandomizer.assignThreeSlotPositions(trial)
                else -> positionRandomizer.randomizePositions(trial).allOptions
            }
    }
}
