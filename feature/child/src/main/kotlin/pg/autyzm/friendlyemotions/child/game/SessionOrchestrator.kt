package pg.autyzm.friendlyemotions.child.game

import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.service.ErrorCorrectionController
import pg.autyzm.friendlyemotions.domain.service.ErrorCorrectionController.RequeueLayout
import pg.autyzm.friendlyemotions.domain.service.ErrorCorrectionController.TrialOutcome
import pg.autyzm.friendlyemotions.domain.service.TrialPositionRandomizer

private const val FIXED_SLOT_OPTION_LIMIT = 2

/**
 * Sequences a session's [trials] one at a time for `GameViewModel`. Per ADR-014 this is a plain
 * ViewModel delegate, not a domain service — it only sequences already-implemented domain services
 * ([TrialPositionRandomizer], [ErrorCorrectionController]), it adds no business logic of its own.
 *
 * Owns exactly one [positionRandomizer] and one [errorCorrectionController] instance for the
 * session's lifetime, per ADR-014 — both are stateful and session-scoped.
 * [TrialPositionRandomizer]'s methods mutate their position history on every call, so recomputing
 * on every read (rather than reading the cached [currentSlots]) would corrupt that history.
 *
 * Error-correction evaluation runs **once per completed attempt** (when the child finally taps
 * correctly), matching Friendly Words' `hadMistakeThisRound` / `repeatStage` table — not on every
 * wrong tap. A wrong tap or hint-timeout only marks [hadMistakeThisInstance]; the eventual correct
 * tap then feeds that flag into [ErrorCorrectionController.evaluate].
 */
class SessionOrchestrator(
    private val trials: List<Trial>,
    private val sessionMode: SessionMode = SessionMode.LEARNING,
    private val positionRandomizer: TrialPositionRandomizer = TrialPositionRandomizer(),
    private val errorCorrectionController: ErrorCorrectionController = ErrorCorrectionController(),
) {
    /**
     * Working queue seeded from [trials], mutated by [applyRequeue] to splice in a corrective
     * repeat of a failed/not-clean trial immediately after the current position (target-domain.md
     * §14). [trials] itself is kept untouched so [totalCount] always reflects the original, scored
     * trial count — corrective repeats are extra practice, not additional scored attempts.
     */
    private val queue: MutableList<Trial> = trials.toMutableList()

    private var currentIndex = 0

    /**
     * [queue] indices that must reuse a previously captured slot layout rather than being freshly
     * randomized by [positionRandomizer] — populated only for [RequeueLayout.SAME] requeues.
     * Consumed (removed) the moment [renderCurrentTrial] renders that index.
     */
    private val forcedSlotLayouts = mutableMapOf<Int, List<TrialOption?>>()

    var correctCount: Int = 0
        private set

    val totalCount: Int get() = trials.size

    val isComplete: Boolean get() = currentIndex >= queue.size

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

    /**
     * Whether this attempt of [currentTrial] already counts as failed for error correction and
     * reinforcement: a wrong tap **or** a hint-timeout (Friendly Words `hadMistakeThisRound`).
     * Reset whenever a new trial instance becomes current. Exposed as [hintShown] for ViewModel
     * reinforcement (`CLEAN_CORRECT` requires this still `false`).
     */
    var hintShown: Boolean = false
        private set

    init {
        renderCurrentTrial()
    }

    /**
     * Marks the current attempt as failed (wrong tap or hint timeout) without advancing or
     * evaluating [ErrorCorrectionController] — evaluation waits until the child eventually taps
     * correctly. Idempotent; LEARNING-only.
     */
    fun markFailedAttempt() {
        if (sessionMode != SessionMode.LEARNING || currentTrial == null) return
        hintShown = true
    }

    /**
     * Compares [imageId] against [currentTrial]'s correct option.
     *
     * On a correct answer in `LEARNING` mode, evaluates error correction **once for this completed
     * attempt** using Friendly Words' `(repeatStage, hadMistakeThisRound)` table:
     * - failed attempt (`hintShown`) → [TrialOutcome.MISTAKE] (same-layout requeue at stages 0/1,
     *   shuffled at stage 2)
     * - clean attempt at stage 0 → [TrialOutcome.CLEAN_CORRECT] (no requeue)
     * - clean attempt at stage 1/2 → [TrialOutcome.CORRECT_NOT_CLEAN] (shuffled requeue at 1;
     *   recovery complete at 2)
     *
     * On an incorrect answer in `LEARNING` mode, only marks [hintShown] — does not requeue yet.
     * Incorrect answers never advance.
     */
    fun submitAnswer(imageId: ImageId): Boolean {
        val trial = currentTrial ?: return false
        val isCorrect = trial.correctOption.imageId == imageId
        if (isCorrect) {
            if (sessionMode == SessionMode.LEARNING) {
                val outcome = outcomeForCompletedAttempt()
                val requeue = errorCorrectionController.evaluate(outcome)
                applyRequeue(trial, requeue)
                if (requeue == null) correctCount++
            } else {
                correctCount++
            }
            currentIndex++
            renderCurrentTrial()
        } else if (sessionMode == SessionMode.LEARNING) {
            markFailedAttempt()
        }
        return isCorrect
    }

    fun buildResult(mode: SessionMode): SessionResult =
        SessionResult(correctCount = correctCount, totalCount = totalCount, mode = mode)

    /**
     * Maps this attempt's [hintShown] + current `repeatStage` onto [TrialOutcome], matching
     * Friendly Words' end-of-round `(repeatStage, hadMistakeThisRound)` evaluation.
     */
    private fun outcomeForCompletedAttempt(): TrialOutcome =
        when {
            hintShown -> TrialOutcome.MISTAKE
            errorCorrectionController.repeatStage == 0 -> TrialOutcome.CLEAN_CORRECT
            else -> TrialOutcome.CORRECT_NOT_CLEAN
        }

    /**
     * Splices a repeat of [trial] into [queue] immediately after the current position. Called at
     * most once per completed attempt (right before advancing), so there is never a mid-attempt
     * pending duplicate to update in place.
     */
    private fun applyRequeue(
        trial: Trial,
        requeue: RequeueLayout?,
    ) {
        if (requeue == null) return
        val insertIndex = currentIndex + 1
        queue.add(insertIndex, trial)
        if (requeue == RequeueLayout.SAME) {
            forcedSlotLayouts[insertIndex] = currentSlots
        }
    }

    private fun renderCurrentTrial() {
        val trial = queue.getOrNull(currentIndex)
        currentTrial = trial
        hintShown = false
        currentSlots =
            when {
                trial == null -> emptyList()
                else -> forcedSlotLayouts.remove(currentIndex) ?: randomizedSlots(trial)
            }
    }

    private fun randomizedSlots(trial: Trial): List<TrialOption?> =
        if (trial.allOptions.size <= FIXED_SLOT_OPTION_LIMIT) {
            positionRandomizer.assignThreeSlotPositions(trial)
        } else {
            positionRandomizer.randomizePositions(trial).allOptions
        }
}
