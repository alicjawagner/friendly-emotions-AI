# ADR-005: Shared Data Architecture Between Child App and Therapist App

# Status
Accepted

# Context
Friendly Emotions ships as a single APK with two distinct entry points: a Child App (used during therapy sessions by the child) and a Therapist App (used by the therapist to configure teaching materials and learning steps). Both run in the same process and share the same device storage.

The core interaction pattern is: the therapist activates a learning step in the Therapist App, and the Child App must reflect this change immediately on the next session start — without network communication, without manual synchronization, and without restarting the application.

A naive approach of duplicating data access per entry point would require explicit notification between the two sides and introduce synchronization bugs. An approach of direct inter-Activity communication would tightly couple the two feature modules.

# Decision
Both apps share a single Room database (`friendly_emotions`) accessed through the `:data` module, which is a singleton in the Hilt component graph. All data access flows through domain repository interfaces, implemented by `:data` and injected by Hilt in `:app`.

Live data is exposed as Kotlin `Flow<T>` from repository interfaces. The Child App observes `LearningStepRepository.observeActiveStep()` as a `StateFlow`. When the Therapist App calls `ActivateLearningStepUseCase`, it executes an atomic Room `@Transaction` that updates the database. Room's `Flow` infrastructure automatically emits the new value to all active collectors — the Child App receives the update without any explicit notification or shared state object.

The data module is a strict singleton:
- `AppDatabase` is `@Singleton`-scoped in Hilt.
- All DAOs are derived from the single database instance.
- Repository implementations are `@Singleton`-scoped.

Feature modules (`:feature:child`, `:feature:therapist`) never declare a dependency on `:data`. They interact only with domain repository interfaces injected by Hilt.

# Consequences
**Benefits:**
- No synchronization code needed between the two apps — Room's reactive `Flow` layer handles propagation automatically.
- The therapist can activate a step, and the child's home screen updates reactively if both are visible on a split-screen display.
- A single source of truth for all persistent state eliminates consistency issues.
- Feature modules remain fully decoupled from each other and from the persistence implementation.

**Trade-offs:**
- The single-database design assumes both entry points run in the same process. If future requirements demand running them as separate processes or on separate devices, this design would need significant rework.
- Developers must understand that changes made through therapist use cases will immediately affect the live state observed by child ViewModels.

**Limitations:**
- There is no offline-sync, conflict resolution, or multi-user capability. This is by design — the app is explicitly fully offline and single-device (stated in the functional specification).

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §3 Module Structure, §5.3 Shared Concerns, §8.3 Learning Step Activation, §9.1 Room Database
- `docs/friendly-emotions/friendly-emotions-functional-specification.md` — §10 Inherited Unchanged from Friendly Words (fully offline, Room as sole store)
