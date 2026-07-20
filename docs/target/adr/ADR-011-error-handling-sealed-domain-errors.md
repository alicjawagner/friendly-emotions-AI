# ADR-011: Error Handling with Sealed Domain Error Hierarchy

# Status
Accepted

# Context
Android applications commonly handle errors in one of three ways: uncaught exceptions that crash the app, silently swallowed exceptions that leave the UI in a stale state, or generic error messages that give the user no actionable information. All three are problematic in a therapeutic context where the app must remain stable during sessions.

Friendly Emotions has a variety of error conditions that require distinct handling: validation failures (blank or duplicate step names), constraint violations (attempt to delete example content), business rule violations (MIXED-folder images missing a gender), file I/O failures, and session eligibility failures. Each type has a different appropriate UI response — an inline field error, a dialog, a Snackbar with a retry option, or a disabled button.

An exception-based error model forces callers to remember to catch exceptions and does not communicate which error types are expected vs. unexpected. A generic `Boolean` or nullable return forces callers to guess what went wrong.

# Decision
All use cases that can fail with a domain-defined error condition return `Result<Output, DomainError>`. `DomainError` is a sealed class in `:domain/error/` with a variant for each distinct failure mode:

```
sealed class DomainError {
    data class FolderNotFound(val folderId: FolderId)
    data class DuplicateStepName(val name: String)
    object StepNameBlank
    object NoExampleStepAvailable
    data class GenderNotAssigned(val imageIds: List<ImageId>)
    object ExampleContentNotDeletable
    object InsufficientMaterialForSession
    data class FileOperationFailed(val cause: Throwable)
}
```

ViewModels map the `Result` to UI state:
- `Success` → update `UiState` with the successful data.
- `Failure(DomainError)` → update `UiState` with an `ErrorUiState` describing the error and the appropriate recovery action.

Screens render errors as inline field messages, dialogs, or Snackbars depending on the error category — never as unhandled crashes.

Unexpected runtime exceptions (programming errors, out-of-memory, etc.) are caught in a ViewModel `catch {}` block, logged, and surfaced as a generic error Snackbar. They are never silently swallowed.

# Consequences
**Benefits:**
- Every call site receives a typed result — the compiler prevents ignoring a failure case.
- `when (error)` on the sealed class is exhaustive, so adding a new `DomainError` variant forces all handling sites to be updated.
- The UI can give specific, actionable feedback for each failure type instead of a generic "something went wrong."
- Domain errors have no dependency on Android — they can be asserted on in plain Kotlin unit tests.

**Trade-offs:**
- Every use case that can fail requires its callers to explicitly unwrap the `Result`. This is verbose but deliberate — it prevents silent swallowing.
- The `Result` type must be consistently used throughout. Mixing `Result`-returning and exception-throwing use cases would create an inconsistent contract.

**Limitations:**
- `DomainError.FileOperationFailed` wraps a `Throwable` for file I/O errors originating in the data layer. This is the only place where an infrastructure exception surfaces into the domain error model; it should be treated as a recoverable error, not a programming error.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §15 Error Handling Strategy
- `docs/friendly-emotions/target-domain.md` — §8 Business Rules (the rules whose violation each DomainError variant represents)
