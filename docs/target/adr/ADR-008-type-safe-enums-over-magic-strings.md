# ADR-008: Type-Safe Enums over Magic Strings

# Status
Accepted

# Context
The Friendly Words analysis (H-5 finding) identified that domain discriminators — values that determine which branch of business logic to execute — were stored and compared as raw strings throughout the codebase. String comparisons are invisible to the compiler, survive typos silently, and break with renames unless every occurrence is manually updated. This was a source of subtle bugs and made the valid states of the system unclear to new developers.

Friendly Emotions has a richer set of domain discriminators: emotion IDs, grammatical genders, folder gender policies, session modes, hint types, prompt templates, and trial verdicts. Using strings for any of these would replicate the same fragility.

# Decision
All domain discriminators are expressed as Kotlin `enum class` or `sealed class` types:

| Type | Values |
|---|---|
| `EmotionId` | `HAPPY`, `SAD`, `SURPRISED`, `ANGRY`, `SCARED`, `BORED` |
| `GrammaticalGender` | `MASCULINE`, `FEMININE`, `NEUTER` |
| `FolderGenderPolicy` | `MASCULINE`, `FEMININE`, `NEUTER`, `MIXED` |
| `SessionMode` | `LEARNING`, `TEST` |
| `HintType` | `OUTLINE_CORRECT`, `ANIMATE_CORRECT`, `SCALE_CORRECT`, `DIM_INCORRECT` |
| `PromptTemplate` | `EMOTION_ONLY`, `WHERE_IS`, `SHOW_ME`, `FIND`, `TOUCH`, `POINT_TO`, `CHOOSE` |
| `TrialVerdict` | `CLEAN_CORRECT`, `CORRECT_AFTER_HINT`, `TIMEOUT` |
| `TrialState` | sealed: `AwaitingResponse`, `HintVisible`, `Judged(verdict)` |
| `ChildScreen` | sealed: `Info`, `Main`, `Game`, `End(result)` |

When these values must be persisted to Room, they are stored as their `.name` string via Room `TypeConverter` classes defined in `:data`. This keeps the storage representation stable under refactoring (renaming the enum constant requires a deliberate migration) and keeps the domain type clean.

`when` expressions on sealed classes and enums are exhaustive by default in Kotlin — the compiler forces all branches to be handled. This eliminates the silent default-case bug where a new enum value is added but forgotten in an existing `when`.

# Consequences
**Benefits:**
- Adding a new value (e.g. a new `HintType`) produces compile errors at every `when` expression that does not handle it, forcing developers to consciously decide how to handle it everywhere.
- Renaming an enum constant is a safe refactor — the IDE finds all usages and the compiler validates the result.
- The valid states of the system are documented by the type system, not by string constants buried in comments or separate files.
- No stringly-typed comparisons anywhere in business logic.

**Trade-offs:**
- Renaming an enum constant that is already persisted in the database requires a Room migration to rename the stored string values. Developers must be aware that a Kotlin rename is not automatically a safe refactor for persisted data.
- TypeConverter classes must be registered with Room and tested explicitly.

**Limitations:**
- This decision applies to domain discriminators only. Free-form user-provided strings (step names, folder names, praise words) remain as `String` — they are not candidates for enumeration.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §2 Architectural Principle #4, §9.2 Entity Design, §21 Top Architectural Principles for Implementation (#6)
- `docs/friendly-words/06-architecture-improvement-analysis.md` — H-5 finding (magic string discriminators)
