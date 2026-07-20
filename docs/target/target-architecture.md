# Target Architecture — Friendly Emotions

> **Role:** Principal Android Architect  
> **Date:** 2026-07-16  
> **Scope:** Software architecture only. No UI design, no Compose screen design, no code generation.  
> **Sources:** `friendly-emotions-functional-specification.md`, `target-domain.md`, `01-project-inventory.md` through `06-architecture-improvement-analysis.md`

---

## Table of Contents

1. [Context and Design Philosophy](#1-context-and-design-philosophy)
2. [Architectural Principles](#2-architectural-principles)
3. [Module Structure](#3-module-structure)
4. [Layer Architecture](#4-layer-architecture)
5. [Feature Boundaries](#5-feature-boundaries)
6. [Domain Layer Design](#6-domain-layer-design)
7. [Learning Engine Architecture](#7-learning-engine-architecture)
8. [Therapist Configuration Architecture](#8-therapist-configuration-architecture)
9. [Data Architecture](#9-data-architecture)
10. [Repository Architecture](#10-repository-architecture)
11. [Use Case Organization](#11-use-case-organization)
12. [ViewModel Responsibilities](#12-viewmodel-responsibilities)
13. [State Management](#13-state-management)
14. [Dependency Injection](#14-dependency-injection)
15. [Error Handling Strategy](#15-error-handling-strategy)
16. [Reusable and Shared Components](#16-reusable-and-shared-components)
17. [Scalability, Maintainability, and Testability](#17-scalability-maintainability-and-testability)
18. [Module Dependency Diagram](#18-module-dependency-diagram)
19. [Package Structure](#19-package-structure)
20. [Architecture Decision Summary](#20-architecture-decision-summary)
21. [Top Architectural Principles for Implementation](#21-top-architectural-principles-for-implementation)

---

## 1. Context and Design Philosophy

Friendly Emotions application shares therapeutic purpose and session mechanics with Friendly Words, but its material model (`Emotion → Folder → Image` with grammatical gender) is structurally different. The target architecture does not migrate the Friendly Words codebase — it is designed from first principles, incorporating lessons from the Friendly Words architecture improvement analysis.

**What is inherited conceptually (not in code):**
- The session state machine (info → main → game → end)
- The trial lifecycle (AWAITING_RESPONSE → HINT_VISIBLE → JUDGED)
- The error-correction mechanism (`repeatStage`)
- The reinforcement model (clean-correct only)
- The learning step activation singleton
- The UDF event → ViewModel → state → UI pattern
- The test-settings inheritance rule

**What is redesigned:**
- Module boundaries enforced at the Gradle level
- A genuine domain layer with explicit use cases and domain services
- Clean dependency direction from UI → Domain ← Data
- Type-safe enums throughout, replacing magic strings
- Atomic database transactions for all invariant-critical operations
- Room schema exported from version 1 with proper migrations

---

## 2. Architectural Principles

| # | Principle | Rationale |
|---|---|---|
| 1 | **Clean Architecture** | Domain is independent of Android, Room, and Compose. All dependencies point inward. |
| 2 | **MVVM with Unidirectional Data Flow** | UI emits events; ViewModel processes them; state flows back to UI. No bidirectional data binding. |
| 3 | **Use Cases as the single source of business truth** | Each business operation lives in exactly one use case class. ViewModels delegate to use cases; repositories do not contain business logic. |
| 4 | **Type safety over stringly-typed storage** | All domain discriminators (SessionMode, GrammaticalGender, HintType, PromptTemplate, FolderGenderPolicy) are enums or sealed classes stored via Room TypeConverters. |
| 5 | **Atomic database operations for invariants** | Active-step switching, step deletion with fallback, and any multi-table write are wrapped in Room `@Transaction` or `withTransaction {}`. |
| 6 | **Reactive data contracts at repository boundaries** | Repositories expose `Flow<T>` for live data and suspend functions for one-shot reads/writes. ViewModels never query DAOs directly. |
| 7 | **Gradle-enforced module boundaries** | Child app and therapist app code live in separate Gradle modules. Accidental cross-feature dependencies are compile-time errors. |
| 8 | **Session runtime objects are never persisted** | `Trial`, `RenderedPrompt`, `TrialOption`, `SessionResult` are in-memory value objects. The database stores configuration only. |
| 9 | **Domain services are pure functions or stateless classes** | `TrialGenerator`, `PromptRenderer`, `ImageExhaustionTracker`, `TrialPositionRandomizer` have no Android dependencies and are unit-testable with plain Kotlin. |
| 10 | **Room schema versioning from day one** | `exportSchema = true`, version starts at 1, `AutoMigration` used for simple column additions, manual migration scripts for structural changes. No `fallbackToDestructiveMigration`. |

---

## 3. Module Structure

The project is organized into six Gradle modules. This structure enforces feature isolation and clean dependency direction at build time.

```
:app                        ← Application shell
:domain                     ← Pure Kotlin domain layer
:data                       ← Android data layer (Room, DataStore, file I/O)
:feature:child              ← Child App UI
:feature:therapist          ← Therapist App UI
:core:ui                    ← Shared Compose theme and UI components
```

### Module Responsibilities

| Module | Type | Responsibility | Depends On |
|---|---|---|---|
| `:app` | `com.android.application` | Application class, AndroidManifest, launcher Activities, Hilt root component | `:domain`, `:data`, `:feature:child`, `:feature:therapist`, `:core:ui` |
| `:domain` | `com.android.library` (no Android API usage) | Domain entities, value objects, aggregates, repository interfaces, use cases, domain services | Nothing (pure Kotlin) |
| `:data` | `com.android.library` | Room database, entities, DAOs, repository implementations, DataStore, file storage | `:domain` |
| `:feature:child` | `com.android.library` | Child app screens, ViewModels, navigation graph, child-specific state | `:domain`, `:core:ui` |
| `:feature:therapist` | `com.android.library` | Therapist app screens, ViewModels, navigation graph, wizard coordinator | `:domain`, `:core:ui` |
| `:core:ui` | `com.android.library` | Material3 theme, shared Compose components, design tokens | Nothing (or minimal) |

> **Rule:** `:feature:child` and `:feature:therapist` must **never** depend on `:data`. All data access flows through `:domain` repository interfaces, implemented by `:data` and wired by Hilt in `:app`.

---

## 4. Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                           │
│                                                                     │
│   :feature:child                  :feature:therapist                │
│   ┌─────────────────────┐         ┌─────────────────────────────┐   │
│   │  Screens (Compose)  │         │    Screens (Compose)        │   │
│   │  ViewModels         │         │    ViewModels               │   │
│   │  UI State           │         │    UI State                 │   │
│   │  Navigation         │         │    Navigation               │   │
│   └──────────┬──────────┘         └────────────┬────────────────┘   │
└──────────────┼─────────────────────────────────┼────────────────────┘
               │ calls Use Cases                 │ calls Use Cases
               ▼                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DOMAIN LAYER   :domain                     │
│                                                                     │
│   Use Cases          Domain Services        Repository Interfaces   │
│   ┌──────────┐       ┌──────────────┐       ┌───────────────────┐   │
│   │  Session │       │TrialGenerator│       │EmotionFolderRepo  │   │
│   │  Material│       │PromptRenderer│       │EmotionImageRepo   │   │
│   │  Config  │       │ErrorCorrect. │       │LearningStepRepo   │   │
│   └──────────┘       └──────────────┘       │PreferencesRepo    │   │
│                                             └───────────────────┘   │
│   Domain Entities, Value Objects, Aggregates                        │
└─────────────────────────────────────────────────────────────────────┘
               ▲ implements interfaces
┌─────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER   :data                         │
│                                                                     │
│   Room Entities   DAOs          Repository Impls   DataStore        │
│   ┌───────────┐   ┌──────────┐  ┌────────────────┐ ┌─────────────┐ │
│   │ FolderEnt │   │FolderDao │  │FolderRepoImpl  │ │Preferences  │ │
│   │ ImageEnt  │   │ImageDao  │  │ImageRepoImpl   │ │Repository   │ │
│   │ StepEnt   │   │StepDao   │  │StepRepoImpl    │ │Impl         │ │
│   └───────────┘   └──────────┘  └────────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### Layer Rules

- **Presentation → Domain:** ViewModels call use cases and collect flows from repository interfaces (injected via use cases or directly when reading live data).
- **Domain → nothing:** Domain has zero Android/Room/Compose imports.
- **Data → Domain:** Repository implementations implement domain interfaces; Room entities are mapped to domain models at the repository boundary.
- **Domain entities ≠ Room entities:** Room entities are internal to `:data`. Mapping classes (mappers) convert between Room entities and domain models at the repository implementation boundary.

---

## 5. Feature Boundaries

### 5.1 Child App Boundary

The Child App is read-only from a data perspective. It consumes the active `LearningStep` and runs sessions against it. It never writes to the material catalog, never creates or modifies a `LearningStep`, and never changes the active step.

**Child app responsibilities:**
- Display the info splash screen
- Present the main screen (active step name, mode, play button, `canPlay` guard)
- Execute a session (generate trials, present prompts, handle input, apply hints, deliver reinforcement, enforce error correction)
- Display session results in test mode

**Entry point:** `ChildActivity` in `:app` (a single Activity hosting the child navigation graph defined in `:feature:child`)

### 5.2 Therapist App Boundary

The Therapist App is write-capable across all domains. It manages the material catalog (folders and images), creates and edits learning steps, activates steps, and toggles session mode.

**Therapist app responsibilities:**
- Display the main menu
- Navigate the 6-emotion material catalog
- Create, rename, and delete folders (with gender policy)
- Add, view, edit gender of, and delete images within folders
- Create, edit, copy, delete, activate, and toggle mode for learning steps
- Launch the child app from within the therapist app

**Entry point:** `TherapistActivity` in `:app`

### 5.3 Shared Concerns

Both apps share:
- The Room database (via `:data` singleton)
- The domain use cases (read-only for child, read-write for therapist)
- The Hilt component graph (rooted in `FriendlyEmotionsApp` in `:app`)
- The `PreferencesRepository` (therapist reads and writes; child does not read preferences)

---

## 6. Domain Layer Design

### 6.1 Domain Entities and Value Objects

All domain types live in `:domain/model/`. They are pure Kotlin data classes and sealed classes with no Android or framework imports.

```
domain/model/
├── emotion/
│   ├── EmotionId.kt              (enum: HAPPY, SAD, SURPRISED, ANGRY, SCARED, BORED)
│   ├── Emotion.kt                (id, labels: Map<Locale, EmotionLabel>)
│   ├── EmotionLabel.kt           (masculine, feminine, neuter, invariant)
│   ├── EmotionFolder.kt          (id, emotionId, name, genderPolicy, isExample)
│   ├── EmotionImage.kt           (id, folderId, filePath, gender, isExample)
│   ├── GrammaticalGender.kt      (enum: MASCULINE, FEMININE, NEUTER)
│   └── FolderGenderPolicy.kt     (enum: MASCULINE, FEMININE, NEUTER, MIXED)
├── session/
│   ├── LearningStep.kt           (aggregate root — see §6.2)
│   ├── SessionMode.kt            (enum: LEARNING, TEST)
│   ├── MaterialSelection.kt      (imageUsages: List<ImageUsage>)
│   ├── ImageUsage.kt             (imageId, inLearning, inTest)
│   ├── LearningParameters.kt     (all learning-mode config fields)
│   ├── TestParameters.kt         (overridesLearning + independent fields)
│   ├── ReinforcementSettings.kt  (enabledPraiseWords, animations, fanfare)
│   ├── HintType.kt               (enum: OUTLINE_CORRECT, ANIMATE_CORRECT, SCALE_CORRECT, DIM_INCORRECT)
│   └── PromptTemplate.kt         (enum: EMOTION_ONLY, WHERE_IS, SHOW_ME, FIND, TOUCH, POINT_TO, CHOOSE)
└── runtime/
    ├── Trial.kt                  (targetEmotionId, promptGender, correctOption, allOptions)
    ├── TrialOption.kt            (imageId, imagePath, emotionId, gender)
    └── RenderedPrompt.kt         (displayText, spokenText)
```

### 6.2 Aggregate Invariants

**`LearningStep` aggregate root** enforces:
- `name` uniqueness is checked by `ValidateLearningStepNameUseCase` before persistence.
- `activeMode` is non-null if and only if `isActive = true` — enforced by `ActivateLearningStepUseCase`.
- `TestParameters.overridesLearning = false` means the domain service derives test parameters from learning parameters — the ViewModel never independently sets both without the domain service mediating.
- `activeHintTypes` must contain at least one element — enforced in `LearningParameters` construction.

**`EmotionFolder` aggregate** enforces:
- `genderPolicy` is immutable after creation. The repository implementation rejects updates that modify this field.
- A `MIXED`-policy folder only allows saving images when all images have a gender assigned — enforced by `AssignImagesUseCase`.

### 6.3 Emotion Catalog

The six emotions are domain constants seeded at the `:data` layer level (via `DatabaseInitializer`), not user-created entities. At the domain layer they are accessed via `EmotionCatalog`, which is a pure Kotlin object providing `EmotionId → Emotion` lookup. The `DatabaseInitializer` seeds the emotion rows on first launch and never modifies them.

---

## 7. Learning Engine Architecture

The learning engine is composed entirely of domain services in `:domain`. It has no Android dependencies and is fully unit-testable.

### 7.1 Session Lifecycle

```
SessionEligibilityChecker.check(stepId, mode)
        │ canPlay = true
        ▼
SessionInitializer.initialize(step, mode)
  → filters images by mode via MaterialSelection
  → invokes TrialGenerator.generate(eligibleImages, parameters)
  → resets ImageExhaustionTracker
  → returns Session (immutable list of Trials)
        │
        ▼
SessionOrchestrator (in ViewModel scope)
  → advances Trial index
  → invokes PromptRenderer
  → delegates to TrialPositionRandomizer before display
  → invokes ErrorCorrectionController on outcome
  → invokes ReinforcementEngine on clean-correct
```

`SessionOrchestrator` is the only stateful runtime component. It lives inside `GameViewModel` (in `:feature:child`) as a delegate, keeping trial index, error stage, and per-session trackers.

### 7.2 Domain Services

| Service | Location | Stateful? | Responsibility |
|---|---|---|---|
| `TrialGenerator` | `:domain/service/` | No | Produces `List<Trial>` from eligible images and parameters. Applies image exhaustion cycling, gender filtering for distractors, and all shuffling logic. |
| `PromptRenderer` | `:domain/service/` | No | Produces `RenderedPrompt` from a `PromptTemplate`, `EmotionId`, `GrammaticalGender`, and `Locale`. |
| `ImageExhaustionTracker` | `:domain/service/` | Yes (session-scoped) | Tracks shown images per emotion; ensures no image repeats as the correct answer until the full pool is exhausted. |
| `TrialPositionRandomizer` | `:domain/service/` | Yes (session-scoped) | Shuffles option positions; prevents the correct-answer from occupying the same position across consecutive appearances of the same emotion. Uses up to 50 candidate shuffles. |
| `ErrorCorrectionController` | `:domain/service/` | Yes (session-scoped) | Manages `repeatStage` (0, 1, 2). Determines whether to re-queue a trial and whether to use same-layout or shuffled-layout re-queue. |
| `ReinforcementEngine` | `:domain/service/` | No | Determines reinforcement eligibility (clean-correct in LEARNING mode). Randomly selects a praise word from the active set and an animation theme. |
| `SessionEligibilityChecker` | `:domain/service/` | No | Pre-flight check: verifies active step exists and has at least one eligible image for the current mode. |
| `LearningStepActivationService` | `:domain/service/` | No | Domain service that orchestrates the atomic activate/deactivate transition. Called by `ActivateLearningStepUseCase`. |

### 7.3 State Machine — Trial Lifecycle

The trial state machine is modeled as a sealed class in `:domain/runtime/`:

```kotlin
sealed class TrialState {
    object AwaitingResponse : TrialState()
    object HintVisible : TrialState()
    data class Judged(val verdict: TrialVerdict) : TrialState()
}

enum class TrialVerdict {
    CLEAN_CORRECT,
    CORRECT_AFTER_HINT,
    TIMEOUT
}
```

`GameViewModel` holds the current `TrialState` in a `StateFlow` and transitions it in response to user events and timer events. Composables render from this state. The hint timer is a coroutine launched inside `viewModelScope`, not inside any composable — eliminating the recomposition-driven timing bug identified in Friendly Words.

### 7.4 TTS Management

`TextToSpeech` is owned by a `TtsController`, injected into `GameViewModel`. `TtsController` is lifecycle-aware (implements `DefaultLifecycleObserver`) and is created scoped to the `GameViewModel`'s lifecycle via a Hilt `@ViewModelScoped` binding. The composable never owns TTS resources.

```
TtsController responsibilities:
  - Initialize TextToSpeech engine
  - Set locale (Polish or English based on device locale)
  - Expose: speak(text, queueMode)
  - Lifecycle: shutdown() on ViewModel cleared
```

---

## 8. Therapist Configuration Architecture

### 8.1 Wizard Architecture

The configuration wizard is a 5-tab flow. Each tab gets its own `@HiltViewModel`. State is coordinated by a `LearningStepWizardCoordinator`, which is a plain Kotlin class (not a ViewModel) held in a `@HiltViewModel`-annotated `WizardContainerViewModel`.

```
WizardContainerViewModel  (@HiltViewModel — scoped to the wizard NavBackStackEntry)
  │ holds: WizardStepDraft (in-memory aggregate of all 5 tabs)
  │ exposes: SharedFlow<WizardNavigationEvent>
  │
  ├── MaterialTabViewModel  (@HiltViewModel)
  │     reads:  EmotionCatalog, EmotionFolderRepository, EmotionImageRepository
  │     writes: WizardStepDraft.materialSelection
  │
  ├── LearningTabViewModel  (@HiltViewModel)
  │     reads:  WizardStepDraft.materialSelection (for image count bounds)
  │     writes: WizardStepDraft.learningParameters
  │
  ├── ReinforcementTabViewModel  (@HiltViewModel)
  │     writes: WizardStepDraft.reinforcementSettings
  │
  ├── TestTabViewModel  (@HiltViewModel)
  │     reads:  WizardStepDraft.learningParameters (for inheritance)
  │     writes: WizardStepDraft.testParameters
  │
  └── SaveTabViewModel  (@HiltViewModel)
        uses:   ValidateLearningStepNameUseCase
                SaveLearningStepUseCase
```

**Cross-tab state sharing:** `WizardContainerViewModel` owns the single `WizardStepDraft` StateFlow. Tab ViewModels read it and write slices of it through exposed update functions. This avoids the God-ViewModel anti-pattern while maintaining a single source of truth for the wizard draft.

**Draft persistence:** The draft lives only in the `WizardContainerViewModel`'s scope. Navigating away without saving prompts a discard confirmation dialog. There is no auto-save to the database.

**Test parameters inheritance:** `TestTabViewModel` applies `LearningStepActivationService.deriveTestFromLearning()` whenever `overridesLearning = false` and learning parameters change. This is a domain service call, not a UI-layer derivation.

### 8.2 Material Management Architecture

Material management follows a three-tier drill-down: Emotion list → Folder list → Image list. Each tier is a separate screen with its own ViewModel.

```
EmotionListViewModel       — reads EmotionCatalog (static); navigates to folder list
FolderListViewModel        — reads EmotionFolderRepository for one emotion; creates/deletes folders
FolderDetailViewModel      — reads EmotionImageRepository for one folder; adds/edits/deletes images
```

**Image upload flow:**
1. `FolderDetailViewModel` handles the gallery/camera result via `ActivityResultRegistry`.
2. For `MIXED` gender policy: images arrive in an `AWAITING_GENDER` state stored in a `List<PendingImage>` within the ViewModel.
3. The ViewModel exposes `canSave: Boolean` derived from "all pending images have a gender assigned".
4. On save: `AssignImagesUseCase` validates all genders are assigned, copies files to `filesDir`, inserts `EmotionImage` records.

### 8.3 Learning Step Activation

Activation from the step list screen:

```
LearningStepListViewModel
  → user taps activate
  → calls ActivateLearningStepUseCase(stepId, mode)
        → LearningStepActivationService.activate(stepId, mode)
              → wrapped in a single @Transaction:
                    deactivate currently active step
                    activate target step in given mode
  → emits LearningStepActivated domain event
  → Child App reacts via active step Flow emission
```

The child app collects `LearningStepRepository.observeActiveStep()` as a `StateFlow`. Any activation by the therapist automatically reaches the child app through the shared Room database and Kotlin Flow infrastructure.

---

## 9. Data Architecture

### 9.1 Room Database

**Database name:** `friendly_emotions`  
**Version:** starts at 1  
**Schema export:** `exportSchema = true` (schemas committed to source control)  
**Migration strategy:** explicit `addMigrations()` for structural changes; `@AutoMigration` for simple additions  
**Foreign keys:** enabled via `onOpen` callback

### 9.2 Entity Design

Room entities are internal to `:data`. They do not leak into `:domain`. Mappers convert between the two representations at the repository boundary.

| Room Entity | Domain Equivalent | Notes |
|---|---|---|
| `EmotionFolderEntity` | `EmotionFolder` | Stored with `emotionId` as String (EmotionId.name), `genderPolicy` as String (enum name) |
| `EmotionImageEntity` | `EmotionImage` | `gender` stored as String (GrammaticalGender.name) |
| `LearningStepEntity` | `LearningStep` aggregate root | Flat columns; `activeMode` stored as String (SessionMode.name or null) |
| `ImageUsageEntity` | `ImageUsage` | Junction: `(stepId, imageId)`, `inLearning`, `inTest` |
| `LearningParametersEmbedded` | `LearningParameters` | `@Embedded(prefix = "lp_")`; `activeHintTypes` stored as comma-separated enum names via TypeConverter |
| `TestParametersEmbedded` | `TestParameters` | `@Embedded(prefix = "tp_")` |
| `ReinforcementSettingsEmbedded` | `ReinforcementSettings` | `@Embedded(prefix = "rs_")`; `enabledPraiseWords` as Set<String> via TypeConverter |

Emotions are a static catalog seeded by `DatabaseInitializer` and never modified at runtime. They do not require a Room entity; `EmotionCatalog` provides them as in-memory constants.

### 9.3 DAOs

Each DAO has a single-table responsibility:

| DAO | Tables | Responsibility |
|---|---|---|
| `EmotionFolderDao` | `emotion_folders` | CRUD for folders; query by emotionId |
| `EmotionImageDao` | `emotion_images` | CRUD for images; query by folderId; batch insert |
| `LearningStepDao` | `learning_steps` | CRUD; `getActive(): Flow<LearningStepEntity?>`; `@Transaction` activate/deactivate |
| `ImageUsageDao` | `image_usages` | Insert batch; delete by stepId; query by stepId and mode flag |

### 9.4 DataStore

`PreferencesRepository` stores therapist UI preferences using Jetpack DataStore. It is scoped as a `@Singleton`.

| Key | Type | Default | Usage |
|---|---|---|---|
| `hide_example_folders` | Boolean | false | Filter example folders from folder list |
| `hide_example_steps` | Boolean | false | Filter example steps from step list |

### 9.5 File Storage

User-captured images are stored in `context.filesDir`. Example images are bundled as Android assets.

- **User images:** `filesDir/images/<uuid>.jpg` (UUID-based filename prevents collisions)
- **Camera capture:** uses `ActivityResultContracts.TakePicture()` with a `FileProvider`-backed URI (full-resolution capture, not thumbnail). A `FileProvider` is declared in `AndroidManifest.xml`.
- **Orphan cleanup:** `CleanOrphanImagesUseCase` deletes both the DB record and the physical file when an image is removed or a folder is deleted.

### 9.6 Database Seeding

`DatabaseInitializer` is a Hilt-provided `@Singleton` called from `FriendlyEmotionsApp.onCreate()` (not from any ViewModel). It runs asynchronously using an app-scoped coroutine. It seeds example folders, example images (asset URI references), and two example learning steps on first launch, detected by checking whether any `LearningStepEntity` exists.

---

## 10. Repository Architecture

Repository interfaces live in `:domain/repository/`. Implementations live in `:data/repository/`. Hilt binds the interface to the implementation in `:data`'s DI module.

### 10.1 Repository Interfaces

```
EmotionFolderRepository
  observeFoldersForEmotion(emotionId): Flow<List<EmotionFolder>>
  getFolderById(folderId): EmotionFolder
  createFolder(emotionId, name, genderPolicy): FolderId
  renameFolder(folderId, name)
  deleteFolder(folderId)            ← @Transaction: deletes images + files

EmotionImageRepository
  observeImagesForFolder(folderId): Flow<List<EmotionImage>>
  getImageById(imageId): EmotionImage
  addImages(folderId, images: List<NewEmotionImage>)
  updateImageGender(imageId, gender)
  deleteImage(imageId)              ← deletes DB record + physical file

LearningStepRepository
  observeAllSteps(): Flow<List<LearningStep>>
  observeActiveStep(): Flow<LearningStep?>
  getStepById(stepId): LearningStep
  getImagesEligibleForStep(stepId, mode): List<EmotionImage>
  saveStep(draft: LearningStepDraft): LearningStepId
  updateStep(stepId, draft: LearningStepDraft)
  deleteStep(stepId)                ← @Transaction: fallback activation if active
  activateStep(stepId, mode)        ← @Transaction: clear + activate
  setActiveMode(stepId, mode)
  copyStep(stepId): LearningStepId
  getAllStepNames(): List<String>    ← for uniqueness validation

PreferencesRepository
  observeHideExampleFolders(): Flow<Boolean>
  observeHideExampleSteps(): Flow<Boolean>
  setHideExampleFolders(hide: Boolean)
  setHideExampleSteps(hide: Boolean)
```

### 10.2 Repository Rules

- Repositories contain **no business logic**. They perform data access, entity mapping, and file I/O.
- Multi-table operations that enforce domain invariants (activate/deactivate, delete with fallback, folder delete with cascade) are wrapped in `@Transaction`-annotated DAO methods.
- The mapping between Room entities and domain models is the responsibility of the repository implementation, not the DAO or the use case.

---

## 11. Use Case Organization

Use cases live in `:domain/usecase/`. Each class encapsulates exactly one business operation and is named with a verb phrase.

### 11.1 Material Use Cases

```
material/
├── ObserveFoldersUseCase           observeImagesForFolder via repository
├── CreateFolderUseCase             validates name, creates folder with gender policy
├── RenameFolderUseCase             validates name, updates folder
├── DeleteFolderUseCase             validates folder is user-created (not example), triggers cascade
├── AssignImagesUseCase             validates all MIXED-folder images have gender, persists images
├── UpdateImageGenderUseCase        updates a single image's grammatical gender
└── DeleteImageUseCase             validates image is not example, deletes DB record + file
```

### 11.2 Learning Step Use Cases

```
learningStep/
├── ObserveLearningStepsUseCase     returns Flow from repository
├── GetLearningStepUseCase          single-step fetch for edit pre-population
├── ValidateLearningStepNameUseCase checks non-blank + case-insensitive uniqueness (excluding self)
├── SaveLearningStepUseCase         creates new step; validates name before save
├── UpdateLearningStepUseCase       edits existing step; re-validates name excluding self
├── DeleteLearningStepUseCase       @Transaction: deletes step; activates example fallback if active
├── CopyLearningStepUseCase         deep-copies step with auto-generated unique name
├── ActivateLearningStepUseCase     @Transaction: deactivates current; activates target
├── SetActiveModeUseCase            changes mode of already-active step
└── DeriveTestParametersUseCase     produces TestParameters mirroring LearningParameters
```

### 11.3 Session Use Cases

```
session/
├── CheckSessionEligibilityUseCase  validates canPlay = true
├── InitializeSessionUseCase        loads active step, filters images by mode, invokes TrialGenerator
└── CleanOrphanImagesUseCase        deletes unreferenced image files and DB records
```

### 11.4 Use Case Contracts

Every use case follows the same structure:

```
class SomeUseCase @Inject constructor(
    private val repository: SomeRepository
) {
    suspend operator fun invoke(params): Result<Output>
}
```

Use cases return `Result<T>` for operations that can fail with domain errors. Callers (ViewModels) map the result to UI state. For observing streams, use cases return `Flow<T>` and do not suspend.

---

## 12. ViewModel Responsibilities

ViewModels live in `:feature:child` and `:feature:therapist`. They follow strict rules:

**Allowed:**
- Inject use cases (never repositories or DAOs directly)
- Hold `StateFlow<UiState>` for UI state
- Hold `Channel<NavigationEvent>` for one-shot navigation events
- Call use cases in `viewModelScope`
- Map domain models to UI models (simple transformations)
- Launch coroutines for timers (hint timer in `GameViewModel`)

**Not allowed:**
- Business logic that belongs in a use case
- Direct DAO injection
- Accessing Room TypeConverters or mappers
- Owning Android resources that survive recomposition (TTS is owned by `TtsController` which the ViewModel delegates to)

### 12.1 Key ViewModels

| ViewModel | Feature | Key Responsibilities |
|---|---|---|
| `ChildHomeViewModel` | child | Observes active step, evaluates `canPlay`, drives info→main transition |
| `GameViewModel` | child | Owns `SessionOrchestrator` delegate; manages trial state machine; drives hint timer; delegates TTS to `TtsController`; calls `ErrorCorrectionController`; emits session results |
| `SessionEndViewModel` | child | Displays session results; pre-generates next session asynchronously |
| `LearningStepListViewModel` | therapist | Observes step list; handles activate/copy/delete events |
| `WizardContainerViewModel` | therapist | Owns `WizardStepDraft`; coordinates tab ViewModels; saves on final submit |
| `MaterialTabViewModel` | therapist | Manages material selection within the wizard |
| `FolderListViewModel` | therapist | Manages folders for one emotion |
| `FolderDetailViewModel` | therapist | Manages images within one folder; handles MIXED gender assignment |

---

## 13. State Management

### 13.1 UI State Pattern

Every screen follows the `UiState` pattern:

```
*UiState      — immutable data class (or sealed class for multi-state screens)
*UiEvent      — sealed class of user intents
*ViewModel    — processes events, updates StateFlow<UiState>
*Screen       — reads StateFlow, emits events
```

Navigation events use `Channel<NavigationEvent>` (consumed exactly once via `collectAsEffect()`).

### 13.2 Child App Navigation State

The child app navigation uses a **typed sealed class** instead of strings:

```kotlin
sealed class ChildScreen {
    object Info : ChildScreen()
    object Main : ChildScreen()
    object Game : ChildScreen()
    data class End(val result: SessionResult) : ChildScreen()
}
```

`ChildHomeViewModel` exposes `StateFlow<ChildScreen>`. The root composable routes to the correct screen based on this value. The `when` expression is exhaustive and compiler-enforced. The child app uses no `NavController` — back-navigation for children is intentionally suppressed by this design.

### 13.3 Therapist App Navigation

The therapist app uses Jetpack Navigation Compose with a `NavHost` and typed routes. Routes are defined as sealed classes with serializable arguments (using the Navigation 2.8+ type-safe routes API).

### 13.4 Wizard Draft State

`WizardStepDraft` is an immutable data class held in `WizardContainerViewModel`. Tab ViewModels write to it by calling update functions on the container ViewModel, which produces a new draft via `copy()`. This gives the wizard a single source of truth while keeping tab ViewModels focused.

---

## 14. Dependency Injection

Hilt is the DI framework. The component hierarchy:

| Hilt Component | Bindings |
|---|---|
| `SingletonComponent` | `AppDatabase`, all DAOs, all Repository implementations, `TtsController` provider |
| `ActivityRetainedComponent` | — |
| `ViewModelComponent` | All use cases, `WizardStepDraft` (as assisted injection for edit mode) |

### 14.1 Module Organization

```
:data/di/
├── DatabaseModule.kt        @Provides AppDatabase, all DAOs
├── RepositoryModule.kt      @Binds interface → implementation for all repositories
└── StorageModule.kt         @Provides Context for file I/O

:feature:child/di/
└── ChildModule.kt           @Provides TtsController (ViewModelScoped)

:app/
└── AppComponent             (Hilt root — no additional modules needed)
```

No DI wiring lives in `:domain`. The domain layer has no DI framework dependency.

### 14.2 Injection Rules

- Use cases are `@Inject constructor`-annotated and bound automatically by Hilt.
- Repository interfaces are bound to implementations via `@Binds` in `RepositoryModule`.
- Tab ViewModels in the wizard share `WizardContainerViewModel` via `hiltNavGraphViewModel()` scoped to the wizard back-stack entry.
- `FriendlyEmotionsApp` is annotated `@HiltAndroidApp` and lives in `:app`, not in any feature package.

---

## 15. Error Handling Strategy

### 15.1 Domain Error Model

```kotlin
sealed class DomainError {
    data class FolderNotFound(val folderId: FolderId) : DomainError()
    data class DuplicateStepName(val name: String) : DomainError()
    object StepNameBlank : DomainError()
    object NoExampleStepAvailable : DomainError()
    data class GenderNotAssigned(val imageIds: List<ImageId>) : DomainError()
    object ExampleContentNotDeletable : DomainError()
    object InsufficientMaterialForSession : DomainError()
    data class FileOperationFailed(val cause: Throwable) : DomainError()
}
```

### 15.2 Error Flow

```
Use Case returns Result<T, DomainError>
        │
        ▼
ViewModel maps Result:
  Success → update UiState with data
  Failure → update UiState with ErrorUiState (message, recovery action)
        │
        ▼
Screen reads UiState:
  Shows inline error message, dialog, or Snackbar as appropriate
  Never crashes — all errors are handled
```

### 15.3 Error Categories and Handling

| Error Category | Source | Handling |
|---|---|---|
| Validation errors (blank name, duplicate name) | `ValidateLearningStep*UseCase` | Inline field error in Save tab |
| Constraint violations (delete example content) | Repository/use case | Error dialog with explanation |
| MIXED gender assignment incomplete | `AssignImagesUseCase` | Highlighted images + blocked Save |
| File I/O failure (image copy fails) | Repository impl | Snackbar with retry or skip option |
| Session eligibility failure | `CheckSessionEligibilityUseCase` | Play button disabled + explanatory message |
| Unexpected runtime errors | ViewModel `catch {}` block | Generic error Snackbar + logged |

---

## 16. Reusable and Shared Components

### 16.1 Shared Domain Components (`:domain`)

These domain services are reusable across any feature or future application:

| Component | Reuse Value |
|---|---|
| `TrialGenerator` | Any configurable multiple-choice learning quiz |
| `PromptRenderer` | Any locale-aware, gender-inflected prompt system |
| `TrialPositionRandomizer` | Any repeated-trial quiz with anti-habituation |
| `ErrorCorrectionController` | Any `repeatStage`-based corrective practice system |
| `ImageExhaustionTracker` | Any pool-cycling randomization for correct answers |
| `DeriveTestParametersUseCase` | Any dual-profile settings system (practice vs. assessment) |

### 16.2 Shared UI Components (`:core:ui`)

| Component | Purpose |
|---|---|
| `NumberSelector` | ± button control for integer values with min/max |
| `YesNoConfirmationDialog` | Standard destructive-action confirmation |
| `InfoDialog` | Read-only information overlay |
| `CheckboxRow` | Labeled toggle row used throughout therapist settings |
| `FriendlyEmotionsTheme` | Material3 theme tokens (colors, typography, shapes) |
| `LoadingScreen` | Standard loading state overlay |
| `ErrorScreen` | Standard error state with retry action |

### 16.3 Shared Business Constants

The Emotion catalog (6 fixed emotions with all label forms) is defined once in `:domain/EmotionCatalog.kt` and shared across all features. The praise word set and animation theme list are defined in `:domain/model/session/ReinforcementSettings.kt` as companion object constants.

---

## 17. Scalability, Maintainability, and Testability

### 17.1 Testability

| Layer | Test type | Dependencies |
|---|---|---|
| Domain services | Pure unit tests | No mocks needed — all inputs are value objects |
| Use cases | Unit tests | Repository interfaces mocked via fakes or MockK |
| ViewModels | Unit tests + `kotlinx-coroutines-test` | Use case fakes; `TestDispatcher` for timers |
| Repository implementations | Integration tests | In-memory Room database |
| Composables | Compose UI tests | ViewModel fakes via `hiltRule` |

The domain layer's independence from Android eliminates the need for robolectric or instrumented tests at the business-logic level.

### 17.2 Scalability

- Adding a new session mode (e.g., a "review" mode): add `SESSION_MODE` enum value, add `ReviewParameters` value object, add `inReview: Boolean` to `ImageUsage`, update `TrialGenerator` strategy.
- Adding a new hint type: add `HintType` enum value, update `LearningParameters` validation, no change to the trial state machine.
- Adding a 7th emotion: update `EmotionId` enum and seed data — all other code reacts automatically.
- Adding session history/scoring persistence: add a new `SessionResult` entity and `SessionResultRepository` — no changes to existing architecture.

### 17.3 Maintainability

- **No God ViewModels:** The wizard uses one container ViewModel plus five focused tab ViewModels.
- **No business logic in composables:** Composables are stateless renderers.
- **No magic strings:** All domain discriminators are enums with TypeConverters.
- **No DAO injection in UI:** All UI data access routes through use cases and repositories.
- **Feature isolation:** `:feature:child` cannot accidentally call therapist code and vice versa.

---

## 18. Module Dependency Diagram

```
                         ┌────────────────────────────────┐
                         │              :app               │
                         │  FriendlyEmotionsApp            │
                         │  TherapistActivity              │
                         │  ChildActivity                  │
                         └──────────┬─────────────────┬────┘
                                    │                 │
              ┌─────────────────────┤                 ├──────────────────────┐
              ▼                     ▼                 ▼                      ▼
   ┌──────────────────┐  ┌──────────────────┐   ┌──────────────────┐   ┌──────────┐
   │ :feature:child   │  │:feature:therapist│   │    :data         │   │ :core:ui │
   │                  │  │                  │   │  Room Entities   │   │  Theme   │
   │ ChildHomeVM      │  │ WizardContainerVM│   │  DAOs            │   │Components│
   │ GameViewModel    │  │ FolderDetailVM   │   │  Repo Impls      │   └────┬─────┘
   │ SessionEndVM     │  │ LearningStepVM   │   │  DataStore       │        │
   └──────────┬───────┘  └────────┬─────────┘   └────────┬─────────┘        │
              │                   │                       │                  │
              │  ┌────────────────┘                       │                  │
              ▼  ▼                                        │                  │
   ┌──────────────────────────────────────────────────────┼──────────────────┘
   │                        :domain                       │
   │                                                      │
   │  EmotionCatalog          TrialGenerator               │ implements
   │  EmotionFolder           PromptRenderer               │ repository
   │  EmotionImage            ErrorCorrectionController    │ interfaces
   │  LearningStep            ImageExhaustionTracker       │
   │  Trial, TrialOption      TrialPositionRandomizer      │
   │  RenderedPrompt          ReinforcementEngine          │
   │                                                      │
   │  Use Cases               Repository Interfaces ◄─────┘
   │  (all operations)        (EmotionFolderRepo,
   │                           EmotionImageRepo,
   │                           LearningStepRepo,
   │                           PreferencesRepo)
   └──────────────────────────────────────────────────────┘

Allowed dependency edges:
  :app          → :feature:child, :feature:therapist, :data, :domain, :core:ui
  :feature:child    → :domain, :core:ui
  :feature:therapist → :domain, :core:ui
  :data         → :domain
  :core:ui      → (none — or only Compose/Material3)
  :domain       → (none — pure Kotlin)

Forbidden edges (compiler-enforced):
  :feature:child    ↛ :data
  :feature:therapist ↛ :data
  :feature:child    ↛ :feature:therapist
  :domain           ↛ anything
```

---

## 19. Package Structure

### `:domain`

```
pg.autyzm.friendlyemotions.domain/
├── catalog/
│   └── EmotionCatalog.kt                    # 6 fixed Emotion constants
├── model/
│   ├── emotion/
│   │   ├── EmotionId.kt
│   │   ├── Emotion.kt
│   │   ├── EmotionLabel.kt
│   │   ├── EmotionFolder.kt
│   │   ├── EmotionImage.kt
│   │   ├── GrammaticalGender.kt
│   │   └── FolderGenderPolicy.kt
│   ├── session/
│   │   ├── LearningStep.kt
│   │   ├── SessionMode.kt
│   │   ├── MaterialSelection.kt
│   │   ├── ImageUsage.kt
│   │   ├── LearningParameters.kt
│   │   ├── TestParameters.kt
│   │   ├── ReinforcementSettings.kt
│   │   ├── HintType.kt
│   │   └── PromptTemplate.kt
│   └── runtime/
│       ├── Trial.kt
│       ├── TrialOption.kt
│       ├── TrialState.kt
│       ├── TrialVerdict.kt
│       ├── RenderedPrompt.kt
│       └── SessionResult.kt
├── repository/
│   ├── EmotionFolderRepository.kt
│   ├── EmotionImageRepository.kt
│   ├── LearningStepRepository.kt
│   └── PreferencesRepository.kt
├── service/
│   ├── TrialGenerator.kt
│   ├── PromptRenderer.kt
│   ├── ImageExhaustionTracker.kt
│   ├── TrialPositionRandomizer.kt
│   ├── ErrorCorrectionController.kt
│   ├── ReinforcementEngine.kt
│   ├── SessionEligibilityChecker.kt
│   └── LearningStepActivationService.kt
├── usecase/
│   ├── material/
│   │   ├── ObserveFoldersUseCase.kt
│   │   ├── CreateFolderUseCase.kt
│   │   ├── RenameFolderUseCase.kt
│   │   ├── DeleteFolderUseCase.kt
│   │   ├── AssignImagesUseCase.kt
│   │   ├── UpdateImageGenderUseCase.kt
│   │   └── DeleteImageUseCase.kt
│   ├── learningStep/
│   │   ├── ObserveLearningStepsUseCase.kt
│   │   ├── GetLearningStepUseCase.kt
│   │   ├── ValidateLearningStepNameUseCase.kt
│   │   ├── SaveLearningStepUseCase.kt
│   │   ├── UpdateLearningStepUseCase.kt
│   │   ├── DeleteLearningStepUseCase.kt
│   │   ├── CopyLearningStepUseCase.kt
│   │   ├── ActivateLearningStepUseCase.kt
│   │   ├── SetActiveModeUseCase.kt
│   │   └── DeriveTestParametersUseCase.kt
│   └── session/
│       ├── CheckSessionEligibilityUseCase.kt
│       ├── InitializeSessionUseCase.kt
│       └── CleanOrphanImagesUseCase.kt
└── error/
    └── DomainError.kt
```

### `:data`

```
pg.autyzm.friendlyemotions.data/
├── database/
│   ├── AppDatabase.kt
│   ├── DatabaseInitializer.kt
│   └── converter/
│       ├── GrammaticalGenderConverter.kt
│       ├── FolderGenderPolicyConverter.kt
│       ├── HintTypeSetConverter.kt
│       ├── PromptTemplateConverter.kt
│       └── SessionModeConverter.kt
├── entity/
│   ├── EmotionFolderEntity.kt
│   ├── EmotionImageEntity.kt
│   ├── LearningStepEntity.kt
│   ├── ImageUsageEntity.kt
│   ├── LearningParametersEmbedded.kt
│   ├── TestParametersEmbedded.kt
│   └── ReinforcementSettingsEmbedded.kt
├── dao/
│   ├── EmotionFolderDao.kt
│   ├── EmotionImageDao.kt
│   ├── LearningStepDao.kt
│   └── ImageUsageDao.kt
├── mapper/
│   ├── EmotionFolderMapper.kt
│   ├── EmotionImageMapper.kt
│   └── LearningStepMapper.kt
├── repository/
│   ├── EmotionFolderRepositoryImpl.kt
│   ├── EmotionImageRepositoryImpl.kt
│   ├── LearningStepRepositoryImpl.kt
│   └── PreferencesRepositoryImpl.kt
├── datastore/
│   └── TherapistPreferencesDataStore.kt
└── di/
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    └── StorageModule.kt
```

### `:feature:child`

```
pg.autyzm.friendlyemotions.child/
├── navigation/
│   ├── ChildScreen.kt               # sealed class navigation state
│   └── ChildNavigationHost.kt
├── home/
│   ├── ChildHomeScreen.kt
│   ├── ChildHomeViewModel.kt
│   └── ChildHomeUiState.kt
├── session/
│   ├── GameScreen.kt
│   ├── GameViewModel.kt
│   ├── GameUiState.kt
│   ├── GameUiEvent.kt
│   ├── SessionOrchestrator.kt       # delegate in GameViewModel
│   └── TtsController.kt
├── end/
│   ├── SessionEndScreen.kt
│   ├── SessionEndViewModel.kt
│   └── SessionEndUiState.kt
└── di/
    └── ChildModule.kt
```

### `:feature:therapist`

```
pg.autyzm.friendlyemotions.therapist/
├── navigation/
│   ├── TherapistNavGraph.kt
│   └── TherapistRoutes.kt           # sealed class typed routes
├── home/
│   ├── TherapistHomeScreen.kt
│   └── TherapistHomeViewModel.kt
├── materials/
│   ├── emotionList/
│   │   ├── EmotionListScreen.kt
│   │   └── EmotionListViewModel.kt
│   ├── folderList/
│   │   ├── FolderListScreen.kt
│   │   ├── FolderListViewModel.kt
│   │   └── FolderListUiState.kt
│   └── folderDetail/
│       ├── FolderDetailScreen.kt
│       ├── FolderDetailViewModel.kt
│       └── FolderDetailUiState.kt
└── learningStep/
    ├── list/
    │   ├── LearningStepListScreen.kt
    │   ├── LearningStepListViewModel.kt
    │   └── LearningStepListUiState.kt
    └── wizard/
        ├── WizardContainerViewModel.kt
        ├── WizardStepDraft.kt
        ├── material/
        │   ├── MaterialTabScreen.kt
        │   └── MaterialTabViewModel.kt
        ├── learning/
        │   ├── LearningTabScreen.kt
        │   └── LearningTabViewModel.kt
        ├── reinforcement/
        │   ├── ReinforcementTabScreen.kt
        │   └── ReinforcementTabViewModel.kt
        ├── test/
        │   ├── TestTabScreen.kt
        │   └── TestTabViewModel.kt
        └── save/
            ├── SaveTabScreen.kt
            └── SaveTabViewModel.kt
```

### `:app`

```
pg.autyzm.friendlyemotions/
├── FriendlyEmotionsApp.kt           # @HiltAndroidApp
├── TherapistActivity.kt             # launcher Activity for therapist
└── ChildActivity.kt                 # launcher Activity for child
```

---

## 20. Architecture Decision Summary

| Decision | Choice | Alternative Considered | Rationale |
|---|---|---|---|
| Module structure | 6 Gradle modules | 2 modules (app + shared) | Enforces dependency direction at build time; eliminates the Friendly Words H-3 finding |
| Domain layer | Pure Kotlin `:domain` module | Logic in repositories | Single source of truth for business rules; JVM-testable without Android |
| Use case layer | Explicit use case classes per operation | Logic in ViewModels or repositories | Eliminates the Friendly Words H-1 finding; each invariant has one home |
| Wizard architecture | Container VM + 5 tab VMs sharing draft | Single God-ViewModel | Eliminates C-1 and C-2 findings; each tab VM is independently testable |
| Trial state machine | Sealed class `TrialState` | Boolean flags in ViewModel | Type-safe; compiler-exhaustive; documents all valid states explicitly |
| Child app navigation | Sealed class `ChildScreen` + single composable router | Jetpack NavController | Prevents back-navigation for children; eliminates H-4 finding (string states) |
| TTS ownership | `TtsController` class owned by ViewModel | TTS in composable | Survives recomposition; eliminates C-4 finding |
| Enum persistence | String (enum name) via TypeConverter | Magic string constants | Eliminates H-5 finding; rename-safe; compile-time validated |
| Database transactions | `@Transaction` on all invariant-critical writes | Sequential suspend calls | Eliminates C-5 finding; guarantees active-step singleton invariant |
| File naming | UUID-based filenames | Timestamp-based | Collision-free even when adding multiple images in the same millisecond |
| Camera capture | `TakePicture()` + FileProvider | `TakePicturePreview()` thumbnail | Full-resolution capture; eliminates L-2 finding |
| Schema migration | `exportSchema = true` + explicit migrations | `fallbackToDestructiveMigration` | Eliminates H-9 and L-1 findings; user data is never silently destroyed |
| Room entities vs domain models | Separate; mapped at repository boundary | Shared entities | Domain stays framework-free; data schema can evolve without touching domain |
| Database seeding | `DatabaseInitializer` from `Application.onCreate()` | Seeding in MainScreenViewModel | Eliminates H-8 finding; seeding is not tied to any Activity lifecycle |
| Error handling | `Result<T, DomainError>` sealed hierarchy | Exceptions | Explicit, typed, no silent swallowing; forces callers to handle failures |
| Preferences scope | `@Singleton` | `@ViewModelComponent` | Eliminates L-5 finding; DataStore is process-singleton |

---

## 21. Top Architectural Principles for Implementation

These principles must be followed by every contributor from the first commit:

1. **The domain layer has zero framework imports.** No `import android.*`, no `import androidx.*`, no Room annotations, no Hilt annotations in `:domain`. Violations break the entire architecture contract.

2. **ViewModels call use cases. Never repositories. Never DAOs.** If a ViewModel needs data, there is a use case for it. If there is no use case yet, create one.

3. **Business logic belongs in use cases or domain services, not in ViewModels or composables.** A ViewModel that validates business rules or orchestrates multi-step domain operations is a use case in disguise. Extract it.

4. **All database operations that enforce a domain invariant must be wrapped in a `@Transaction`.** Active-step switching, deletion with fallback, and folder deletion with image cascade are each a single atomic unit. Never execute them as two sequential suspend calls.

5. **Composables are stateless renderers.** They read `UiState` and emit `UiEvent`. They never hold timers, TTS instances, or mutable business state. Any `LaunchedEffect` in a composable should only drive animations or side effects that have no business logic.

6. **Use sealed classes and enums for all domain discriminators.** `SessionMode`, `GrammaticalGender`, `HintType`, `PromptTemplate`, `FolderGenderPolicy`, `TrialVerdict`, `ChildScreen` — all typed. No stringly-typed comparisons anywhere.

7. **Navigation events are one-shot.** Use `Channel<NavigationEvent>` consumed via `collectAsEffect()`. Never use a `Boolean` flag in `UiState` to drive navigation (eliminates the double-navigation race condition).

8. **Room entities never leave the `:data` module.** Domain models are what cross the boundary. Mappers in repository implementations convert between the two representations.

9. **Feature modules never depend on each other or on `:data`.** `:feature:child` and `:feature:therapist` depend only on `:domain` and `:core:ui`. Hilt in `:app` wires the implementations.

10. **Every new schema change increments the Room version and ships with a migration.** `fallbackToDestructiveMigration` is never enabled. `exportSchema = true` is permanent. Schema JSON files are committed to source control alongside the migration code.

---

*End of Target Architecture — Friendly Emotions*
