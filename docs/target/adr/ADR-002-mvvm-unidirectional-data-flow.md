# ADR-002: MVVM with Unidirectional Data Flow

# Status
Accepted

# Context
Friendly Emotions has two distinct user interfaces — the Child App (a constrained game UI) and the Therapist App (a complex configuration wizard with multi-tab state). Both need a predictable, testable way to manage screen state. The Friendly Words analysis identified cases where UI state was driven by bidirectional bindings and Boolean flags that led to race conditions (e.g. double-navigation bugs) and recomposition-driven side effects (e.g. hint timers started inside composables).

A consistent state management contract across all screens reduces the cognitive overhead of switching between the child and therapist feature modules and makes ViewModel unit tests straightforward.

# Decision
Every screen in both the Child App and the Therapist App follows the MVVM pattern with Unidirectional Data Flow (UDF):

- **`*UiState`** — an immutable data class (or sealed class for multi-state screens) representing the complete snapshot of everything the screen needs to render.
- **`*UiEvent`** — a sealed class representing all user intents (button taps, text changes, navigation requests).
- **`*ViewModel`** — processes incoming `UiEvent`s, calls use cases, and updates a `StateFlow<UiState>` for the screen to collect.
- **`*Screen` (Composable)** — collects the `StateFlow`, renders from the received state, and emits `UiEvent`s upward. It never holds business state or launches side effects with business logic.

One-shot navigation events use `Channel<NavigationEvent>` consumed via `collectAsEffect()`, not a Boolean flag in `UiState`. This eliminates the double-navigation race condition that arises when a Boolean flag survives recomposition.

# Consequences
**Benefits:**
- State is always a pure function of user events applied in the ViewModel — easy to reason about and replay in tests.
- Composables are stateless renderers with no hidden mutable state, eliminating recomposition-driven side effects.
- One-shot events (navigation, Snackbar, dialogs) are consumed exactly once through `Channel`, making them safe across configuration changes.
- ViewModels are straightforward to unit-test: inject fake use cases, emit events, assert on resulting `UiState`.

**Trade-offs:**
- Every screen requires a `UiState` class, a `UiEvent` class, and a ViewModel, even for simple screens. This is intentional boilerplate that enforces consistency.
- Developers must understand `StateFlow` collection and `Channel` semantics to contribute effectively.

**Limitations:**
- `collectAsEffect()` must be called correctly in composables to avoid consuming navigation events multiple times. This is a convention that must be documented and reviewed.
- Animation or haptic feedback triggered by state transitions must still be handled carefully in composables to avoid running on every recomposition.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §2 Architectural Principles (#2), §12 ViewModel Responsibilities, §13 State Management
- `docs/friendly-words/06-architecture-improvement-analysis.md` — C-4 finding (TTS in composable), H-4 finding (string-based navigation state)
