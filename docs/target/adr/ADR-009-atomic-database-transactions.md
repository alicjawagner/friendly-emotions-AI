# ADR-009: Atomic Database Transactions for Domain Invariants

# Status
Accepted

# Context
Several business invariants in Friendly Emotions require multiple database rows to be modified atomically:

- **Active-step singleton:** exactly one `LearningStep` can have `isActive = true` at any time. Switching the active step requires deactivating the current one and activating the target in the same operation.
- **Deletion with fallback:** deleting the currently active learning step requires immediately activating the first available example step. If the deletion succeeds but the fallback activation fails, the system is left with no active step.
- **Folder deletion cascade:** deleting a folder must delete all its images and remove them from any learning step's material selection. Partial deletion leaves orphaned references.

The Friendly Words analysis (C-5 finding) identified that these multi-step operations were executed as sequential `suspend` calls without wrapping them in a database transaction. If the process was killed between steps, or if a second operation interleaved, the database could be left in an inconsistent state.

# Decision
All database operations that enforce a domain invariant across multiple rows or tables are wrapped in a single Room `@Transaction` (or `withTransaction {}` for programmatic use). This is a non-negotiable architectural rule.

Specifically:

| Operation | Why a transaction is required |
|---|---|
| `LearningStepDao.activateStep(stepId)` | Deactivates the current active step and activates the target in one atomic unit (mode is set independently via `updateMode`, not by this transaction) |
| `LearningStepDao.deleteStepWithFallback(stepId)` | Deletes the step and activates the fallback example step together |
| `EmotionFolderDao.deleteFolderWithCascade(folderId)` | Deletes the folder, its images, and removes their image usage records from all steps |
| `ImageUsageDao.replaceForStep(stepId, usages)` | Deletes all existing usages for a step and inserts the new batch as one unit |

The use case layer orchestrates *which* repository method to call. The transaction boundary is always inside the repository implementation or DAO — never across two separate repository calls from a use case.

`ForeignKeyConstraints` are enforced via `onOpen` callback in Room to ensure referential integrity is always active.

# Consequences
**Benefits:**
- The active-step singleton invariant is guaranteed at the database level, not just at the application level. No concurrent coroutine or interleaved operation can observe an intermediate state.
- Partial failure of a multi-step operation leaves the database unchanged — not in a corrupt intermediate state.
- Eliminates the class of bugs where "deactivate old step succeeded but activate new step failed."

**Trade-offs:**
- `@Transaction` in Room blocks the database writer for the duration of the transaction. For the operations involved (a handful of row updates), this is negligible in practice.
- Developers must understand that `@Transaction`-annotated DAO methods must not call other `@Transaction` methods that open a new transaction — they must be composed carefully.

**Limitations:**
- Room transactions are local to the device database. This decision provides no protection against external modification of the SQLite file (e.g. via ADB shell). This is an acceptable limitation for an offline therapeutic app.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §2 Architectural Principle #5, §8.3 Learning Step Activation, §10.2 Repository Rules, §21 Top Architectural Principles for Implementation (#4)
- `docs/friendly-emotions/target-domain.md` — §7.3 LearningStepActivationService, §8.4 Learning Step Rules
- `docs/friendly-words/06-architecture-improvement-analysis.md` — C-5 finding (non-atomic multi-step operations)
