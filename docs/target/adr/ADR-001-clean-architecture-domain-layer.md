# ADR-001: Clean Architecture with Pure Kotlin Domain Layer

# Status
Accepted

# Context
Friendly Emotions is built from first principles rather than migrated from the Friendly Words codebase. The primary lesson from the Friendly Words architecture analysis was that business rules were scattered across ViewModels, repositories, and even composables, making them difficult to test in isolation and impossible to validate without an Android runtime.

The application has complex business logic: a therapeutic session state machine, gender-inflected prompt rendering, error-correction re-queuing, image pool cycling, and a configurable five-tab learning step wizard. All of these rules must be exercised reliably in automated tests and must be stable against changes to the Android SDK, Jetpack Compose, or Room.

Additionally, the domain is rich enough to warrant a clear separation between what the business rules say (domain) and how those rules are persisted or displayed (infrastructure/presentation).

# Decision
The architecture follows Clean Architecture with three layers:

- **Domain layer** (`:domain` Gradle module): pure Kotlin. Contains entities, value objects, aggregates, repository interfaces, use cases, and domain services. Has zero imports from `android.*`, `androidx.*`, Room, Hilt, or Compose. Dependencies point inward — the domain depends on nothing outside itself.
- **Data layer** (`:data` Gradle module): implements the domain's repository interfaces. Owns Room entities, DAOs, mappers, DataStore, and file I/O. Depends on `:domain`.
- **Presentation layer** (`:feature:child`, `:feature:therapist` Gradle modules): contains Compose screens and ViewModels. Depends on `:domain` and `:core:ui` only — never on `:data`.

The `:app` module is the application shell that wires the layers together via Hilt.

# Consequences
**Benefits:**
- Domain services (`TrialGenerator`, `PromptRenderer`, `ErrorCorrectionController`, etc.) are testable with plain Kotlin unit tests — no Robolectric, no instrumented test runner.
- The domain is stable against changes to persistence technology (e.g. migrating from Room to SQLDelight would only touch `:data`).
- Business invariants have a single, authoritative home — impossible for them to silently diverge between layers.
- New developers can understand all business rules by reading `:domain` alone.

**Trade-offs:**
- More initial boilerplate: separate module, mapper classes at the repository boundary, explicit use case classes per operation.
- Slightly more indirection when tracing a UI action to its data source (UI → use case → repository interface → implementation).

**Limitations:**
- The `:domain` module must be treated as a strict no-Android zone. Any inadvertent import of an Android type breaks the entire contract and must be caught in code review or enforced via a lint rule.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §2 Architectural Principles, §4 Layer Architecture, §21 Top Architectural Principles for Implementation
- `docs/friendly-words/06-architecture-improvement-analysis.md` — source of H-1, H-3 findings that motivated this decision
