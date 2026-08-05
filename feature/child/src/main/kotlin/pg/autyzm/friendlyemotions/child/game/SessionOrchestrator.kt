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
     * Index in [queue] of the not-yet-current corrective repeat for the trial presently being
     * corrected, if any. Repeated `evaluate()` calls during the same correction cycle (e.g. a 2nd/3rd
     * wrong tap on the same still-displayed instance) update the entry at this index in place via
     * [applyRequeue] instead of inserting a new one each time — otherwise every extra wrong tap on
     * one instance would splice in its own duplicate, some of which would never get "consumed" once
     * `repeatStage` recovers to 0, silently inflating [correctCount] past [totalCount] if answered.
     * Cleared once [renderCurrentTrial] advances onto it (it's then the live current trial, not a
     * pending one).
     */
    private var pendingRetryIndex: Int? = null

    /**
     * [queue] indices that must reuse a previously captured slot layout rather than being freshly
     * randomized by [positionRandomizer] — populated only for [RequeueLayout.SAME] requeues (target-
     * domain.md §14 "re-inserted with identical option positions"). Consumed (removed) the moment
     * [renderCurrentTrial] renders that index.
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
     * Whether a hint has been shown for [currentTrial]'s current instance (target-domain.md §12) —
     * reset to `false` whenever a new trial becomes current. Set by a wrong tap in `LEARNING` mode
     * (this session/phase); consumed by `GameViewModel` in Phase 7.4 to compute `TrialVerdict`
     * (`CLEAN_CORRECT` requires this to still be `false` at the time of a correct tap).
     */
    var hintShown: Boolean = false
        private set

    init {
        renderCurrentTrial()
    }

    /**
     * Compares [imageId] against [currentTrial]'s correct option.
     *
     * On a correct answer in `LEARNING` mode, evaluates the error-correction outcome
     * ([TrialOutcome.CLEAN_CORRECT] if no mistake has occurred yet this correction cycle,
     * [TrialOutcome.CORRECT_NOT_CLEAN] otherwise per target-domain.md §14) and requeues a repeat via
     * [applyRequeue] if the controller calls for one; [correctCount] only increments once the trial
     * is fully resolved (no further requeue). In `TEST` mode (no error correction), a correct answer
     * always increments [correctCount]. Either way, a correct answer then advances to the next
     * queued trial.
     *
     * On an incorrect answer in `LEARNING` mode, marks [hintShown] (per target-domain.md §12, "the
     * first wrong tap ... marks the trial 'has mistake'" — hints are shown immediately on a wrong
     * tap) and evaluates [TrialOutcome.MISTAKE], requeuing a repeat via [applyRequeue]. Either way,
     * an incorrect answer never advances — the current trial stays displayed.
     */
    fun submitAnswer(imageId: ImageId): Boolean {
        val trial = currentTrial ?: return false
        val isCorrect = trial.correctOption.imageId == imageId
        if (isCorrect) {
            if (sessionMode == SessionMode.LEARNING) {
                val outcome =
                    if (errorCorrectionController.repeatStage == 0) {
                        TrialOutcome.CLEAN_CORRECT
                    } else {
                        TrialOutcome.CORRECT_NOT_CLEAN
                    }
                val requeue = errorCorrectionController.evaluate(outcome)
                applyRequeue(trial, requeue)
                if (requeue == null) correctCount++
            } else {
                correctCount++
            }
            currentIndex++
            renderCurrentTrial()
        } else if (sessionMode == SessionMode.LEARNING) {
            hintShown = true
            val requeue = errorCorrectionController.evaluate(TrialOutcome.MISTAKE)
            applyRequeue(trial, requeue)
        }
        return isCorrect
    }

    fun buildResult(mode: SessionMode): SessionResult =
        SessionResult(correctCount = correctCount, totalCount = totalCount, mode = mode)

    /**
     * Splices a repeat of [trial] into [queue] immediately after the current position, or — if a
     * repeat for this same correction cycle is already pending (see [pendingRetryIndex]) — updates
     * that pending entry's required layout in place rather than inserting another one.
     */
    private fun applyRequeue(
        trial: Trial,
        requeue: RequeueLayout?,
    ) {
        if (requeue == null) return
        val pendingIndex = pendingRetryIndex
        if (pendingIndex != null) {
            if (requeue == RequeueLayout.SAME) {
                forcedSlotLayouts[pendingIndex] = currentSlots
            } else {
                forcedSlotLayouts.remove(pendingIndex)
            }
            return
        }
        val insertIndex = currentIndex + 1
        queue.add(insertIndex, trial)
        pendingRetryIndex = insertIndex
        if (requeue == RequeueLayout.SAME) {
            forcedSlotLayouts[insertIndex] = currentSlots
        }
    }

    private fun renderCurrentTrial() {
        if (pendingRetryIndex == currentIndex) pendingRetryIndex = null
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
