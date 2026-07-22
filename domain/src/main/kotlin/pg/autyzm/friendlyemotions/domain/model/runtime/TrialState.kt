package pg.autyzm.friendlyemotions.domain.model.runtime

/** Trial state machine, held by `GameViewModel` (target-architecture.md §7.3). */
sealed class TrialState {
    object AwaitingResponse : TrialState()

    object HintVisible : TrialState()

    data class Judged(val verdict: TrialVerdict) : TrialState()
}
