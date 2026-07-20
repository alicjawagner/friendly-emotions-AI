# ADR-012: Child App Navigation via Sealed Class State Machine

# Status
Accepted

# Context
The Child App has a linear, constrained navigation flow: Info splash → Main screen → Game session → Session End. Children using this app must not be able to navigate backward through the Android back stack — back navigation in the middle of a therapy session would be disruptive and confusing for children with developmental needs.

The Friendly Words analysis (H-4 finding) identified that screen state in the equivalent flow was tracked with string constants, which are not compiler-validated and made the set of valid transitions implicit and error-prone.

Jetpack Navigation Compose's `NavController` supports back navigation by default and requires explicit suppression at every relevant point. It also allows navigating to any route by string, which is too permissive for the child experience.

# Decision
The Child App uses a sealed class state machine instead of Jetpack `NavController`:

```kotlin
sealed class ChildScreen {
    object Info : ChildScreen()
    object Main : ChildScreen()
    object Game : ChildScreen()
    data class End(val result: SessionResult) : ChildScreen()
}
```

`ChildHomeViewModel` exposes a `StateFlow<ChildScreen>`. The single root composable (`ChildNavigationHost`) observes this `StateFlow` and renders the appropriate screen in a `when` expression. The `when` is exhaustive — the Kotlin compiler requires all four variants to be handled.

Back navigation is suppressed by design. The child-facing screens have no back button. There is no `NavController` whose back stack could be popped. The system back gesture, if applicable, is intercepted and suppressed at the Activity level.

The Therapist App, which has complex multi-screen navigation with genuine back-stack requirements, uses Jetpack Navigation Compose with typed, sealed-class routes.

# Consequences
**Benefits:**
- Back navigation for children is structurally impossible — it cannot be accidentally re-enabled by a future code change that adds a `NavController`.
- The set of valid child app screens is exhaustively enumerated by the sealed class — adding a new screen requires updating all `when` expressions that handle `ChildScreen`.
- Screen transitions are testable by asserting on `StateFlow<ChildScreen>` in a ViewModel unit test — no UI instrumentation needed.
- `SessionResult` is passed type-safely as a property of `ChildScreen.End`, eliminating the need for string-based navigation arguments.

**Trade-offs:**
- The child app cannot use Jetpack Navigation Compose's deep-link support or the Navigation animation APIs. This is acceptable for the child's constrained flow.
- Two different navigation approaches exist in the same codebase (sealed class for child, Jetpack Navigation for therapist). Developers must understand when each applies.

**Limitations:**
- This approach does not scale to a navigation graph with many screens or branching paths. It is appropriate specifically for the child app's linear, back-suppressed flow. The therapist app correctly uses `NavController` for its more complex navigation.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §13.2 Child App Navigation State, §20 Architecture Decision Summary (child navigation row)
- `docs/friendly-emotions/friendly-emotions-functional-specification.md` — §5 Child App (linear session flow)
- `docs/friendly-words/06-architecture-improvement-analysis.md` — H-4 finding (string-based navigation state)
