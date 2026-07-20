# ADR-006: Hilt for Dependency Injection

# Status
Accepted

# Context
Friendly Emotions has a multi-module structure with multiple layers, feature-specific ViewModels, shared singletons (database, repositories), and a complex wizard that requires tab ViewModels to share state with a parent coordinator ViewModel. Manual dependency injection at this scale is error-prone and difficult to scope correctly (singleton vs. ViewModel-scoped vs. Activity-scoped).

The domain layer must remain DI-framework-free — it is pure Kotlin and must not carry Hilt annotations. Only Android-adjacent layers should be coupled to the DI framework.

# Decision
Hilt is the dependency injection framework for the project. It is used in `:app`, `:data`, `:feature:child`, and `:feature:therapist`. It is explicitly excluded from `:domain` and `:core:ui`.

Component scoping:

| Hilt Component | Bindings |
|---|---|
| `SingletonComponent` | `AppDatabase`, all DAOs, all repository implementations, `PreferencesRepository` |
| `ViewModelComponent` | All use cases (auto-bound via `@Inject constructor`) |
| `ViewModelScoped` | `TtsController` (scoped to the `GameViewModel` lifecycle) |

DI module organization:
- `:data/di/DatabaseModule.kt` — provides `AppDatabase` and all DAOs.
- `:data/di/RepositoryModule.kt` — binds each repository interface to its implementation via `@Binds`.
- `:data/di/StorageModule.kt` — provides `Context` for file I/O.
- `:feature:child/di/ChildModule.kt` — provides `TtsController`.

The wizard's tab ViewModels share the `WizardContainerViewModel` instance via `hiltNavGraphViewModel()`, scoped to the wizard back-stack entry. This gives all tab ViewModels access to the shared `WizardStepDraft` without a God-ViewModel that owns all tab logic.

`FriendlyEmotionsApp` is annotated `@HiltAndroidApp` and lives in `:app`, which is the Hilt root.

# Consequences
**Benefits:**
- Scoping is explicit and enforced by the Hilt component hierarchy — a singleton cannot accidentally be created twice.
- `@Inject constructor` on use cases means they require no manual DI module registration.
- `hiltNavGraphViewModel()` provides a clean solution for sharing state between wizard tab ViewModels without coupling them directly.
- Compile-time validation of the dependency graph catches missing bindings before runtime.

**Trade-offs:**
- Hilt requires Kotlin annotation processing (KSP), which adds to build time.
- The framework introduces a learning curve for developers unfamiliar with Hilt's component hierarchy and scoping rules.
- `:domain` classes use `@Inject constructor` only — they must never carry `@Singleton`, `@InstallIn`, or any other Hilt annotation.

**Limitations:**
- Because Hilt's `@HiltAndroidApp` root lives in `:app`, integration tests for individual feature modules require a test application component setup.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §14 Dependency Injection
- `docs/friendly-words/06-architecture-improvement-analysis.md` — L-5 finding (DataStore scope inconsistency that Hilt singleton scoping resolves)
