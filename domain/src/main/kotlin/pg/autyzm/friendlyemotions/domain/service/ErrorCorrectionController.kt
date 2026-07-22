package pg.autyzm.friendlyemotions.domain.service

/**
 * Manages the `repeatStage` (0/1/2) re-queue state machine, active in `LEARNING` mode only
 * (target-domain.md §14). The transition table is transcribed verbatim from §14:
 *
 * | repeatStage | Outcome            | Next action                | New repeatStage |
 * |---|---|---|---|
 * | 0 | Clean correct       | No requeue                          | 0 |
 * | 0 | Mistake             | Requeue same layout, immediately after current position | 1 |
 * | 1 | Mistake             | Requeue same layout                 | 1 |
 * | 1 | Correct (not clean) | Requeue shuffled layout              | 2 |
 * | 2 | Mistake             | Requeue shuffled layout              | 2 |
 * | 2 | Correct (not clean) | No requeue (recovery complete)       | 0 |
 *
 * Session-scoped and stateful — create a fresh instance per session, or call [reset] at session
 * start when a longer-lived instance is reused; `repeatStage` resets to 0 at session end (§14).
 */
class ErrorCorrectionController {
    var repeatStage: Int = 0
        private set

    /**
     * Advances the state machine for the outcome of the trial instance just completed and returns
     * the requeue action to perform, if any. [TrialOutcome.CLEAN_CORRECT] is only a valid outcome
     * at `repeatStage == 0`; [TrialOutcome.CORRECT_NOT_CLEAN] is only valid at `repeatStage` 1 or 2 —
     * these are the only combinations the domain model produces (§14).
     */
    fun evaluate(outcome: TrialOutcome): RequeueLayout? {
        val (nextStage, requeue) =
            when (repeatStage) {
                0 ->
                    when (outcome) {
                        TrialOutcome.CLEAN_CORRECT -> 0 to null
                        TrialOutcome.MISTAKE -> 1 to RequeueLayout.SAME
                        TrialOutcome.CORRECT_NOT_CLEAN ->
                            error("CORRECT_NOT_CLEAN is not a valid outcome at repeatStage 0")
                    }
                1 ->
                    when (outcome) {
                        TrialOutcome.MISTAKE -> 1 to RequeueLayout.SAME
                        TrialOutcome.CORRECT_NOT_CLEAN -> 2 to RequeueLayout.SHUFFLED
                        TrialOutcome.CLEAN_CORRECT ->
                            error("CLEAN_CORRECT is not a valid outcome at repeatStage 1")
                    }
                2 ->
                    when (outcome) {
                        TrialOutcome.MISTAKE -> 2 to RequeueLayout.SHUFFLED
                        TrialOutcome.CORRECT_NOT_CLEAN -> 0 to null
                        TrialOutcome.CLEAN_CORRECT ->
                            error("CLEAN_CORRECT is not a valid outcome at repeatStage 2")
                    }
                else -> error("invalid repeatStage $repeatStage")
            }

        repeatStage = nextStage
        return requeue
    }

    /** Resets `repeatStage` to 0. Call at session end/start when reusing one instance. */
    fun reset() {
        repeatStage = 0
    }

    /** Classification of a completed trial instance's outcome, as evaluated by [evaluate]. */
    enum class TrialOutcome {
        CLEAN_CORRECT,
        MISTAKE,
        CORRECT_NOT_CLEAN,
    }

    /** How the re-inserted trial's on-screen option layout should be produced. */
    enum class RequeueLayout {
        SAME,
        SHUFFLED,
    }
}
