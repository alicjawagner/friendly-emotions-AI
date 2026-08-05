package pg.autyzm.friendlyemotions.child.game

import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult

/**
 * One-shot navigation event emitted by [GameViewModel] through a `Channel` (target-architecture.md
 * §13.1/§7 — never a `Boolean` flag in `UiState`). `GameViewModel` cannot mutate
 * `ChildHomeViewModel`'s `ChildScreen` state directly (phase-6 plan decision #5), so
 * `ChildNavigationHost` collects this event via `collectAsEffect()` and forwards it to
 * `ChildHomeViewModel.onSessionComplete`.
 */
sealed class GameNavigationEvent {
    data class SessionCompleted(val result: SessionResult) : GameNavigationEvent()
}
