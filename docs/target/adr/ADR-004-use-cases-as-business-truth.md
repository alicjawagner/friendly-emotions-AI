# ADR-004: Use Cases as the Single Source of Business Truth

# Status
Accepted

# Context
The Friendly Words analysis (H-1 finding) revealed that business logic was duplicated and spread across ViewModels, repositories, and occasionally composables. Invariants such as name uniqueness checks, cascade deletion logic, and active-step fallback activation had no single canonical home. This made it difficult to answer the question "where is this business rule enforced?" and made the rules easy to bypass accidentally.

Friendly Emotions has a richer rule set: grammatical gender invariants for MIXED-policy folders, active-step singleton enforcement, test-parameter inheritance from learning parameters, session eligibility checks, and error-correction stage management. Each of these must have exactly one authoritative implementation.

# Decision
Every discrete business operation is encapsulated in exactly one use case class in the `:domain/usecase/` package. Use cases:

- Are named with a verb phrase describing the operation (e.g. `ActivateLearningStepUseCase`, `ValidateLearningStepNameUseCase`, `AssignImagesUseCase`).
- Receive all inputs as parameters and return `Result<Output, DomainError>` for fallible operations, or `Flow<T>` for observation.
- Contain all validation, orchestration, and domain service calls required for that operation.
- Depend only on repository interfaces and domain services — never on DAOs, Room entities, or Android types.
- Use the `operator fun invoke(...)` convention so callers read naturally as `useCase(params)`.

ViewModels call use cases exclusively. They never inject repositories or DAOs directly. If no use case exists for a needed operation, one must be created — not worked around by putting logic in the ViewModel.

Repositories contain no business logic. They perform data access, entity mapping, and file I/O only. Business decisions (e.g. whether a step can be deleted, whether a name is valid) belong in use cases.

# Consequences
**Benefits:**
- "Where is this rule enforced?" has a definitive answer: the use case named after the operation.
- Use cases are independently unit-testable with fake repositories — no ViewModel or Android runtime needed.
- Invariants cannot be silently bypassed by a ViewModel that decides to write directly to a repository.
- Adding a new business operation is a localized change: one new use case class, no changes to existing ones.

**Trade-offs:**
- More classes per feature. A simple CRUD operation still requires a use case class rather than a direct repository call from the ViewModel.
- The discipline of "never call the repository from a ViewModel" must be enforced in code review; there is no compile-time prevention for this specific constraint.

**Limitations:**
- Use cases are intentionally single-operation. Cross-cutting workflows that span multiple use cases (e.g. the wizard save flow) are orchestrated by the ViewModel or a coordinator, not by use cases calling each other.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §2 Architectural Principles (#3), §11 Use Case Organization, §12 ViewModel Responsibilities
- `docs/friendly-words/06-architecture-improvement-analysis.md` — H-1 finding (business logic scattered)
