package pg.autyzm.friendlyemotions.child.navigation

import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult

/**
 * The child app's navigation state machine (ADR-012, target-architecture.md §13.2). Rendered by
 * [pg.autyzm.friendlyemotions.child.navigation.ChildNavigationHost]'s exhaustive `when` — no
 * `NavController`, since back navigation for children is intentionally suppressed by design.
 */
sealed class ChildScreen {
    data object Info : ChildScreen()

    data object Main : ChildScreen()

    data object Game : ChildScreen()

    data class End(val result: SessionResult) : ChildScreen()
}
