# ADR-007: Repository Pattern with Reactive Data Contracts

# Status
Accepted

# Context
The domain layer defines business rules through use cases and domain services. Those rules need access to persistent data, but the domain must not know or care whether that data lives in Room, a remote API, or an in-memory cache. Furthermore, some screens (the child home screen, the learning step list) must update reactively when the underlying data changes without polling.

The boundary between the domain and data layers needs a clear, stable contract.

# Decision
Repository interfaces are defined in `:domain/repository/` and are the only means by which the domain (use cases) and presentation (ViewModels) access persistent state. Implementations live in `:data/repository/`.

The data contract for each repository follows two patterns:

- **Live data (observations):** methods that return `Flow<T>`. These are used when a screen needs to stay in sync with the database in real time (e.g. `LearningStepRepository.observeActiveStep()`, `EmotionFolderRepository.observeFoldersForEmotion()`). The ViewModel collects the flow in `viewModelScope`.
- **One-shot reads and writes:** `suspend fun` methods. Used for single fetches by ID, creates, updates, and deletes.

Repository implementation rules:
- Repository implementations contain no business logic. Validation, orchestration, and invariant enforcement live in use cases.
- The mapping between Room entities and domain models is the responsibility of the repository implementation (via mapper classes), not the DAO or the use case.
- Room entities never leave the `:data` module.
- Multi-step writes that enforce a domain invariant are wrapped in `@Transaction` (see ADR-009).

The `PreferencesRepository` uses Jetpack DataStore instead of Room and follows the same interface pattern.

# Consequences
**Benefits:**
- ViewModels and use cases program to the interface, making it trivial to substitute a fake repository in unit tests.
- Reactive `Flow` contracts mean the UI automatically reflects database changes — no polling, no manual refresh calls.
- Mappers at the repository boundary keep domain models clean and stable when the Room schema evolves.
- The domain is completely decoupled from persistence implementation details.

**Trade-offs:**
- Mapper classes are additional boilerplate that must be kept in sync when either the domain model or the Room entity changes.
- `Flow` semantics require developers to understand cold vs. hot streams and backpressure to avoid accidental resource leaks.

**Limitations:**
- The current contract does not include pagination (`PagingSource`). If material catalogs grow very large, the repository interface would need to be extended to support paged queries — a non-breaking change if the interface is extended rather than modified.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §10 Repository Architecture, §2 Architectural Principle #6
- `docs/friendly-emotions/target-domain.md` — §5 Aggregates (repository boundary per aggregate)
