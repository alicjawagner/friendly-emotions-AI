# Project Inventory — Przyjazne Słowa v2

> **Status:** Descriptive inventory only. No improvements or redesign suggestions are included.
> **Date:** 2026-06-18
> **Analyst role:** Senior Android Software Architect

---

## Table of Contents

1. [High-Level Overview](#1-high-level-overview)
2. [Gradle Modules](#2-gradle-modules)
3. [Package Structure](#3-package-structure)
4. [Entry Points](#4-entry-points)
5. [Main Features](#5-main-features)
6. [External Dependencies](#6-external-dependencies)
7. [Architecture Summary](#7-architecture-summary)

---

## 1. High-Level Overview

**"Przyjazne Słowa"** ("Friendly Words") is a Polish-language, open-source Android educational application developed in partnership with Politechnika Gdańska and the IWRD foundation. Its primary audience is children with developmental needs (e.g. autism spectrum disorder). The application teaches word recognition by presenting the child with a set of images and asking them to select the one matching a spoken word.

The application ships as a **single APK** that contains **two independent sub-applications**, each exposed as a separate launcher icon on the device:

| Sub-application | Launcher label | Audience |
|---|---|---|
| **Child App** ("Gra") | Przyjazne Słowa Gra | Children during therapy sessions |
| **Therapist App** ("Ustawienia") | Przyjazne Słowa Ustawienia | Therapists who configure learning sessions |

Both sub-applications share the same Room database and business-logic code. There is no network communication — all data is stored locally on the device.

### Major Subsystems

| Subsystem | Location | Description |
|---|---|---|
| **Shared Data Layer** | `:shared` module | Room database, entities, DAOs, repositories, shared state models |
| **Child Game Engine** | `:app` — `child_app/` package | State-machine navigation, game round generation, image-matching quiz |
| **Therapist Configuration UI** | `:app` — `therapist/` package | Multi-step configuration wizard, material management, DataStore preferences |
| **Dependency Injection** | Both modules | Hilt/Dagger wiring for database, repositories, and ViewModels |

---

## 2. Gradle Modules

The project has **two active Gradle modules**. A third directory (`child_app/`) exists at the root but is **not included** in `settings.gradle.kts` and contains no active source code — its source lives inside `:app`.

### `settings.gradle.kts`

```
rootProject.name = "friendly-words"
include(":app")
include(":shared")
```

---

### Module: `:app`

| Property | Value |
|---|---|
| **Type** | Android Application (`com.android.application`) |
| **Namespace** | `com.example.friendly_words` |
| **Application ID** | `pg.autyzm.friendlywords` |
| **compileSdk / targetSdk** | 35 |
| **minSdk** | 24 |
| **Layer** | UI + App entry point |

**Responsibility:** Contains all UI code for both sub-applications (child game and therapist configuration), Navigation graphs, all ViewModels, DataStore preferences, and the `Application` class. Depends on `:shared` for data access.

**Plugins:** `android.application`, `kotlin.android`, `kotlin.compose`, `ksp`, `hilt.android`

**Key dependencies:**

| Dependency | Purpose |
|---|---|
| `project(":shared")` | Access to Room DB, entities, DAOs, repositories |
| `androidx.navigation:navigation-compose` | Therapist-side Compose NavHost |
| `androidx.room:room-runtime` + `room-ktx` | Direct DAO injection into GameViewModel / ChildMainViewModel |
| `dagger.hilt.android` + `hilt-compiler` | Dependency injection |
| `androidx.hilt:hilt-navigation-compose` | `hiltViewModel()` factory in Compose |
| `kotlinx.coroutines.core` + `.android` | Async operations, Flow collection |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModel scoping in Compose |
| `io.coil-kt:coil-compose` | Asynchronous image loading |
| `androidx.datastore:datastore-preferences` | Persistent therapist preferences |
| `com.composables:core` | Additional Compose primitives |
| `androidx.compose.material:material` (M2) | Material 2 components |
| `androidx.compose.material:material-icons-extended` | Extended icon set |
| Compose BOM `2024.04.01` + Material3 | Core Compose UI framework |
| `androidx.activity:activity-compose` | `ComponentActivity` + Compose integration |

---

### Module: `:shared`

| Property | Value |
|---|---|
| **Type** | Android Library (`com.android.library`) |
| **Namespace** | `com.example.shared` |
| **compileSdk** | 34 |
| **minSdk** | 24 |
| **Layer** | Data + Domain |

**Responsibility:** Defines the Room database, all entities, all DAOs, all repositories, shared state model classes used by both sub-applications, and the Hilt `AppModule` that provides the database singleton.

**Plugins:** `android.library`, `kotlin.android`, `ksp`, `hilt.android`

**Key dependencies:**

| Dependency | Purpose |
|---|---|
| `androidx.room:room-runtime` + `room-ktx` + `room-compiler` | Persistence layer |
| `dagger.hilt.android` + `hilt-compiler` | Provides `@Module` for `SingletonComponent` |
| `kotlinx.coroutines.core` + `.android` | Suspend functions in DAOs and repositories |
| `androidx.navigation:navigation-compose` | Listed but not actively used in this module |
| `io.coil-kt:coil-compose` | Listed but not actively used in this module |
| `androidx.hilt:hilt-navigation-compose` | Listed but not actively used in this module |

> **Note:** Several `:shared` dependencies (Navigation Compose, Coil, hilt-navigation-compose) appear in the build file but have no active usage within the module's source code.

---

### Directory: `child_app/` (root-level — orphaned stub)

This directory is **not included in `settings.gradle.kts`** and contains no Kotlin source files. It holds only drawable resources. Its build file references namespace `com.example.child_app`. The actual child app source code lives inside `:app` at `com.example.friendly_words.child_app`. This directory is an unused artifact.

---

## 3. Package Structure

### Module `:app` — `com.example.friendly_words`

```
com.example.friendly_words/
│
├── child_app/                              # Child sub-application
│   ├── components/
│   │   ├── ImageOptionBox.kt               # Single image choice tile
│   │   ├── PlayButton.kt                   # Reusable play/start button
│   │   └── RoundOptionsLayout.kt           # Layout for image option grid
│   ├── data/
│   │   ├── GameData.kt                     # GameItem data class
│   │   └── GameRoundManager.kt             # GameRound model + generateGameRounds()
│   ├── game/
│   │   ├── CorrectAnswerScreen.kt          # Feedback screen on correct answer
│   │   ├── FloatingSprites.kt              # Praise animation composable
│   │   ├── GameEndScreen.kt                # Session summary (correct/wrong counts)
│   │   ├── GameScreen.kt                   # Main game UI
│   │   └── GameViewModel.kt                # Game state management
│   ├── main/
│   │   ├── ChildMainEvent.kt               # UI event sealed class
│   │   ├── ChildMainScreen.kt              # ScreenNavigationGame — state router
│   │   ├── ChildMainState.kt               # screenState: String
│   │   ├── ChildMainViewModel.kt           # Loads active config, owns screenState
│   │   ├── MainActivityChild.kt            # Launcher Activity for child app
│   │   └── MainScreen.kt                   # Child home screen (Play button)
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
└── therapist/                              # Therapist sub-application
    ├── data/
    │   ├── PreferencesModule.kt            # Hilt @Module (ViewModelComponent)
    │   ├── PreferencesRepository.kt        # DataStore read/write wrapper
    │   └── SettingsDataStore.kt            # DataStore preference keys
    └── ui/
        ├── components/                     # Shared UI components (therapist side)
        │   ├── InfoDialog.kt
        │   ├── NewConfigurationTopBar.kt
        │   ├── NewConfigurationTopTabs.kt
        │   ├── NumberSelector.kt
        │   ├── NumberSelectorForPictures.kt
        │   ├── NumberSelectorForPicturesPlain.kt
        │   ├── YesNoDialog.kt
        │   └── YesNoDialogWithName.kt
        ├── configuration/                  # Configuration wizard
        │   ├── learning/                   # "Learning" tab of the wizard
        │   │   ├── ConfigurationLearningEvent.kt
        │   │   ├── ConfigurationLearningScreen.kt
        │   │   └── ConfigurationLearningViewModel.kt
        │   ├── list/                       # List of all configurations
        │   │   ├── ConfigurationsListEvent.kt
        │   │   ├── ConfigurationsListScreen.kt
        │   │   ├── ConfigurationsListState.kt
        │   │   └── ConfigurationsListViewModel.kt
        │   ├── material/                   # "Material" tab — assign resources
        │   │   ├── ConfigurationMaterialEvent.kt
        │   │   ├── ConfigurationMaterialScreen.kt
        │   │   └── ConfigurationMaterialViewModel.kt
        │   ├── reinforcement/              # "Reinforcement" tab — praise settings
        │   │   ├── ConfigurationReinforcementEvent.kt
        │   │   ├── ConfigurationReinforcementScreen.kt
        │   │   └── ConfigurationReinforcementViewModel.kt
        │   ├── save/                       # "Save" step — persist configuration
        │   │   ├── ConfigurationSaveEvent.kt
        │   │   ├── ConfigurationSaveScreen.kt
        │   │   ├── ConfigurationSaveState.kt
        │   │   └── ConfigurationSaveViewModel.kt
        │   ├── settings/                   # Master configuration settings screen
        │   │   ├── ConfigurationSettingsEvent.kt
        │   │   ├── ConfigurationSettingsScreen.kt
        │   │   ├── ConfigurationSettingsState.kt
        │   │   └── ConfigurationSettingsViewModel.kt
        │   └── test/                       # "Test" tab — test mode settings
        │       ├── ConfigurationTestEvent.kt
        │       ├── ConfigurationTestScreen.kt
        │       └── ConfigurationTestViewModel.kt
        ├── main/
        │   ├── FriendlyWordsApp.kt         # @HiltAndroidApp Application class
        │   ├── MainActivity.kt             # Launcher Activity for therapist app
        │   ├── MainScreen.kt               # NavHost + NavRoutes definitions
        │   └── MainScreenViewModel.kt
        ├── materials/                      # Educational materials management
        │   ├── creating_new/               # Create / edit a material (Resource)
        │   │   ├── MaterialsCreatingNewMaterialEvent.kt
        │   │   ├── MaterialsCreatingNewMaterialScreen.kt
        │   │   ├── MaterialsCreatingNewMaterialState.kt
        │   │   └── MaterialsCreatingNewMaterialViewModel.kt
        │   └── list/                       # List of all materials
        │       ├── MaterialsListEvent.kt
        │       ├── MaterialsListScreen.kt
        │       ├── MaterialsListState.kt
        │       └── MaterialsListViewModel.kt
        └── theme/
            └── Color.kt
```

---

### Module `:shared` — `com.example.shared`

```
com.example.shared/
└── data/
    ├── another/                            # Shared state models + converters
    │   ├── ConfigurationLearningState.kt
    │   ├── ConfigurationMaterialState.kt   # also contains VocabularyItem
    │   ├── ConfigurationReinforcementState.kt
    │   ├── ConfigurationTestState.kt
    │   ├── Converters.kt                   # Room TypeConverters
    │   ├── ResourceWithImages.kt           # @Relation aggregate DTO
    │   └── RoundSettings.kt               # Interface (shared game contract)
    ├── daos/
    │   ├── ConfigurationDao.kt
    │   ├── ConfigurationResourceDao.kt
    │   ├── ImageDao.kt
    │   └── ResourceDao.kt
    ├── database/
    │   ├── AppDatabase.kt                  # Room @Database class
    │   └── AppModule.kt                    # Hilt @Module SingletonComponent
    ├── entities/
    │   ├── Configuration.kt               # @Entity with embedded LearningSettings + TestSettings
    │   ├── ConfigurationImageUsage.kt
    │   ├── ConfigurationResource.kt
    │   ├── Image.kt
    │   ├── LearningSettings.kt            # @Embedded sub-entity
    │   ├── Resource.kt
    │   ├── ResourceImage.kt               # Junction table
    │   └── TestSettings.kt                # @Embedded sub-entity
    ├── enums/
    │   └── HintType.kt
    └── repositories/
        ├── ConfigurationRepository.kt
        ├── ImageRepository.kt
        └── ResourceRepository.kt
```

---

## 4. Entry Points

### Application Class

| Class | Module | Annotation |
|---|---|---|
| `com.example.friendly_words.therapist.ui.main.FriendlyWordsApp` | `:app` | `@HiltAndroidApp` |

Declared in `AndroidManifest.xml` as `android:name=".therapist.ui.main.FriendlyWordsApp"`. This is the sole Application class for the entire APK — both sub-applications share it.

---

### Activities and Launchers

Both Activities are declared in the single `:app` `AndroidManifest.xml` with `android.intent.action.MAIN` + `android.intent.category.LAUNCHER` intent filters, resulting in **two separate icons** on the device launcher.

#### Therapist App Entry Point

| Property | Value |
|---|---|
| **Class** | `com.example.friendly_words.therapist.ui.main.MainActivity` |
| **Label** | `Przyjazne Słowa Ustawienia` |
| **Icon** | `@drawable/friendly_words_settings` |
| **launchMode** | `singleTask` |
| **Orientation** | Landscape (forced) |

**Boot sequence:**
1. Displays `InformationScreen()` for 5 seconds (splash).
2. Navigates to `MainScreen()` — the root Compose NavHost.

#### Child App Entry Point

| Property | Value |
|---|---|
| **Class** | `com.example.friendly_words.child_app.main.MainActivityChild` |
| **Label** | `Przyjazne Słowa Gra` |
| **Icon** | `@drawable/friendly_words` |
| **launchMode** | `singleTask` |
| **Orientation** | Landscape (forced) |

**Boot sequence:**
1. Mounts `ScreenNavigationGame()` composable.
2. `ChildMainViewModel` loads the active `Configuration` from the database.
3. State machine begins at `"info"` screen.

---

### Navigation Entry Points

#### Therapist App — Compose NavHost (`MainScreen.kt`)

The `NavRoutes` object defines the following route graph:

```
"main"
    └── MaterialsListScreen          "materials"
            └── MaterialsCreatingNewMaterialScreen  "materials/create"
            └── MaterialsCreatingNewMaterialScreen  "materials/edit/{resourceId}"
    └── ConfigurationsListScreen     "config"
            └── ConfigurationSettingsScreen         "config/create"
            └── ConfigurationSettingsScreen         "config/edit/{configId}"
```

`ConfigurationSettingsScreen` is a tabbed screen (tabs handled by `NewConfigurationTopTabs`) that internally shows one of five sub-screens via `NewConfigurationTopTabs` rendering:
- Material, Learning, Reinforcement, Test, Save

#### Child App — String-Based State Machine (`ChildMainScreen.kt`)

No `NavController` is used. Navigation is driven by `ChildMainState.screenState: String`:

```
"info"  ──(auto after 5s)──► "main"
"main"  ──(Play pressed)────► "game"
"game"  ──(all rounds done)──► "end"
"end"   ──(auto/button)──────► "main"
```

---

## 5. Main Features

### Feature 1 — Educational Materials Management (Therapist)

**Purpose:** Allows therapists to create and manage a library of educational materials (called *Materiały Edukacyjne*). Each material (`Resource`) consists of a word label (`learnedWord`), a display name, a category, and one or more images (`Image`). Images can be imported from the device gallery or camera.

**Key classes:** `MaterialsListViewModel`, `MaterialsCreatingNewMaterialViewModel`, `ResourceRepository`, `ImageRepository`, `ResourceDao`, `ImageDao`

**Storage:** Room database — `resources`, `images`, `resource_images` (junction) tables.

---

### Feature 2 — Configuration Management (Therapist)

**Purpose:** Allows therapists to create named *Kroki Uczenia* (Learning Steps) — configurations that define exactly how a child game session will behave. Each configuration bundles: which materials to use, learning-mode parameters, test-mode parameters, reinforcement (praise) settings, and an active/inactive flag.

**Sub-features within configuration:**

| Tab | ViewModel | Description |
|---|---|---|
| Settings (master) | `ConfigurationSettingsViewModel` | Container VM; owns shared state for the entire wizard |
| Material | `ConfigurationMaterialViewModel` | Select which `Resource` items are included |
| Learning | `ConfigurationLearningViewModel` | Number of words, images, hints, repetitions, command type, label visibility |
| Reinforcement | `ConfigurationReinforcementViewModel` | Praise animations selection |
| Test | `ConfigurationTestViewModel` | Test-mode specific parameters (answer time, etc.) |
| Save | `ConfigurationSaveViewModel` | Assign name, persist `Configuration` to database |

**Key classes:** `ConfigurationRepository`, `ConfigurationDao`, `Configuration`, `LearningSettings`, `TestSettings`

---

### Feature 3 — Preferences (Therapist)

**Purpose:** Stores two boolean therapist preferences using DataStore: whether to hide example materials and whether to hide example configurations (pre-seeded demo content).

**Key classes:** `PreferencesRepository`, `SettingsDataStore`, `PreferencesModule`

**Storage:** `datastore-preferences` — file `"user_preferences"` with keys `hide_example_materials` and `hide_example_steps`.

---

### Feature 4 — Child Game Engine

**Purpose:** Delivers the interactive word-learning quiz to the child. At session start, it loads the single *active* `Configuration` from the database and generates a sequence of `GameRound` objects. Each round presents multiple images; the child must tap the one that matches the spoken word command. Correct answers trigger praise animations.

**Sub-features:**

| Component | Class | Description |
|---|---|---|
| Round generation | `GameRoundManager.generateGameRounds()` | Builds ordered `GameRound` list from active config's resources and images |
| Game UI | `GameScreen` + `GameViewModel` | Renders current round, handles answer selection, advances rounds |
| Correct feedback | `CorrectAnswerScreen` | Brief animated confirmation on correct tap |
| Praise animation | `FloatingSprites` | Floating sprite animation for reinforcement |
| Session summary | `GameEndScreen` | Shows correct/wrong counts at session end |

**Key classes:** `GameViewModel`, `ChildMainViewModel`, `GameRoundManager`, `GameData`, `ConfigurationRepository`

---

### Feature 5 — Active Configuration Selection

**Purpose:** Exactly one `Configuration` can be marked `isActive = true` in the database at a time. The therapist can activate a configuration from `ConfigurationsListScreen`; the child app always loads the currently active one. The `ConfigurationDao` provides `getActiveConfiguration(): Flow<Configuration?>` and `activateConfiguration(id)` / `clearActiveConfiguration()` operations.

---

## 6. External Dependencies

### Persistence

| Library | Version | Usage |
|---|---|---|
| **Room** (`androidx.room:room-runtime`, `room-ktx`, `room-compiler`) | 2.7.1 | Primary local database. Used in `:shared` (entities, DAOs, `AppDatabase`) and in `:app` (direct `ConfigurationDao` injection in `ChildMainViewModel`). `fallbackToDestructiveMigration = true`. DB version: 27. |
| **DataStore Preferences** (`androidx.datastore:datastore-preferences`) | 1.1.7 | Stores two therapist boolean flags. Used only in `:app` via `PreferencesRepository`. |

### Dependency Injection

| Library | Version | Usage |
|---|---|---|
| **Hilt / Dagger** (`dagger.hilt.android`, `hilt-compiler`) | 2.56.2 | Application-wide DI framework. `AppModule` (`:shared`, `SingletonComponent`) provides Room DB and DAOs. `PreferencesModule` (`:app`, `ViewModelComponent`) provides `PreferencesRepository`. All ViewModels annotated `@HiltViewModel`. |
| **hilt-navigation-compose** (`androidx.hilt:hilt-navigation-compose`) | 1.2.0 | `hiltViewModel()` factory for ViewModel instantiation inside Compose NavHost. |

### Asynchronous / Reactive

| Library | Version | Usage |
|---|---|---|
| **Kotlin Coroutines** (`kotlinx.coroutines.core`, `kotlinx.coroutines.android`) | 1.7.3 | All suspend functions in DAOs, repositories, and ViewModels. |
| **Kotlin Flow** (part of Coroutines library) | 1.7.3 | `ConfigurationDao.getActiveConfiguration()` returns `Flow<Configuration?>`. `PreferencesRepository` exposes `Flow<Boolean>` streams. ViewModels collect flows via `stateIn()` / `collectAsState()`. |

### UI / Compose

| Library | Version | Usage |
|---|---|---|
| **Compose BOM** (`androidx.compose:compose-bom`) | 2024.04.01 | Aligns all Compose library versions. |
| **Material3** (via BOM) | BOM-managed | Primary design system for both sub-applications. |
| **Material M2** (`androidx.compose.material:material`) | 1.5.4 | Used alongside Material3 in `:app`. |
| **Material Icons Extended** (`androidx.compose.material:material-icons-extended`) | 1.6.0 | Extended icon set used in therapist UI. |
| **composables/core** (`com.composables:core`) | 1.32.0 | Third-party Compose utility components. |
| **Navigation Compose** (`androidx.navigation:navigation-compose`) | 2.9.0 | Therapist-app NavHost routing. |
| **lifecycle-viewmodel-compose** (`androidx.lifecycle:lifecycle-viewmodel-compose`) | 2.9.0 | ViewModel scoping within Compose. |
| **activity-compose** (`androidx.activity:activity-compose`) | 1.10.1 | `setContent {}` and Compose integration in Activities. |

### Image Loading

| Library | Version | Usage |
|---|---|---|
| **Coil Compose** (`io.coil-kt:coil-compose`) | 2.3.0 | Asynchronous image loading for `Resource` images in both sub-applications. Loads from file paths and `file:///android_asset/` URIs. |

### Text-to-Speech

> **ASSUMPTION** — No explicit TTS library import was found in `libs.versions.toml` or any `build.gradle.kts`. Based on the domain (spoken word commands during the game) and the `commandType` / `readCommand` fields on `LearningSettings`, TTS functionality is likely present; however, the implementation may use Android's built-in `android.speech.tts.TextToSpeech` API directly without a third-party library, which would not appear in dependency files. This cannot be confirmed from build files alone.

### Audio Players

> **ASSUMPTION** — No audio player library (e.g. ExoPlayer, MediaPlayer wrapper) was identified in the dependency catalogs. Praise/reinforcement audio, if present, likely uses Android's `MediaPlayer` or `SoundPool` from the platform SDK. Not confirmed from build files alone.

### Animation Libraries

| Library | Version | Usage |
|---|---|---|
| **Compose Animation** (via BOM) | BOM-managed | `FloatingSprites.kt` uses Compose's built-in animation APIs (`animate*AsState`, `AnimatedVisibility`, etc.) for praise sprite animations. |

No third-party animation library (e.g. Lottie) was found in the dependency declarations.

---

## 7. Architecture Summary

### Architecture Style

The application follows a **single-Activity-per-sub-application** approach built entirely on **Jetpack Compose** for UI rendering. The overall architectural style is **MVVM (Model-View-ViewModel)** with a shared data layer.

Evidence:
- All screens are Compose `@Composable` functions.
- Each screen has a corresponding `ViewModel` holding UI state.
- ViewModels read from repositories and expose `StateFlow` / `State` objects consumed by composables via `collectAsState()`.
- There are no XML layouts (apart from the orphaned stub module).

---

### Architectural Patterns

#### Unidirectional Data Flow (UDF)

Each screen follows an Event → ViewModel → State → UI cycle:
- `*Event.kt` — sealed class of user intents (e.g. `ConfigurationLearningEvent`)
- `*State.kt` — immutable data class representing UI state (e.g. `ConfigurationLearningState`)
- `*ViewModel.kt` — processes events, updates state
- `*Screen.kt` — renders state, emits events

#### Repository Pattern

All data access goes through repository classes (`ConfigurationRepository`, `ResourceRepository`, `ImageRepository`). ViewModels never access DAOs directly, with one observed exception:

> **ASSUMPTION** — `ChildMainViewModel` receives `ConfigurationDao` via injection in addition to `ConfigurationRepository`. This appears to be an inconsistency rather than a deliberate pattern, but it is documented as-is without judgement.

#### Hilt Dependency Injection

DI is layered:
- `SingletonComponent` (`:shared` `AppModule`): database, DAOs, `ConfigurationRepository`
- `ViewModelComponent` (`:app` `PreferencesModule`): `PreferencesRepository`
- `@Inject constructor` (no explicit `@Provides`): `ResourceRepository`, `ImageRepository`

#### State Machine Navigation (Child App)

The child app does not use Jetpack Navigation. Instead, a `screenState: String` variable in `ChildMainState` drives which composable is rendered. This is a deliberate simplification, likely to prevent children from accidentally navigating backwards.

#### Compose Navigation (Therapist App)

The therapist app uses `androidx.navigation:navigation-compose` with a `NavHost` and string-based route constants defined in a `NavRoutes` companion/object.

---

### Feature Boundaries

The codebase has **two clearly separated feature packages** inside the single `:app` module, plus a shared data module:

| Boundary | Package | Module |
|---|---|---|
| Child game sub-application | `com.example.friendly_words.child_app` | `:app` |
| Therapist configuration sub-application | `com.example.friendly_words.therapist` | `:app` |
| Shared data and domain | `com.example.shared` | `:shared` |

The two UI feature packages are separated by package convention only — there is no Gradle-level isolation between `child_app` and `therapist` within `:app`. They share the same Application class, the same Room database (via `:shared`), and the same set of Hilt components.

---

### Database Schema (Reference)

**Database:** `friendly_words` | **Room version:** 27 | **Migration strategy:** `fallbackToDestructiveMigration = true`

#### Tables

| Table | Entity class | PK |
|---|---|---|
| `resources` | `Resource` | `id: Long (autoGenerate)` |
| `images` | `Image` | `id: Long (autoGenerate)` |
| `configurations` | `Configuration` | `id: Long (autoGenerate)` |
| `configuration_resources` | `ConfigurationResource` | composite `(configurationId, resourceId)` |
| `configuration_image_usages` | `ConfigurationImageUsage` | composite `(configurationId, imageId)` |
| `resource_images` | `ResourceImage` | composite `(resourceId, imageId)` |

#### Embedded Sub-entities in `configurations`

| Sub-entity | Columns prefix | Fields |
|---|---|---|
| `LearningSettings` | `learning_` | `numberOfWords`, `displayedImagesCount`, `repetitionPerWord`, `commandType`, `showLabelsUnderImages`, `readCommand`, `hintAfterSeconds`, `typesOfHints: List<String>`, `typesOfPraises: List<String>`, `animationsEnabled` |
| `TestSettings` | `test_` | `numberOfWords`, `displayedImagesCount`, `repetitionPerWord`, `commandType`, `showLabelsUnderImages`, `readCommand`, `answerTimeSeconds` |

#### TypeConverters

| Kotlin type | DB type | Separator |
|---|---|---|
| `List<String>` | `String` | `\|\|` |
| `Map<String, Boolean>` | `String` | `;;` (pairs), `::` (key–value) |

---

### ViewModels Reference

| ViewModel | Annotation | Key Injections |
|---|---|---|
| `MainScreenViewModel` | `@HiltViewModel` | `ConfigurationRepository`, `ResourceRepository`, `ImageRepository` |
| `ConfigurationsListViewModel` | `@HiltViewModel` | `ConfigurationRepository`, `PreferencesRepository` |
| `ConfigurationSettingsViewModel` | `@HiltViewModel` | `ConfigurationRepository`, `ResourceRepository`, `ImageRepository`, `PreferencesRepository` |
| `ConfigurationLearningViewModel` | none (plain `ViewModel`) | — |
| `ConfigurationMaterialViewModel` | none (plain `ViewModel`) | — |
| `ConfigurationReinforcementViewModel` | none (plain `ViewModel`) | — |
| `ConfigurationTestViewModel` | none (plain `ViewModel`) | — |
| `ConfigurationSaveViewModel` | none (plain `ViewModel`) | — |
| `MaterialsListViewModel` | `@HiltViewModel` | `ResourceRepository`, `ImageRepository`, `ConfigurationRepository`, `PreferencesRepository` |
| `MaterialsCreatingNewMaterialViewModel` | `@HiltViewModel` | `@ApplicationContext`, `SavedStateHandle`, `ImageRepository`, `ResourceRepository` |
| `ChildMainViewModel` | `@HiltViewModel` | `ConfigurationDao`, `ConfigurationRepository` |
| `GameViewModel` | `@HiltViewModel` | `ConfigurationRepository` |

---

*End of Project Inventory*
