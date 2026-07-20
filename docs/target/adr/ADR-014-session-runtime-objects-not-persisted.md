# ADR-014: Session Runtime Objects Are Never Persisted

# Status
Accepted

# Context
A learning session consists of a generated list of `Trial` objects, each containing a correct answer and a set of distractor options, a rendered prompt, and the current trial state. It would be technically possible to persist these objects to the database so a session could be resumed after a process restart.

However, persisting session runtime objects introduces significant complexity: schema migrations when the trial format changes, reconciliation of persisted trial state with possibly changed material (an image may have been deleted since the session was saved), and the overhead of transactional writes on every trial advance.

The therapeutic context also does not require session resumption — a therapy session interrupted by a process restart should simply start fresh from the next launch.

# Decision
`Trial`, `TrialOption`, `RenderedPrompt`, `SessionResult`, `TrialState`, and `TrialVerdict` are in-memory value objects. They are created at session start by `TrialGenerator` and `SessionOrchestrator`, exist only for the lifetime of `GameViewModel`, and are discarded when the session ends or the process is killed.

The database stores only configuration: `LearningStep`, `MaterialSelection`, `LearningParameters`, `TestParameters`, and `ReinforcementSettings`. Session runtime is always derived from this configuration at session start.

The `SessionOrchestrator` — the only stateful runtime component — is a delegate inside `GameViewModel` and is scoped to `viewModelScope`. It holds the trial list, the current trial index, the error-correction stage, and the per-session image exhaustion trackers. All of this is re-generated if the ViewModel is cleared and recreated (e.g. after a configuration change), which simply restarts the session.

`SessionResult` (correct count, total count, mode) is emitted as a `ChildScreen.End` value at session completion and passed to the `SessionEndViewModel` for display. It is not written to the database.

# Consequences
**Benefits:**
- No additional Room schema for session runtime state — the database schema remains focused on configuration.
- `TrialGenerator` and all related domain services are pure functions or stateless classes — they can be re-invoked at any time without consulting stored state.
- Session startup is always consistent: generated fresh from the current active step configuration.
- Session history (if required in the future) is a completely additive feature — a new `SessionResultRepository` and entity can be introduced without touching the existing architecture.

**Trade-offs:**
- Sessions cannot be resumed after process death. A new session starts fresh from the beginning.
- Test-mode scores are displayed immediately at session end but are not stored. If the therapist needs session history, they must observe it in person or export it through a future feature.

**Limitations:**
- If session history and score tracking become a requirement, a `SessionResultEntity` and corresponding repository must be added. The current architecture supports this addition without structural changes (see §17.2 Scalability in the target architecture document).

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §2 Architectural Principle #8, §7 Learning Engine Architecture, §17.2 Scalability
- `docs/friendly-emotions/target-domain.md` — §10 Learning Session Lifecycle, §12 Trial Lifecycle
