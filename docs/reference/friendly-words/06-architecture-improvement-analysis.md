# Architecture Improvement Analysis — Przyjazne Słowa v2

> **Role:** Principal Android Architect  
> **Date:** 2026-06-25  
> **Scope:** Architecture assessment based exclusively on documentation in `/docs`. No speculative findings beyond documented evidence.  
> **Source documents:** `01-project-inventory.md`, `02-domain-model.md`, `03-learning-session-flow.md`, `04-therapist-configuration-flow.md`, `05-data-architecture.md`

---

## Table of Contents

1. [Findings](#findings)
   - [Critical](#critical-severity)
   - [High](#high-severity)
   - [Medium](#medium-severity)
   - [Low](#low-severity)
2. [Top 10 Improvements](#top-10-improvements-by-architectural-benefit)
3. [Components to Redesign Before Next Generation](#components-to-redesign-before-building-the-next-generation)
4. [Components That Are Well-Designed](#components-that-are-well-designed-and-should-be-preserved)

---

## Findings

---

### CRITICAL Severity

---

#### C-1 — `ConfigurationSettingsViewModel` is a God Object

**Explanation**  
`ConfigurationSettingsViewModel` is annotated `@HiltViewModel` and injected with four dependencies: `ConfigurationRepository`, `ResourceRepository`, `ImageRepository`, and `PreferencesRepository`. It owns and orchestrates the complete in-memory state of a five-tab wizard (`ConfigurationMaterialState`, `ConfigurationLearningState`, `ConfigurationReinforcementState`, `ConfigurationTestState`, `ConfigurationSaveState`), performs duplicate-name validation, constructs and persists the full `Configuration` entity graph (including resource and image-usage links), handles the "edit vs. create" branching, manages wizard navigation side-effects, and derives cross-tab constraints such as `availableForLearning` / `availableForTest`. All five tab-specific events are funnelled through a single sealed hierarchy (`ConfigurationSettingsEvent`) and dispatched inside one `reduce()` call site.

**Potential Impact**  
- Any modification to any single tab's behaviour requires touching the same class, creating a constant merge-conflict surface.  
- Unit-testing a single tab (e.g., learning parameters) requires constructing the full ViewModel with all four repositories mocked.  
- The class will grow unboundedly as new tabs or wizard steps are added.  
- Cognitive load is extremely high; a new developer must understand all five tabs to safely change one.

**Recommended Improvement**  
Decompose responsibilities along the following lines:  
1. Promote each tab ViewModel to a `@HiltViewModel` with its own injected dependencies (see C-2 for the related issue about non-Hilt tab VMs).  
2. Extract a dedicated `ConfigurationWizardCoordinator` (or equivalent use-case/interactor) that holds the cross-tab derived state and the final persist operation.  
3. `ConfigurationSettingsViewModel` should become a thin coordinator that only holds navigation state (selected tab, exit dialog flag) and delegates all tab-specific logic to the respective tab ViewModel.

---

#### C-2 — Five Tab ViewModels Bypassing Hilt, Creating an Implicit Dependency Chain

**Explanation**  
`ConfigurationLearningViewModel`, `ConfigurationMaterialViewModel`, `ConfigurationReinforcementViewModel`, `ConfigurationTestViewModel`, and `ConfigurationSaveViewModel` are all plain `ViewModel` subclasses with no `@HiltViewModel` annotation and no injected dependencies. They cannot request any dependency from the Hilt graph. As a consequence, all business data they need must be passed to them as constructor arguments or pushed in as events from `ConfigurationSettingsViewModel`. The net effect is that `ConfigurationSettingsViewModel` must pull data from four repositories, transform it, and push the results into five subordinate ViewModels — recreating a manual dependency-injection mechanism inside a class that is itself an injected ViewModel.

**Potential Impact**  
- The five tab ViewModels are not independently testable; their inputs come from `ConfigurationSettingsViewModel`, making true isolation impossible.  
- Adding a data dependency to any tab ViewModel requires changing `ConfigurationSettingsViewModel` instead of the tab ViewModel itself, violating the Open/Closed principle.  
- The pattern is invisible from the outside: a developer looking at `ConfigurationLearningViewModel` sees no injected dependencies and falsely concludes it is self-contained.

**Recommended Improvement**  
Annotate all five tab ViewModels with `@HiltViewModel` and inject their own minimum required dependencies directly. This dissolves the manual data-passing chain and allows each ViewModel to be tested in isolation.

---

#### C-3 — Direct DAO Injection into `ChildMainViewModel` Bypasses the Repository Pattern

**Explanation**  
`ChildMainViewModel` is documented as receiving both `ConfigurationDao` and `ConfigurationRepository` as constructor injections. `ConfigurationDao` is a data-layer detail that should never reach the UI layer. The direct DAO injection is used to call `configurationDao.getActiveConfiguration().firstOrNull()` inside `refreshCanPlay()`, even though `ConfigurationRepository` already encapsulates this query and exposes `hasMaterialsForActiveConfig()` — which itself calls `getActiveConfiguration()` internally. Additionally, `MainScreen` composable directly collects `configurationDao.getActiveConfiguration()` as a live stream to display the active configuration name, further short-circuiting the repository boundary.

**Potential Impact**  
- Two parallel code paths query the active configuration (via DAO and via repository). They can diverge if the DAO query logic changes.  
- The DAO is a low-level persistence detail. Injecting it into a ViewModel couples the child-app UI layer directly to the Room implementation, making it impossible to swap persistence mechanisms without changing the ViewModel.  
- Any developer adding child-app features has a precedent to inject DAOs directly, gradually eroding the data layer boundary.

**Recommended Improvement**  
Remove `ConfigurationDao` from `ChildMainViewModel`. Extend `ConfigurationRepository` with a single method that satisfies all `ChildMainViewModel` needs (streaming the active configuration name + mode for display, and checking whether material is available). The repository already has `getActiveConfiguration()` as a Flow — expose it at the repository level and let `ChildMainViewModel` depend only on `ConfigurationRepository`.

---

#### C-4 — Business Logic and Side Effects Embedded in `GameScreen` Composable

**Explanation**  
The `GameScreen` composable directly hosts:  
- The complete `TextToSpeech` lifecycle (`remember`, `LaunchedEffect`, `DisposableEffect`)  
- The `handleAnswer(item)` function that evaluates answer correctness, sets `correctClicked`, `shouldReinforce`, `showCongratsScreen`, `answerJudged`, and `correctAnswersCount` via direct mutable state  
- The hint timer (`LaunchedEffect(currentRoundIndex, ttsReady)` with `delay(roundTimeoutMillis)`)  
- The on-timeout repetition callback (`onTimeout`) passed as a lambda to `CorrectAnswerScreen` that directly mutates the `rounds` list and `repeatStage`  
- Round option position tracking (`currentRound.options.find { it.label == item.label }`)  

Composables in the MVVM + Compose architecture are supposed to be stateless renderers that emit events to a ViewModel. State mutation, business rules, and side effects should live in the ViewModel.

**Potential Impact**  
- `GameScreen` is both a UI component and a business logic controller, making it untestable without Android instrumentation.  
- The hint timer is a coroutine launched directly from a `LaunchedEffect` inside the composable. Any recomposition that restarts the `LaunchedEffect` can cause timing bugs.  
- The `TextToSpeech` instance is owned by the composable. A configuration change (orientation, system font scale) that triggers recomposition will call `tts.shutdown()` and reinitialize, interrupting audio mid-session.  
- The `rounds` list is mutated directly by the composable through a ViewModel-passed lambda, bypassing ViewModel encapsulation.

**Recommended Improvement**  
Move all of the following into `GameViewModel` or dedicated use cases:  
- `TextToSpeech` management (via a scoped `AudioFeedbackController` that the ViewModel owns)  
- `handleAnswer()` logic (expose as a `GameViewModel.onAnswerSelected(item: GameItem)` event handler)  
- Hint timer (a coroutine launched inside `GameViewModel.viewModelScope`)  
- The `onTimeout` repetition logic (a `GameViewModel.onRoundTimeout()` method)  
`GameScreen` should only read state from `GameViewModel` and emit events.

---

#### C-5 — Non-Atomic Active Configuration Change Can Leave System in an Inconsistent State

**Explanation**  
`ConfigurationRepository.setActiveConfiguration(configuration, mode)` executes two sequential SQL statements: `clearActiveConfiguration()` (sets `isActive=0`, `activeMode=NULL` for all rows) followed by `activateConfiguration(id, mode)` (sets `isActive=1`, `activeMode=mode` for the chosen row). These two statements are documented explicitly as **not wrapped in an explicit database transaction**. A process kill, ANR, or uncaught exception between the two calls leaves the database in a state where `isActive=1` belongs to no row — meaning the child app will have no active configuration and the `canPlay` flag will be permanently `false` until the therapist manually reactivates a configuration.

**Potential Impact**  
- The child app becomes completely unusable without therapist intervention.  
- The fallback logic (auto-activate first example configuration on delete) does not fire in this scenario, leaving no safety net.  
- The domain invariant "exactly one Configuration is active at all times" is silently violated.

**Recommended Improvement**  
Wrap both SQL calls in a single Room `@Transaction`-annotated DAO method (or use `withTransaction {}` at the repository level). Room's `@Transaction` annotation ensures the two UPDATE statements are executed atomically.

---

### HIGH Severity

---

#### H-1 — No Use Case / Domain Service Layer

**Explanation**  
The application has no explicit use-case or interactor layer between ViewModels and repositories. Business logic is distributed across at least three layers:  
- **Repositories** contain cross-table business logic (`getResourcesWithImagesForActiveConfigFiltered`, `hasMaterialsForActiveConfig`, `setActiveConfiguration`, `getConfigurationNamesUsingResource`).  
- **ViewModels** contain validation rules (duplicate-name check, hint minimum-one invariant), transformation logic (`toLearningSettings()`, `toTestSettings()`), and wizard orchestration.  
- **Composables** contain round-evaluation logic, TTS triggers, and repetition decisions (see C-4).  
- **Top-level functions** (`generateGameRounds()`) contain the core session algorithm.  

Domain invariants described in `02-domain-model.md` — such as the active-configuration-singleton rule, the test-settings inheritance rule, and the minimum-one-hint constraint — have no single authoritative location; they are enforced in different layers.

**Potential Impact**  
- The same invariant can be accidentally omitted or duplicated when adding a new entry point (e.g., a future API, a batch import, or a second UI path).  
- Testing business rules requires wiring up full ViewModel or repository stacks.  
- Repositories grow beyond their data-access mandate, making them harder to reason about.

**Recommended Improvement**  
Introduce a use-case layer (e.g., `domain/usecases/`) with classes such as `ActivateConfigurationUseCase`, `GenerateGameSessionUseCase`, `SaveConfigurationUseCase`, `ValidateConfigurationNameUseCase`. Each use case encapsulates one business operation, depends only on repository interfaces, and can be unit-tested with pure Kotlin mocks.

---

#### H-2 — UI-Layer State Models Live in the `:shared` Data Module

**Explanation**  
The `another/` package inside `:shared` contains `ConfigurationLearningState`, `ConfigurationMaterialState`, `ConfigurationReinforcementState`, `ConfigurationTestState`, and `RoundSettings`. `ConfigurationLearningState` and siblings are wizard-screen UI state objects. They are used directly by tab ViewModels and screen composables in `:app`. Having these in `:shared` means the data/domain module has knowledge of UI presentation concerns — an upward dependency that violates Clean Architecture's dependency rule (outer layers depend on inner; inner layers must not depend on outer).

**Potential Impact**  
- Any change to the UI representation of configuration state requires editing the `:shared` module, which is also compiled into the child app.  
- The `:shared` module cannot be adopted in a pure domain/data library role without carrying UI state baggage.  
- The `another/` package name itself signals that its contents were placed there without a clear design intent.

**Recommended Improvement**  
Move `ConfigurationLearningState`, `ConfigurationMaterialState`, `ConfigurationReinforcementState`, and `ConfigurationTestState` to the `:app` module's therapist feature package, where they belong. Retain only true shared domain contracts in `:shared` (`RoundSettings` interface is a legitimate candidate to stay as it is a domain abstraction shared by both sub-applications). Rename `another/` to a semantically correct package (`domain/`, `model/`, etc.).

---

#### H-3 — No Gradle-Level Isolation Between the Two Sub-Applications Within `:app`

**Explanation**  
`child_app` and `therapist` are separated only by package convention inside the single `:app` Gradle module. Any Kotlin class in `child_app` can import any class from `therapist` (and vice versa) without any compile-time boundary. The build system provides no enforcement. The two sub-applications share the same `Application` class (`FriendlyWordsApp`), which is placed in the therapist package (`therapist.ui.main`) despite being the shared application entry point for the entire APK.

**Potential Impact**  
- Accidental cross-package dependencies will not be detected until a code review catches them.  
- Both sub-applications cannot be built or tested independently.  
- The `FriendlyWordsApp` is visually "owned" by the therapist package, which misleads contributors about its shared role.  
- Future extraction of either sub-application into a separate deliverable (e.g., a standalone child app) requires untangling implicit package-level dependencies.

**Recommended Improvement**  
Extract `child_app` into a `:child` Gradle module and `therapist` into a `:therapist` Gradle module, both depending on `:shared` (and a future `:domain` module). Move `FriendlyWordsApp` to a dedicated `:app` shell module that only wires the DI graph and declares manifest entries. This enforces dependency boundaries at the build level.

---

#### H-4 — String-Based Screen State Machine Is Untyped and Fragile

**Explanation**  
`ChildMainState.screenState` is typed as `String`. Valid values are `"info"`, `"main"`, `"game"`, and `"end"`. These string literals are compared directly in `ScreenNavigationGame` via `when (state.screenState)` branches. Events are also string-keyed in the implicit transition logic. There is no compile-time guarantee that only valid states are ever assigned. Typos produce silent bugs — the app navigates to no screen without crashing.

**Potential Impact**  
- A typo like `"mian"` instead of `"main"` compiles without error and silently breaks navigation.  
- All valid states, allowed transitions, and terminal states are invisible to the type system and tooling.  
- The state machine cannot be documented, visualized, or verified by the compiler.

**Recommended Improvement**  
Replace `String` with a sealed class or enum:

```kotlin
sealed class ChildScreen {
    object Info : ChildScreen()
    object Main : ChildScreen()
    object Game : ChildScreen()
    object End  : ChildScreen()
}
```

`when` exhaustiveness is then enforced by the compiler, and all transitions become explicit and type-safe.

---

#### H-5 — Magic String Constants for Domain Modes and Command Types Stored in the Database

**Explanation**  
Multiple domain concepts are persisted as raw strings with no type enforcement:  
- `Configuration.activeMode`: `"uczenie"` or `"test"` — compared with `==` in `ChildMainViewModel`, `GameViewModel`, and `ConfigurationRepository`.  
- `LearningSettings.commandType` / `TestSettings.commandType`: `"SHORT"`, `"WHERE_IS"`, `"SHOW_ME"` — stored in the database and then re-mapped to display strings in a `when` block inside `GameViewModel.commandType`.  
- `ConfigurationLearningState.selectedPrompt`: stores the display string (`"Gdzie jest {Słowo}"`) and converts back to the code string at save time in `toLearningSettings()`.  
- Hint type names stored as `List<String>` with `||` delimiter.  
- The `HintType` enum exists but hint type values are not used from it when persisting — the Polish display strings are stored directly.

**Potential Impact**  
- Any renaming of a Polish display label (e.g., localisation refactor) breaks DB reads for all existing records unless the string value in the DB is migrated.  
- The round-trip `"SHORT"` → `"{Słowo}"` → `"SHORT"` conversion is a hidden semantic dependency between the save path and the read path.  
- Case-sensitivity bugs are silent: `"Uczenie"` ≠ `"uczenie"`.

**Recommended Improvement**  
Define sealed classes or enums for all domain discriminators (`ActiveMode`, `CommandType`). Store enum `name` (or a stable ordinal) in the database. Provide Room `TypeConverter`s for each enum. Eliminate the display-string round-trip by keeping display concerns in the UI layer only.

---

#### H-6 — N+1 Database Query Pattern Inside `toConfigurationImageUsages()`

**Explanation**  
`ConfigurationMaterialState.toConfigurationImageUsages(configurationId, imageRepository)` iterates over `materialState.vocabItems` and calls `imageRepository.getByResourceId(item.id)` inside the loop — one database query per vocabulary item. For a configuration with N words, this generates N+1 database round trips (1 for the configuration + N for the images). This call happens synchronously inside the save flow and blocks the coroutine for each word.

**Potential Impact**  
- Save latency scales linearly with the number of configured words. A configuration with 20 words makes 20 sequential DB queries.  
- The pattern is hidden inside a state transformation function (`toConfigurationImageUsages`), making it invisible during code review.  
- It sets a precedent for embedding repository calls in state mapping functions, which are typically expected to be pure.

**Recommended Improvement**  
Load all images for all vocabulary items in a single batch query before the mapping phase, then pass the pre-loaded image map into the mapping function. Alternatively, the repository can provide a `getImagesByResourceIds(ids: List<Long>): Map<Long, List<Image>>` method that executes one query with an `IN` clause.

---

#### H-7 — `ConfigurationDao` Manages Three Tables, Violating Single Responsibility

**Explanation**  
`ConfigurationDao` is documented as managing three distinct tables: `configurations`, `configuration_resources`, and `configuration_image_usages`. It exposes CRUD methods for all three, including six methods for `configuration_resources` and seven for `configuration_image_usages`. As the data model grows (e.g., adding session history, scoring, or per-resource metadata), this DAO will continue to accumulate unrelated queries.

**Potential Impact**  
- All tests involving any configuration query must instantiate and stub the same large interface.  
- Developers cannot determine at a glance what a "configuration" is at the data layer versus what is a join-table concern.  
- The DAO is already large enough to merit splitting; its size will only increase.

**Recommended Improvement**  
Split into three DAOs: `ConfigurationDao` (configurations table only), `ConfigurationResourceDao` (already partially exists as a separate DAO for the reverse lookup — expand it), and `ConfigurationImageUsageDao`. Each DAO has a single table responsibility.

---

#### H-8 — Example Data Seeding Logic Placed in `MainScreenViewModel.init {}`

**Explanation**  
`MainScreenViewModel.init {}` calls `ensureExampleMaterials()` and `ensureExampleConfigurations()`, which insert 50 resources with 150 image records and 2 configurations on first launch. This is a database initialization concern — it runs conditionally based on whether the database is empty and inserts a large, structured, domain-representative dataset. Placing this logic in a ViewModel violates the single-responsibility principle: ViewModels should manage UI state, not orchestrate database seeding.

**Potential Impact**  
- First-launch seeding runs inside a ViewModel scope, meaning it is tied to the lifecycle of `MainScreen`. If the user navigates away before seeding completes, the operation may be interrupted.  
- The seeding logic depends on `ResourceRepository`, `ImageRepository`, and `ConfigurationRepository` — four repository instances just for initialization.  
- Testing the ViewModel requires mocking the seeding path, or the seeding path requires testing the ViewModel.

**Recommended Improvement**  
Move seeding to a dedicated `DatabaseInitializer` class or a `SeedDatabaseUseCase` called from the `Application.onCreate()` or a Hilt `@Initializer`. This class has a single entry point, runs once before any Activity starts, and can be tested independently of any ViewModel.

---

#### H-9 — `fallbackToDestructiveMigration = true` at Database Version 27

**Explanation**  
The Room database is at version 27 with `fallbackToDestructiveMigration = true` and `exportSchema = false`. There are no migration scripts in the documentation. Every schema change wipes all user data (resources, configurations, image usage links) silently. This was acceptable in early development but represents an unacceptable risk in a deployed application where therapists have invested significant time building materials and configurations.

**Potential Impact**  
- An APK update that changes any entity field name, type, or adds a new table will destroy all therapy session configurations on all devices without warning.  
- There is no recovery path — deleted data cannot be restored.  
- With `exportSchema = false`, the schema history is not tracked, making incremental migration scripts impossible to write retroactively.

**Recommended Improvement**  
1. Enable `exportSchema = true` immediately to begin tracking schema history.  
2. Write a migration script for the current schema baseline (version 27 → 28 onwards).  
3. Replace `fallbackToDestructiveMigration(true)` with explicit `addMigrations(...)` for all future schema changes.  
4. Consider adding Room's `AutoMigration` support for simple column additions going forward.

---

#### H-10 — `ConfigurationRepository` Contains Cross-Cutting Data Assembly Logic That Belongs in a Use Case

**Explanation**  
`ConfigurationRepository` implements `getResourcesWithImagesForActiveConfigFiltered(isTestMode)` — a multi-step algorithm that: (1) loads the active configuration, (2) loads all configuration-resource links, (3) loads all configuration-image-usage links, (4) loads all resources with images, (5) filters resources to those in the configuration, (6) filters images within each resource to those eligible for the mode, and (7) removes resources with empty post-filter image sets. This is a domain-level session-preparation algorithm that coordinates four separate data sources. It belongs in a use case or domain service, not in a repository. Repositories should provide data-access operations scoped to their entity, not orchestrate multi-entity business workflows.

**Potential Impact**  
- `ConfigurationRepository` has grown into a domain orchestrator rather than a data accessor, conflating two distinct responsibilities.  
- The method is called from three different places: `ChildMainViewModel.refreshCanPlay()`, `GameViewModel.init{}`, and `generateGameRounds()`.  
- Testing this algorithm requires instantiating `ConfigurationRepository` with all its DAO dependencies.

**Recommended Improvement**  
Extract a `PrepareGameSessionUseCase` or `SessionMaterialLoader` that takes the necessary repositories as dependencies and implements this multi-step algorithm. `ConfigurationRepository` should be reduced to raw CRUD and single-entity queries.

---

### MEDIUM Severity

---

#### M-1 — `GameViewModel` Re-assembles `LearningSettings` via `toLearningSettings()` at Runtime

**Explanation**  
`GameViewModel.init {}` calls `config.toLearningSettings(materialState, reinforcementState)` to reconstruct `activeLearningSettings`. This means the ViewModel loads the embedded `LearningSettings` from the `Configuration` entity, then loads `materialState` (via `getMaterialState()`) and `reinforcementState` (via `getReinforcementState()`), and passes all three into a mapping function to produce the settings object actually used at runtime. `getReinforcementState()` is documented as returning hardcoded defaults at this stage with no DB query, meaning reinforcement state from the persisted configuration is effectively bypassed and replaced with in-memory defaults.

**Potential Impact**  
- If a therapist persists a configuration with specific reinforcement settings, those settings may not be correctly loaded at game time.  
- The three-way join of `config`, `materialState`, and `reinforcementState` inside `toLearningSettings()` makes the actual data consumed by the game engine opaque.  
- `getReinforcementState()` creating a hardcoded default is a hidden bug: the method name implies a DB read but the implementation contradicts it.

**Recommended Improvement**  
`LearningSettings` already embeds `typesOfPraises` and `animationsEnabled` — reinforcement settings are already stored in the `Configuration` entity. Remove the `getReinforcementState()` indirection and read reinforcement parameters directly from `config.learningSettings`. Eliminate `toLearningSettings()` in favour of direct field access or a simple adapter function that does not require an additional repository call.

---

#### M-2 — `TestSettings.answerTimeSeconds` Is Unused; Timer Reads from `LearningSettings`

**Explanation**  
`TestSettings.answerTimeSeconds` is a documented field in the `TestSettings` entity and the `configurations` table. However, the game engine's test-mode timeout (`GameScreen`'s `LaunchedEffect`) uses `roundTimeoutMillis = hintAfterSeconds × 1000`, which maps to `LearningSettings.hintAfterSeconds` — the learning-mode hint timer. The therapist UI confirms this: the Test tab shows no separate `answerTimeSeconds` control, and a UI note states that the test timeout equals the learning hint delay. `TestSettings.answerTimeSeconds` is persisted but never read during game execution.

**Potential Impact**  
- Dead column in the database carrying a misleading name. `test_answerTimeSeconds` stores `0` and is never consumed.  
- If a future developer adds a UI control for `answerTimeSeconds` (the obvious next step), they must also update the game engine to read it — a non-obvious cross-file dependency.  
- The implicit coupling between learning `hintAfterSeconds` and test timeout is invisible unless both the UI note and the game engine implementation are read together.

**Recommended Improvement**  
Either: (a) remove `answerTimeSeconds` from `TestSettings` and document that test timeout is shared with `hintAfterSeconds`; or (b) implement `answerTimeSeconds` properly — add a UI control and update the game engine to read it — which is the architecturally cleaner path. Do not leave a persisted field that is never read.

---

#### M-3 — Two Material Design Versions Coexist in `:app`

**Explanation**  
`:app` depends on both `androidx.compose.material:material` (Material 2, version 1.5.4) and Material 3 (via Compose BOM 2024.04.01). Material 2 and Material 3 have different component APIs, theming systems, and design tokens. Using both simultaneously creates ambiguity: a developer adding a new component must decide which design system to use, and no tooling enforces consistency.

**Potential Impact**  
- Visual inconsistency between screens that use M2 components and those that use M3 components.  
- Theme tokens do not translate automatically between the two systems; custom M2 theming does not propagate to M3 components and vice versa.  
- `material-icons-extended` at version 1.6.0 predates BOM 2024.04.01, creating a potential version mismatch.  
- Migrating to M3 only requires identifying and replacing all M2 usages — harder to do when usage is entrenched across the codebase.

**Recommended Improvement**  
Commit fully to Material 3. Identify all M2 component usages, replace them with M3 equivalents, and remove the M2 dependency. Use the Compose BOM for all Material library versions to guarantee alignment.

---

#### M-4 — Physical Image Files Are Not Deleted When Images Are Removed

**Explanation**  
`ImageRepository.deleteUnassignedImages()` removes `Image` rows from the database when their reference count drops to zero, but it does **not** delete the corresponding `.jpg` files from `context.filesDir`. Two deletion flows trigger this gap: (1) when the therapist removes an image from a resource, and (2) when a resource is deleted entirely. The `path` stored in the `Image` record points to a file that remains on disk indefinitely.

**Potential Impact**  
- Over time, `filesDir` accumulates unreferenced image files, consuming device storage with no cleanup path.  
- A user cannot reclaim this storage without uninstalling the app.  
- The number of orphaned files grows proportionally with therapist activity (editing materials, discarding images during creation).

**Recommended Improvement**  
When deleting an `Image` record whose `path` points to internal storage (i.e., not a `file:///android_asset/` URI), delete the corresponding file. This should be a single `File(path).delete()` call in `ImageRepository.deleteUnassignedImages()` after the DB record is removed.

---

#### M-5 — Business Constraint Derivation (`availableForLearning`, `availableForTest`) Performed in Composable

**Explanation**  
`ConfigurationSettingsScreen` computes `availableImagesForLearning` and `availableImagesForTest` inline within the composable function body by iterating over `materialState.vocabItems`. These values are then passed as parameters to child composables (`ConfigurationLearningScreen`, `ConfigurationTestScreen`) to constrain UI controls (maximum image count). This is a business rule — how many words have at least one eligible image per mode — and belongs in the ViewModel.

**Potential Impact**  
- The constraint cannot be tested without Compose testing infrastructure.  
- If the constraint logic changes (e.g., a minimum-image-count rule is added), the change must be made in the composable rather than in a testable unit.  
- Composables are supposed to be pure renderers; embedding derived business data in them blurs the boundary between view and logic.

**Recommended Improvement**  
Move `availableForLearning` and `availableForTest` to `ConfigurationSettingsViewModel` as derived `StateFlow` values. The composable reads them from state, eliminating inline computation from the rendering function.

---

#### M-6 — Navigation Side Effects Encoded via `navigateToList` Flag in `ConfigurationSettingsState`

**Explanation**  
Post-save navigation back to the configuration list is triggered by setting `navigateToList = true` in `ConfigurationSettingsState`. The composable observes this boolean in a `LaunchedEffect` and calls `onBackClick()`. Communicating navigation intent through a boolean field in UI state is an anti-pattern in Compose MVVM. It requires the composable to observe state, detect the transition, execute the navigation, and then the state must be reset to `false` to prevent re-navigation on recomposition.

**Potential Impact**  
- If the composable recomposes after the navigation event has fired but before the boolean is reset, duplicate navigation can occur.  
- The `navigateToList` field leaks navigation concerns into the data class that models form state.  
- Adding future navigation destinations requires adding more boolean flags, each with the same fragile observe-navigate-reset cycle.

**Recommended Improvement**  
Use a `Channel<NavigationEvent>` or `SharedFlow<NavigationEvent>` (the standard one-shot event pattern for Compose MVVM) to emit navigation commands from the ViewModel. The composable collects these in a `LaunchedEffect` and calls the appropriate NavController operation exactly once.

---

#### M-7 — `commandType` Stored as Display String, Converted at Save, Re-converted at Read

**Explanation**  
`ConfigurationLearningState.selectedPrompt` stores the Polish display string (e.g., `"Gdzie jest {Słowo}"`). At save time, `toLearningSettings()` converts it to a code constant (`"WHERE_IS"`). At read time in `GameViewModel.commandType`, the code constant is mapped back to a display string template (`"Gdzie jest {Słowo}"`). The `{Słowo}` placeholder is then replaced with the actual word in `GameScreen`. This creates a three-step round trip through two different representations of the same value, with the mapping logic split across `toLearningSettings()`, `GameViewModel`, and `GameScreen`.

**Potential Impact**  
- Adding a new command type requires changes in three separate locations.  
- The intermediate display string in `selectedPrompt` must exactly match the `when` branches in `toLearningSettings()` — a fragile implicit contract.  
- The `{Słowo}` template itself is a mini-template engine that lives outside any formal abstraction.

**Recommended Improvement**  
Represent command type as a `CommandType` sealed class or enum throughout the UI state layer. Store the enum `name` in the database. Build the display string at the single point of display (in the composable or as a pure function), eliminating the intermediate Polish-string representation from `ConfigurationLearningState`.

---

#### M-8 — Orphaned `child_app/` Directory at Repository Root

**Explanation**  
A directory named `child_app/` exists at the repository root and is not included in `settings.gradle.kts`. It contains only drawable resources. Its `build.gradle.kts` references namespace `com.example.child_app`. It is an unused artifact that has no active source code.

**Potential Impact**  
- Confuses new developers who may spend time trying to understand why a module directory exists but is not listed in settings.  
- If any tooling scans the repository for Gradle modules, it may attempt to process this directory.  
- The drawable resources inside may be duplicates of resources in `:app`, creating an undetected inconsistency.

**Recommended Improvement**  
Delete the `child_app/` root directory entirely after verifying that all drawable resources it contains are either unused or already present in the `:app` module.

---

### LOW Severity

---

#### L-1 — `exportSchema = false` Prevents Schema History Tracking

**Explanation**  
`AppDatabase` is annotated with `exportSchema = false`. Room's schema export generates a JSON snapshot of the database schema at each version change, committed to source control. Without it, there is no historical record of what the schema looked like at any previous version, making it impossible to write correct migration scripts retroactively.

**Potential Impact**  
- Combined with `fallbackToDestructiveMigration = true` (see H-9), this doubles the migration risk: not only is there no migration code, there is also no schema history to write migration code from.  
- Any attempt to introduce proper migrations later requires reverse-engineering the schema history from git blame.

**Recommended Improvement**  
Set `exportSchema = true` and commit the generated schema JSON files to source control under `schemas/`. Configure the annotation processor output path in `build.gradle.kts`.

---

#### L-2 — Camera Capture Uses Low-Resolution Thumbnail API

**Explanation**  
Camera capture uses `ActivityResultContracts.TakePicturePreview()`, which returns a low-resolution thumbnail `Bitmap`. Full-resolution camera capture requires `ActivityResultContracts.TakePicture()` with a `FileProvider`-backed `Uri`. No `FileProvider` is declared in `AndroidManifest.xml`.

**Potential Impact**  
- Educational materials captured via camera will be low-resolution and may appear blurry at the display sizes used during the child's game session.  
- This is a functional regression risk for therapists who rely on camera capture for custom materials.

**Recommended Improvement**  
Declare a `FileProvider` in `AndroidManifest.xml`, switch to `ActivityResultContracts.TakePicture()`, and save the full-resolution image directly to `filesDir` via the `FileProvider` URI. This is a well-documented Android pattern.

---

#### L-3 — Unused Dependencies in `:shared` Module

**Explanation**  
The `:shared` module lists `androidx.navigation:navigation-compose`, `io.coil-kt:coil-compose`, and `androidx.hilt:hilt-navigation-compose` as dependencies with no active usage within the module's source code. These are UI-specific libraries that have no business being in a data/domain module.

**Potential Impact**  
- These dependencies increase compile-time and APK size for no benefit.  
- Their presence suggests the module boundaries were not enforced during development.  
- Navigation Compose and hilt-navigation-compose in `:shared` imply someone considered putting navigation logic in the shared module, which would be an incorrect dependency direction.

**Recommended Improvement**  
Remove all three unused dependencies from `:shared`'s `build.gradle.kts`.

---

#### L-4 — `FriendlyWordsApp` Application Class Placed Inside Therapist Package

**Explanation**  
The `@HiltAndroidApp` Application class `FriendlyWordsApp` is located at `com.example.friendly_words.therapist.ui.main.FriendlyWordsApp`. This is the Application class for the entire APK — both sub-applications. Its placement inside the therapist feature package implies ownership it does not have.

**Potential Impact**  
- Structural confusion: developers reading the therapist feature package will find the Application class mixed in with `MainActivity` and `MainScreen`.  
- If the two sub-applications are ever separated into distinct modules, the Application class is in the wrong module.

**Recommended Improvement**  
Move `FriendlyWordsApp` to a root-level package: `com.example.friendly_words.FriendlyWordsApp` or a dedicated `:app` shell module.

---

#### L-5 — `PreferencesRepository` Scoped to `ViewModelComponent` Instead of Singleton

**Explanation**  
`PreferencesModule` provides `PreferencesRepository` scoped to `ViewModelComponent`, meaning a new `PreferencesRepository` instance is created for each injected ViewModel. However, the underlying DataStore delegate is process-singleton. Multiple `PreferencesRepository` instances all reference the same DataStore, so functionally there is no issue — but the `ViewModelComponent` scoping suggests the intent was to make it VM-local, which is neither necessary nor accurate.

**Potential Impact**  
- Minor memory overhead (multiple repository instances wrapping one DataStore).  
- Misleading: `ViewModelComponent` scoping implies the repository should not be shared, but DataStore itself is process-singleton and inherently shared.

**Recommended Improvement**  
Scope `PreferencesRepository` to `SingletonComponent` alongside the other repositories. It has no ViewModel-specific state that justifies a shorter scope.

---

#### L-6 — Hardcoded Splash Delay in Composable and Activity

**Explanation**  
The 5-second splash/information screen delay is implemented as `delay(5000)` inside composables (`ScreenNavigation` in the therapist app) and `LaunchedEffect(Unit): delay(5000ms)` in `ChildMainScreen`. The delay value is a magic number with no named constant and no configurability.

**Potential Impact**  
- Changing the splash duration requires finding and updating multiple call sites.  
- The delay cannot be shortened during UI testing without bypassing or mocking coroutine delays.

**Recommended Improvement**  
Extract the delay duration to a named constant (`SPLASH_DURATION_MS = 5_000L`) in a shared constants file. In tests, inject a controllable delay (e.g., via a `CoroutineDispatcher` or a `DelayProvider` abstraction) to allow the splash to be bypassed.

---

## Top 10 Improvements by Architectural Benefit

Ranked by the breadth and depth of their positive impact on maintainability, testability, and long-term evolution:

| Rank | Improvement | Finding(s) | Primary Benefit |
|------|-------------|-----------|-----------------|
| 1 | **Decompose `ConfigurationSettingsViewModel`** into a thin coordinator + independent Hilt tab ViewModels | C-1, C-2 | Eliminates the God Object, enables per-tab unit testing, reduces merge conflicts |
| 2 | **Introduce a use-case layer** to centralise business logic currently spread across repositories, ViewModels, and composables | H-1, H-10 | Single authoritative location for each business rule; enables pure-Kotlin testing |
| 3 | **Fix `fallbackToDestructiveMigration`** — enable `exportSchema = true` and implement proper Room migrations | H-9, L-1 | Prevents silent user-data loss on any APK update |
| 4 | **Move business logic out of `GameScreen`** into `GameViewModel` (TTS, answer handling, timers, repetition) | C-4 | Makes the game engine testable, eliminates recomposition-driven timing bugs |
| 5 | **Wrap active configuration switch in a database transaction** | C-5 | Guarantees the "exactly one active configuration" domain invariant is never violated |
| 6 | **Replace string-based screen state and magic string constants with sealed classes/enums** | H-4, H-5 | Type-safe navigation, compile-time exhaustiveness, elimination of fragile string comparisons |
| 7 | **Extract `:child` and `:therapist` Gradle modules** from the monolithic `:app` | H-3 | Enforces sub-application boundaries at build time, enables independent testing and future independent delivery |
| 8 | **Remove DAO injection from `ChildMainViewModel`; extend repository API** | C-3 | Restores the repository abstraction boundary in the child app |
| 9 | **Relocate UI state models from `:shared` to `:app`** | H-2 | Corrects the dependency direction; `:shared` becomes a pure data/domain module |
| 10 | **Implement physical file cleanup on image deletion** | M-4 | Prevents unbounded storage growth with no user-visible cleanup path |

---

## Components to Redesign Before Building the Next Generation

The following components contain architectural debt that will compound with every new feature. They should be redesigned — not just incrementally improved — before embarking on the next application generation:

### 1. `ConfigurationSettingsViewModel` + Five Tab ViewModels
The current design — one God-ViewModel orchestrating five plain ViewModels through a manual data-passing chain — is the single largest architectural liability. Any significant extension of the configuration wizard (new tabs, conditional flows, validation dependencies) will make this class substantially more complex. Redesigning it as a coordinator + independent tab ViewModels with a use-case layer is a prerequisite for sustainable feature development.

### 2. `GameScreen` Composable
As documented, `GameScreen` combines UI rendering, audio management, game state mutations, timer management, and the error-correction repetition algorithm. In Compose MVVM, composables are renderers. The game engine logic belongs in `GameViewModel` and domain use cases. Redesigning this split is necessary before adding any new game mechanics (new hint types, new reinforcement modes, progressive difficulty, session recording).

### 3. Database Schema + Migration Strategy
At version 27 with no migration history, the schema is in a position where proper migration is already difficult. The redesign work is: (a) enable schema export, (b) audit all 27 version steps from source history to produce a canonical V27 baseline migration, (c) replace destructive migration with explicit migrations going forward. This must be done before any production field deployment.

### 4. The `:app` / `:shared` Module Boundary
The current boundary (`app` = all UI, `shared` = all data+domain) conflates two sub-applications in one compilation unit and places UI state models in the data layer. For the next generation, the module structure should be redesigned as: `:domain` (entities, use cases, repository interfaces), `:data` (Room implementation, DataStore), `:child` (child UI), `:therapist` (therapist UI), `:app` (DI wiring, manifest).

### 5. `ConfigurationRepository`
Currently functioning as both a data accessor and a domain orchestrator, this repository needs to be split: raw CRUD stays in the repository; the multi-entity assembly algorithms (`getResourcesWithImagesForActiveConfigFiltered`, `setActiveConfiguration` atomicity, session material loading) move to use cases.

---

## Components That Are Well-Designed and Should Be Preserved

The following components demonstrate clear design intent, appropriate responsibility boundaries, and reusability. They should be preserved and used as reference patterns:

### 1. `RoundSettings` Interface
A clean, minimal abstraction (`numberOfWords`, `displayedImagesCount`, `repetitionPerWord`) that decouples the round-generation algorithm from the concrete settings type. The `asRoundSettings()` extension functions on `LearningSettings` and `TestSettings` are a textbook use of the Adapter pattern. This interface should be retained as-is in any future domain layer.

### 2. `generateGameRounds()` Algorithm
The round-generation algorithm in `GameRoundManager.kt` — despite being a top-level function — is a well-specified, self-contained, side-effect-free algorithm. Its inputs are a list of domain objects and a `RoundSettings`; its output is `List<GameRound>`. It correctly implements word-pool capping, minimum-repetition enforcement, distractor selection, and dual-shuffle. Moving it into a use class would improve its architectural placement without changing any of its logic.

### 3. Correct-Answer Position Anti-Repeat Mechanism
`noteCorrectPos()` and `shuffledRoundAvoidingPrevious()` implement a compact, well-bounded algorithm that prevents position habituation across repeated word appearances. The 50-candidate-shuffle approach with position history is a clearly reasoned design decision. This should be preserved as-is and moved into a `GameRoundShuffler` domain utility class.

### 4. UDF Event → State Pattern (Per-Screen)
Each screen implements a consistent `*Event.kt` (sealed class) → `*ViewModel.kt` (reducer) → `*State.kt` (immutable data class) → `*Screen.kt` (renderer) pattern. This is a well-applied UDF architecture that gives each screen a clear, testable contract. The pattern itself is sound and should be retained and extended consistently to all screens.

### 5. `ConfigurationImageUsage` Data Model
The per-image, per-mode eligibility model (`inLearning: Boolean`, `inTest: Boolean`) is a well-normalized, extensible design. It correctly separates "is this image in the configuration?" from "which modes can use it?". This model can accommodate future modes (e.g., a review mode or a homework mode) without schema changes, only additions.

### 6. Room Foreign Key Cascade Configuration
The cascade delete setup across `resource_images`, `configuration_resources`, and `configuration_image_usages` is correctly designed. Deleting a `Configuration` or `Resource` automatically cleans up all junction table rows. Combined with proper transactions (see C-5), this is a robust referential integrity model.

### 7. `Test-Settings Inheritance` Pattern (`testEditEnabled`)
The flag-based test-settings inheritance (`testEditEnabled = false` → test mirrors learning; `true` → independent) is a cleanly implemented UX pattern. `toDerivedTestState()` and the round-trip detection on load (comparing persisted test settings against the derived state to infer `testEditEnabled`) are correct and reusable. This pattern should be preserved in any refactoring of the wizard.

### 8. Hilt Singleton Repository Scoping
All three core repositories (`ConfigurationRepository`, `ResourceRepository`, `ImageRepository`) are provided as `@Singleton`s from `:shared`'s `AppModule`. This correctly ensures a single source of truth for all domain data across both sub-applications.

---

*End of Architecture Improvement Analysis*
